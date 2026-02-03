#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
코드 검증 결과(.github/code-completion-verification.json)와
JIRA 실제 상태를 비교하여 완료 처리 대상 추출.
"""
import base64
import json
import os
import sys
import time
from pathlib import Path

import requests

PROJECT_ROOT = Path(__file__).resolve().parents[2]
VERIFICATION_FILE = PROJECT_ROOT / ".github" / "code-completion-verification.json"
OUTPUT_FILE = PROJECT_ROOT / ".github" / "jira-mismatch-issues.json"


def load_jira_env(paths: list) -> None:
    for p in paths:
        path = PROJECT_ROOT / p if not os.path.isabs(p) else Path(p)
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    if "=" in line and not line.startswith("#"):
                        k, v = line.strip().split("=", 1)
                        os.environ[k.strip()] = v.strip().strip("'\"")


def get_issue_status(jira_url: str, headers: dict, issue_key: str):
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}?fields=status",
            headers=headers,
            timeout=10,
        )
        if r.status_code != 200:
            return None
        return r.json().get("fields", {}).get("status", {}).get("name")
    except Exception:
        return None


def is_todo_status(status_name) -> bool:
    """해야 할 일/To Do 상태 판단."""
    if not status_name:
        return False
    todo_names = ["해야 할 일", "to do", "todo", "시작 전", "open"]
    return (status_name or "").strip().lower() in [t.lower() for t in todo_names]


def main() -> None:
    load_jira_env(["docs/jira/jira.env", "jira.env"])

    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""

    if not all([jira_url, jira_email, jira_token]):
        print("오류: JIRA 인증 정보 필요", file=sys.stderr)
        sys.exit(1)

    if not VERIFICATION_FILE.exists():
        print(
            f"오류: {VERIFICATION_FILE} 없음. 먼저 jira-verify-code-completion.py 실행",
            file=sys.stderr,
        )
        sys.exit(1)

    with open(VERIFICATION_FILE, "r", encoding="utf-8") as f:
        verification = json.load(f)

    auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
    }

    mismatch_issues = []
    # Task 불일치 (기존: completed 또는 tasks_completed)
    tasks_completed = verification.get("tasks_completed", verification.get("completed", []))
    for item in tasks_completed:
        key = item["key"]
        time.sleep(0.2)
        status = get_issue_status(jira_url, headers, key)
        if is_todo_status(status):
            mismatch_issues.append(
                {
                    "key": key,
                    "type": "Task",
                    "description": item.get("description", ""),
                    "files": item.get("files", []),
                    "jira_status": status,
                }
            )
            print(f"불일치(Task): {key} (코드 완료, JIRA: {status})")

    # Story 불일치: 코드 기준 Story 완료인데 JIRA는 해야 할 일
    for story in verification.get("stories_completed", []):
        key = story["key"]
        time.sleep(0.2)
        status = get_issue_status(jira_url, headers, key)
        if is_todo_status(status):
            mismatch_issues.append(
                {
                    "key": key,
                    "type": "Story",
                    "title": story.get("title", ""),
                    "completion_rate": story.get("completion_rate", 0),
                    "jira_status": status,
                }
            )
            rate_pct = story.get("completion_rate", 0) * 100
            print(f"불일치(Story): {key} (하위 작업 {rate_pct:.0f}% 완료, JIRA: {status})")

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(mismatch_issues, f, indent=2, ensure_ascii=False)

    print(f"\n불일치 이슈: {len(mismatch_issues)}개")
    print(f"결과 저장: {OUTPUT_FILE}")

    if mismatch_issues:
        print("\n완료 처리 대상:")
        for issue in mismatch_issues:
            if issue.get("type") == "Story":
                label = issue.get("title", "")
            else:
                label = issue.get("description", "")
            label_short = label[:60] + "..." if len(label) > 60 else label
            print(f"  {issue['key']}: {label_short}")


if __name__ == "__main__":
    main()
