#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA에서 매핑/백로그에 없는 이슈(중복·고아)를 취소 상태로 변경.
정규 이슈만 참조하도록 하고, 나머지는 '참조하지 않음' 처리(취소)합니다.
"""
import os
import re
import sys
import json
import argparse
import base64
import time
import requests
from pathlib import Path
from typing import Set, List, Dict


def load_canonical_keys(mapping_file: str, backend_backlog_path: str) -> Set[str]:
    """정규 이슈 키 집합: 매핑 값 + 백엔드 백로그에 등장하는 GAM-* ID."""
    canonical = set()
    if os.path.exists(mapping_file):
        with open(mapping_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        for k, v in data.items():
            if not k.startswith('_') and isinstance(v, str):
                canonical.add(v)
    if os.path.exists(backend_backlog_path):
        content = Path(backend_backlog_path).read_text(encoding='utf-8')
        for m in re.finditer(r'\b(GAM-\d+(?:-\d+)?)\b', content):
            canonical.add(m.group(1))
    return canonical


def get_available_transitions(jira_url: str, headers: dict, issue_key: str) -> List[dict]:
    try:
        r = requests.get(f"{jira_url}/rest/api/3/issue/{issue_key}/transitions", headers=headers)
        return r.json().get('transitions', []) if r.status_code == 200 else []
    except Exception:
        return []


def transition_to_cancel(jira_url: str, headers: dict, issue_key: str) -> bool:
    """이슈를 취소/완료/종료 등으로 전환."""
    transitions = get_available_transitions(jira_url, headers, issue_key)
    cancel_names = ['취소', 'Cancel', 'Done', '완료', 'Closed', '종료', 'Resolved', '해결됨']
    tid = None
    for t in transitions:
        name = (t.get('name') or '').lower()
        if any(c.lower() in name for c in cancel_names):
            tid = t.get('id')
            break
    if not tid and transitions:
        tid = transitions[0].get('id')
    if not tid:
        return False
    r = requests.post(
        f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
        headers=headers,
        json={"transition": {"id": tid}}
    )
    return r.status_code == 204


def fetch_all_issue_keys(jira_url: str, headers: dict, project_key: str) -> List[Dict]:
    """프로젝트 내 모든 이슈 키·상태 조회. /rest/api/3/search/jql 사용 시 이슈는 id만 반환하므로 개별 GET으로 key/status 조회."""
    url = f"{jira_url}/rest/api/3/search/jql"
    out = []
    next_token = None
    max_results = 100
    while True:
        payload = {"jql": f"project = {project_key} ORDER BY key ASC", "maxResults": max_results}
        if next_token:
            payload["nextPageToken"] = next_token
        r = requests.post(url, headers=headers, json=payload)
        if r.status_code != 200:
            break
        data = r.json()
        batch = data.get('issues', [])
        for i in batch:
            issue_id = i.get('id')
            key = i.get('key')
            fields = i.get('fields', {})
            status_obj = fields.get('status')
            status = status_obj.get('name', '') if isinstance(status_obj, dict) else str(status_obj or '')
            if not key and issue_id:
                dr = requests.get(
                    f"{jira_url}/rest/api/3/issue/{issue_id}",
                    headers=headers,
                    params={"fields": "key,status"},
                )
                if dr.status_code == 200:
                    d = dr.json()
                    key = d.get('key')
                    st = (d.get('fields') or {}).get('status')
                    status = st.get('name', '') if isinstance(st, dict) else ''
            out.append({"key": key, "status": status})
            time.sleep(0.2)
        next_token = data.get('nextPageToken')
        if not next_token or data.get('isLast'):
            break
        time.sleep(0.3)
    return out


def run(
    jira_url: str,
    jira_email: str,
    jira_api_token: str,
    project_key: str,
    mapping_file: str,
    backend_backlog: str,
    dry_run: bool,
    yes: bool,
) -> None:
    jira_url = jira_url.rstrip('/')
    auth = base64.b64encode(f"{jira_email}:{jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    canonical = load_canonical_keys(mapping_file, backend_backlog)
    print(f"정규 이슈 수: {len(canonical)}개 (매핑 + 백엔드 백로그)")
    print("프로젝트 이슈 조회 중...")
    all_issues = fetch_all_issue_keys(jira_url, headers, project_key)
    print(f"전체 이슈: {len(all_issues)}개")

    done_statuses = {'done', '완료', 'closed', '취소', '종료', 'resolved', '해결됨'}
    unmapped = [
        i for i in all_issues
        if i['key'] not in canonical
        and (i['status'] or '').lower() not in done_statuses
    ]
    print(f"취소 대상 (정규에 없고 미완료): {len(unmapped)}개")
    if not unmapped:
        print("처리할 이슈가 없습니다.")
        return

    for i in unmapped[:30]:
        print(f"  - {i['key']} [{i['status']}]")
    if len(unmapped) > 30:
        print(f"  ... 외 {len(unmapped) - 30}개")

    if dry_run:
        print("\n[DRY RUN] 실제 상태 변경 없이 종료.")
        return

    if not yes:
        print("\n실제로 위 이슈들을 취소 상태로 변경하려면 --yes 를 붙여 다시 실행하세요.")
        return

    print("\n취소 상태로 전환 중...")
    ok, fail = 0, 0
    for i, issue in enumerate(unmapped, 1):
        if transition_to_cancel(jira_url, headers, issue['key']):
            ok += 1
            if i % 20 == 0:
                print(f"  진행: {i}/{len(unmapped)} (성공 {ok})")
        else:
            fail += 1
            print(f"  ✗ 전환 실패: {issue['key']}")
        time.sleep(0.3)
    print(f"\n완료: 성공 {ok}개, 실패 {fail}개.")


def main():
    parser = argparse.ArgumentParser(description="매핑에 없는 JIRA 이슈(중복·고아) 취소 처리")
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
    parser.add_argument("--project-key", default="GAM")
    parser.add_argument("--mapping-file", default=".github/jira-mapping.json")
    parser.add_argument("--backend-backlog", default="docs/jira/JIRA_BACKLOG.md")
    parser.add_argument("--dry-run", action="store_true", help="실제 변경 없이 대상만 출력")
    parser.add_argument("--yes", action="store_true", help="확인 없이 취소 전환 실행")
    args = parser.parse_args()

    if not args.jira_url or not args.jira_email or not args.jira_api_token:
        print("오류: JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 필요.", file=sys.stderr)
        sys.exit(1)

    run(
        jira_url=args.jira_url,
        jira_email=args.jira_email,
        jira_api_token=args.jira_api_token,
        project_key=args.project_key,
        mapping_file=args.mapping_file,
        backend_backlog=args.backend_backlog,
        dry_run=args.dry_run,
        yes=args.yes,
    )


if __name__ == "__main__":
    main()
