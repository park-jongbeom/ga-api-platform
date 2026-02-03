#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
하위 작업이 모두 완료된 백엔드 에픽을 JIRA에서 완료(Done) 처리.
백로그 문서 키 = JIRA 키 (매핑 불필요).
"""
import os
import sys
import argparse
import base64
import time
import requests
from typing import Dict, List

# 에픽별 하위 Story JIRA 키 (JIRA_BACKLOG.md Epic/Story 구조 기준)
BACKEND_EPIC_CHILDREN: Dict[str, List[str]] = {
    "GAM-1": ["GAM-7", "GAM-8", "GAM-9"],
    "GAM-2": ["GAM-10", "GAM-20", "GAM-21", "GAM-22", "GAM-23"],
    "GAM-3": ["GAM-31", "GAM-32", "GAM-33"],
    "GAM-4": ["GAM-41", "GAM-42", "GAM-43", "GAM-44"],
    "GAM-5": ["GAM-51", "GAM-52", "GAM-53", "GAM-54", "GAM-55"],
    "GAM-6": ["GAM-61", "GAM-62", "GAM-63"],
}


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


def get_issue_status(jira_url: str, headers: dict, issue_key: str) -> str:
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}?fields=status",
            headers=headers,
        )
        if r.status_code != 200:
            return ""
        return ((r.json().get("fields") or {}).get("status") or {}).get("name") or ""
    except Exception:
        return ""


def get_transitions(jira_url: str, headers: dict, issue_key: str) -> list:
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
            headers=headers,
        )
        return r.json().get("transitions", []) if r.status_code == 200 else []
    except Exception:
        return []


def transition_to_done(jira_url: str, headers: dict, issue_key: str) -> bool:
    transitions = get_transitions(jira_url, headers, issue_key)
    done_names = ["done", "완료", "complete", "closed", "종료", "resolved", "해결됨"]
    tid = None
    for t in transitions:
        name = (t.get("name") or "").lower()
        if any(d in name for d in done_names):
            tid = t.get("id")
            break
    if not tid and transitions:
        tid = transitions[0].get("id")
    if not tid:
        return False
    r = requests.post(
        f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
        headers=headers,
        json={"transition": {"id": tid}},
    )
    return r.status_code == 204


def is_done_status(status_name: str) -> bool:
    done_names = ["done", "완료", "complete", "closed", "종료", "resolved", "해결됨"]
    return (status_name or "").strip().lower() in [d.lower() for d in done_names]


def main():
    parser = argparse.ArgumentParser(
        description="하위가 모두 완료된 백엔드 에픽을 JIRA에서 Done으로 전환"
    )
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
    parser.add_argument("--dry-run", action="store_true")
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

    to_done: List[str] = []
    already_done: List[str] = []
    children_not_all_done: List[str] = []

    for epic_key, child_keys in BACKEND_EPIC_CHILDREN.items():
        time.sleep(0.2)
        epic_status = get_issue_status(jira_url, headers, epic_key)
        if is_done_status(epic_status):
            already_done.append(epic_key)
            continue
        all_children_done = True
        for ck in sorted(child_keys):
            time.sleep(0.2)
            st = get_issue_status(jira_url, headers, ck)
            if not is_done_status(st):
                all_children_done = False
                break
        if all_children_done:
            to_done.append(epic_key)
        else:
            children_not_all_done.append(epic_key)

    print("하위가 모두 완료된 에픽만 완료 처리 대상")
    print(f"  이미 완료(에픽): {already_done}")
    print(f"  하위 미완료로 스킵: {children_not_all_done}")
    print(f"  완료 전환 대상: {to_done}")

    if not to_done:
        print("\n전환할 에픽 없음.")
        return

    if args.dry_run:
        for k in to_done:
            print(f"  [DRY RUN] {k} -> 완료")
        return

    for epic_key in to_done:
        if transition_to_done(jira_url, headers, epic_key):
            print(f"  ✓ {epic_key} 완료")
        else:
            print(f"  ✗ {epic_key} 전환 실패")
        time.sleep(0.35)
    print("\n완료.")


if __name__ == "__main__":
    main()
