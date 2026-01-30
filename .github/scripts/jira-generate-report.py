#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 프로젝트 진행 상황 보고서 생성 스크립트.
전체/완료/진행중/남은 작업을 분석하여 마크다운 보고서를 생성하고,
파일로 저장하거나 GitHub Issue 본문으로 사용할 수 있음.
"""
import os
import re
import sys
import json
import argparse
import base64
import requests
import time
from datetime import datetime
from typing import Dict, List, Optional
from pathlib import Path


DONE_STATUSES = {'done', '완료', 'complete', 'closed', '종료', 'resolved', '해결됨'}
IN_PROGRESS_STATUSES = {'in progress', '진행 중', 'in progress', 'code review', 'testing'}
TO_DO_STATUSES = {'to do', '해야 할 일', 'open', 'backlog'}


def normalize_status(s: str) -> str:
    t = (s or '').strip().lower()
    if t in DONE_STATUSES or any(d in t for d in ['done', '완료', 'complete', 'closed', '종료']):
        return 'done'
    if t in IN_PROGRESS_STATUSES or any(p in t for p in ['진행 중', 'in progress']):
        return 'in_progress'
    return 'to_do'


def load_canonical_keys(mapping_file: str, backend_backlog_path: Optional[str] = None) -> set:
    """
    정규(참조할) 이슈 키 집합 로드.
    - 매핑 파일의 값(GAM-xxx) + 백엔드 백로그에 등장하는 ID(GAM-*, GAM-*-*)를 정규로 간주.
    """
    canonical = set()
    if os.path.exists(mapping_file):
        with open(mapping_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        for k, v in data.items():
            if not k.startswith('_') and isinstance(v, str):
                canonical.add(v)
    if backend_backlog_path and os.path.exists(backend_backlog_path):
        content = Path(backend_backlog_path).read_text(encoding='utf-8')
        # GAM-1, GAM-11, GAM-11-1 등 백엔드 ID 추출 (GAMF- 제외)
        for m in re.finditer(r'\b(GAM-\d+(?:-\d+)?)\b', content):
            canonical.add(m.group(1))
    return canonical


def fetch_all_issues(jira_url: str, headers: dict, project_key: str) -> List[dict]:
    """JIRA 프로젝트의 모든 이슈 조회 (search/jql, 페이지네이션)."""
    url = f"{jira_url}/rest/api/3/search/jql"
    all_issues = []
    next_token = None
    max_results = 100
    while True:
        payload = {
            "jql": f"project = {project_key} ORDER BY key ASC",
            "maxResults": max_results
        }
        if next_token:
            payload["nextPageToken"] = next_token
        try:
            r = requests.post(url, headers=headers, json=payload)
            if r.status_code != 200:
                break
            data = r.json()
            batch = data.get('issues', [])
            if not batch:
                break
            for issue in batch:
                issue_id = issue.get('id')
                if not issue_id:
                    continue
                # 상세 필드가 없을 수 있으므로 개별 조회
                detail_url = f"{jira_url}/rest/api/3/issue/{issue_id}"
                dr = requests.get(detail_url, headers=headers, params={"fields": "key,summary,issuetype,status,duedate,created"})
                if dr.status_code == 200:
                    all_issues.append(dr.json())
            next_token = data.get('nextPageToken')
            if not next_token or data.get('isLast'):
                break
            time.sleep(0.3)
        except Exception:
            break
    return all_issues


def build_report_markdown(
    issues: List[dict],
    report_date: str,
    report_web_url: str,
    project_name: str = "Go Almond Matching"
) -> str:
    """마크다운 보고서 본문 생성."""
    done_list = []
    in_progress_list = []
    to_do_list = []
    epic_timeline = []

    for raw in issues:
        key = raw.get('key', '')
        fields = raw.get('fields', {})
        summary = (fields.get('summary') or '').strip()
        itype = (fields.get('issuetype') or {}).get('name', '')
        status = (fields.get('status') or {}).get('name', '')
        duedate = fields.get('duedate') or ''
        normalized = normalize_status(status)
        entry = f"- **{key}** [{itype}] {summary[:60]}" + (f" (기한: {duedate})" if duedate else "")
        if normalized == 'done':
            done_list.append(entry)
        elif normalized == 'in_progress':
            in_progress_list.append(entry)
        else:
            to_do_list.append(entry)
        if '에픽' in itype or 'Epic' in itype:
            epic_timeline.append(f"- **{key}** {summary[:50]}" + (f" ~ {duedate}" if duedate else ""))

    total = len(issues)
    done_count = len(done_list)
    in_progress_count = len(in_progress_list)
    to_do_count = len(to_do_list)
    progress_pct = round(100 * done_count / total, 1) if total else 0
    bar_len = 20
    filled = int(bar_len * done_count / total) if total else 0
    bar = "█" * filled + "░" * (bar_len - filled)

    lines = [
        f"# 프로젝트 진행 상황 보고서",
        f"",
        f"**프로젝트**: {project_name}  ",
        f"**보고일**: {report_date}  ",
        f"**작업물 확인**: [{report_web_url}]({report_web_url})",
        f"",
        f"---",
        f"",
        f"## 진행도 요약",
        f"",
        f"| 구분 | 건수 |",
        f"|------|------|",
        f"| 전체 이슈 | {total} |",
        f"| 완료 | {done_count} |",
        f"| 진행 중 | {in_progress_count} |",
        f"| 남은 작업 | {to_do_count} |",
        f"",
        f"**진행률**: `{bar}` **{progress_pct}%**",
        f"",
        f"---",
        f"",
        f"## 전체 일정 (Epic 타임라인)",
        f"",
    ]
    lines.extend(epic_timeline if epic_timeline else ["- (Epic 없음)"])
    lines.extend([
        f"",
        f"---",
        f"",
        f"## 완료된 작업",
        f"",
    ])
    lines.extend(done_list if done_list else ["- 없음"])
    lines.extend([
        f"",
        f"## 진행 중인 작업",
        f"",
    ])
    lines.extend(in_progress_list if in_progress_list else ["- 없음"])
    lines.extend([
        f"",
        f"## 남은 작업",
        f"",
    ])
    lines.extend(to_do_list if to_do_list else ["- 없음"])
    lines.extend([
        f"",
        f"---",
        f"",
        f"**작업물 링크**: [Go Almond]({report_web_url})",
        f"",
    ])
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description='JIRA 진행 상황 보고서 생성')
    parser.add_argument('--jira-url', default=os.getenv('JIRA_URL'), help='JIRA URL')
    parser.add_argument('--jira-email', default=os.getenv('JIRA_EMAIL'), help='JIRA 이메일')
    parser.add_argument('--jira-api-token', default=os.getenv('JIRA_API_TOKEN'), help='JIRA API 토큰')
    parser.add_argument('--project-key', default='GAM', help='프로젝트 키')
    parser.add_argument('--report-web-url', default='https://go-almond.ddnsfree.com/', help='작업물 확인 URL')
    parser.add_argument('--output', '-o', default='', help='저장할 마크다운 파일 경로 (없으면 stdout)')
    parser.add_argument('--date', default='', help='보고일 (YYYY-MM-DD, 기본: 오늘)')
    parser.add_argument('--canonical-only', action='store_true',
                        help='매핑/백로그에 있는 정규 이슈만 포함 (중복·고아 이슈 제외)')
    parser.add_argument('--mapping-file', default='.github/jira-mapping.json', help='정규 이슈 목록용 매핑 파일')
    parser.add_argument('--backend-backlog', default='docs/jira/JIRA_BACKLOG.md', help='백엔드 백로그 (정규 키 추출용)')
    args = parser.parse_args()

    jira_url = (args.jira_url or '').rstrip('/')
    if not jira_url or not args.jira_email or not args.jira_api_token:
        print("JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN이 필요합니다.", file=sys.stderr)
        sys.exit(1)

    report_date = args.date or datetime.now().strftime('%Y-%m-%d')
    auth = base64.b64encode(f"{args.jira_email}:{args.jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json"
    }

    print("JIRA 이슈 조회 중...", file=sys.stderr)
    issues = fetch_all_issues(jira_url, headers, args.project_key)
    print(f"조회 완료: {len(issues)}개 이슈", file=sys.stderr)

    if args.canonical_only:
        canonical_keys = load_canonical_keys(args.mapping_file, args.backend_backlog)
        before = len(issues)
        issues = [i for i in issues if (i.get('key') or '') in canonical_keys]
        print(f"정규 이슈만 포함: {before}개 → {len(issues)}개 (중복·고아 제외)", file=sys.stderr)

    markdown = build_report_markdown(
        issues,
        report_date=report_date,
        report_web_url=args.report_web_url
    )

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(markdown, encoding='utf-8')
        print(f"보고서 저장: {args.output}", file=sys.stderr)
    else:
        print(markdown)


if __name__ == '__main__':
    main()
