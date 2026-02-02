#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
백엔드 에픽(GAM-1~6)과 하위 스토리/작업을 JIRA에서 연동.
JIRA_BACKLOG.md 기준 Epic ID -> Story 목록 매핑으로 Epic Link 또는 Issue Link 설정.
백로그 키와 JIRA 실제 키가 다를 경우 _jiraToBacklog 역매핑으로 실제 JIRA 키를 사용.
"""
import os
import sys
import json
import argparse
import base64
import time
import requests
from typing import Dict, List, Set

# 백엔드 에픽 ID -> 하위 스토리/작업 목록 (백로그 키 기준)
BACKEND_EPIC_CHILDREN: Dict[str, List[str]] = {
    "GAM-1": ["GAM-11", "GAM-12", "GAM-13"],
    "GAM-2": ["GAM-20", "GAM-21", "GAM-22", "GAM-23"],
    "GAM-3": ["GAM-31", "GAM-32", "GAM-33"],
    "GAM-4": ["GAM-41", "GAM-42", "GAM-43", "GAM-44"],
    "GAM-5": ["GAM-51", "GAM-52", "GAM-53", "GAM-54", "GAM-55"],
    "GAM-6": ["GAM-61", "GAM-62", "GAM-63"],
}

# Epic Link로 자주 쓰이는 custom field ID (프로젝트별로 다를 수 있음)
EPIC_LINK_FIELD_IDS = ["customfield_10011", "customfield_10014", "customfield_10015", "customfield_10016"]


def load_jira_to_backlog(mapping_file_path: str) -> Dict[str, str]:
    """매핑 파일의 _jiraToBacklog (JIRA 키 → 백로그 키) 로드."""
    path = mapping_file_path
    if not os.path.isabs(path):
        base = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        path = os.path.join(base, path.lstrip("/"))
    out: Dict[str, str] = {}
    if not os.path.exists(path):
        return out
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    j2b = data.get("_jiraToBacklog")
    if isinstance(j2b, dict):
        for k, v in j2b.items():
            if isinstance(k, str) and isinstance(v, str):
                out[k] = v
    return out


def expand_backlog_children_to_jira_keys(
    child_keys: List[str], jira_to_backlog: Dict[str, str]
) -> Set[str]:
    """백로그 기준 자식 키 목록을 실제 JIRA에서 사용할 이슈 키 집합으로 변환."""
    backlog_to_jira: Dict[str, List[str]] = {}
    for jira_key, backlog_key in jira_to_backlog.items():
        backlog_to_jira.setdefault(backlog_key, []).append(jira_key)
    result: Set[str] = set()
    for c in child_keys:
        result.add(c)
        result.update(backlog_to_jira.get(c, []))
    return result


def load_jira_env(paths: List[str]) -> None:
    for p in paths:
        if not p or not os.path.exists(p):
            continue
        with open(p, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, _, v = line.partition("=")
                k, v = k.strip(), v.strip()
                if k:
                    os.environ[k] = v


def link_via_epic_link_field(
    jira_url: str, headers: dict, child_key: str, epic_key: str
) -> bool:
    """Epic Link custom field 또는 parent 필드로 자식 이슈를 에픽에 연결."""
    # 1) parent 필드 (JIRA Cloud Next-Gen 등)
    try:
        r = requests.put(
            f"{jira_url}/rest/api/3/issue/{child_key}",
            headers=headers,
            json={"fields": {"parent": {"key": epic_key}}},
        )
        if r.status_code == 204:
            return True
    except Exception:
        pass
    # 2) Epic Link custom field
    for field_id in EPIC_LINK_FIELD_IDS:
        try:
            r = requests.put(
                f"{jira_url}/rest/api/3/issue/{child_key}",
                headers=headers,
                json={"fields": {field_id: epic_key}},
            )
            if r.status_code == 204:
                return True
        except Exception:
            continue
    return False


def get_issue_link_types(jira_url: str, headers: dict) -> List[dict]:
    """사용 가능한 이슈 링크 타입 목록 조회."""
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issueLinkType",
            headers=headers,
        )
        if r.status_code == 200:
            return r.json().get("issueLinkTypes", [])
    except Exception:
        pass
    return []


def link_via_issue_link(
    jira_url: str, headers: dict, child_key: str, epic_key: str, link_type_name: str
) -> bool:
    """Issue Link로 자식 이슈를 에픽에 연결. (outward=에픽, inward=자식 등)"""
    try:
        # "Epic-Story" 또는 "relates to" 등: outwardIssue=에픽, inwardIssue=자식
        payload = {
            "type": {"name": link_type_name},
            "inwardIssue": {"key": child_key},
            "outwardIssue": {"key": epic_key},
        }
        r = requests.post(
            f"{jira_url}/rest/api/3/issueLink",
            headers=headers,
            json=payload,
        )
        if r.status_code == 201:
            return True
        # 반대 방향 시도
        payload = {
            "type": {"name": link_type_name},
            "inwardIssue": {"key": epic_key},
            "outwardIssue": {"key": child_key},
        }
        r = requests.post(
            f"{jira_url}/rest/api/3/issueLink",
            headers=headers,
            json=payload,
        )
        return r.status_code == 201
    except Exception:
        return False


def get_current_epic_link(jira_url: str, headers: dict, issue_key: str) -> str:
    """이슈에 설정된 Epic Link(에픽 키) 조회. 없으면 빈 문자열."""
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}?fields=parent",
            headers=headers,
        )
        if r.status_code != 200:
            return ""
        fields = (r.json().get("fields") or {})
        parent = fields.get("parent")
        if parent:
            return (parent.get("key") or "")
        return ""
    except Exception:
        return ""


def main():
    parser = argparse.ArgumentParser(
        description="백엔드 에픽과 하위 스토리 JIRA 연동 (Epic Link / Issue Link)"
    )
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--epic", default=None, help="특정 에픽만 처리 (예: GAM-2)")
    parser.add_argument("--mapping-file", default=".github/jira-mapping.json", help="JIRA↔백로그 매핑 파일")
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    jira_url = (args.jira_url or os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = args.jira_email or os.getenv("JIRA_EMAIL", "")
    jira_api_token = args.jira_api_token or os.getenv("JIRA_API_TOKEN", "")

    if not jira_url or not jira_email or not jira_api_token:
        print(
            "오류: JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 필요. docs/jira/jira.env 참조.",
            file=sys.stderr,
        )
        sys.exit(1)

    auth = base64.b64encode(f"{jira_email}:{jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    epics_to_process = (
        {args.epic: BACKEND_EPIC_CHILDREN[args.epic]}
        if args.epic and args.epic in BACKEND_EPIC_CHILDREN
        else BACKEND_EPIC_CHILDREN
    )

    project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    mapping_path = os.path.normpath(os.path.join(project_root, args.mapping_file.lstrip("/")))
    jira_to_backlog = load_jira_to_backlog(mapping_path)

    if args.dry_run:
        print("[DRY RUN] 연동 대상:")
        for epic_key, children in epics_to_process.items():
            jira_keys = expand_backlog_children_to_jira_keys(children, jira_to_backlog)
            print(f"  {epic_key} -> {sorted(jira_keys)} (백로그: {children})")
        return

    link_types = get_issue_link_types(jira_url, headers)
    link_type_names = [t.get("name") for t in link_types if t.get("name")]
    try_names = ["Epic-Story Link", "Epic-Story", "relates to", "Parent-Child", "Child"]
    candidate_link_types = [n for n in try_names if n in link_type_names]
    if not candidate_link_types and link_type_names:
        candidate_link_types = link_type_names[:3]

    ok, skip, fail = 0, 0, 0
    for epic_key, child_keys in epics_to_process.items():
        jira_child_keys = expand_backlog_children_to_jira_keys(child_keys, jira_to_backlog)
        print(f"\n에픽 {epic_key}")
        for child_key in sorted(jira_child_keys):
            time.sleep(0.2)
            current = get_current_epic_link(jira_url, headers, child_key)
            if current == epic_key:
                print(f"  ⊘ {child_key} 이미 {epic_key} 연결됨")
                skip += 1
                continue
            if current and current != epic_key:
                print(f"  ⚠ {child_key} 다른 에픽({current}) 연결됨 — 덮어쓰기 시도")

            linked = link_via_epic_link_field(jira_url, headers, child_key, epic_key)
            if linked:
                print(f"  ✓ {child_key} -> {epic_key} (Epic Link 필드)")
                ok += 1
                time.sleep(0.3)
                continue

            for link_name in candidate_link_types:
                if link_via_issue_link(jira_url, headers, child_key, epic_key, link_name):
                    print(f"  ✓ {child_key} -> {epic_key} (Issue Link: {link_name})")
                    ok += 1
                    time.sleep(0.3)
                    break
            else:
                print(f"  ✗ {child_key} -> {epic_key} 연동 실패 (수동 연결 필요)")
                fail += 1
            time.sleep(0.35)

    print(f"\n완료: 성공 {ok}개, 스킵 {skip}개, 실패 {fail}개.")


if __name__ == "__main__":
    main()
