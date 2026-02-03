#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
불일치 이슈(.github/jira-mismatch-issues.json)를
JIRA에서 일괄 완료(Done) 처리.
"""
import argparse
import base64
import json
import os
import sys
import time
from pathlib import Path

import requests

PROJECT_ROOT = Path(__file__).resolve().parents[2]
MISMATCH_FILE = PROJECT_ROOT / ".github" / "jira-mismatch-issues.json"


def load_jira_env(paths: list) -> None:
    for p in paths:
        path = PROJECT_ROOT / p if not os.path.isabs(p) else Path(p)
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    if "=" in line and not line.startswith("#"):
                        k, v = line.strip().split("=", 1)
                        os.environ[k.strip()] = v.strip().strip("'\"")


def get_transitions(jira_url: str, headers: dict, issue_key: str) -> list:
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
            headers=headers,
            timeout=10,
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


def main() -> None:
    parser = argparse.ArgumentParser(description="불일치 이슈 일괄 완료 처리")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="실제 전환 없이 대상만 출력",
    )
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""

    if not all([jira_url, jira_email, jira_token]):
        print("오류: JIRA 인증 정보 필요", file=sys.stderr)
        sys.exit(1)

    if not MISMATCH_FILE.exists():
        print(
            f"오류: {MISMATCH_FILE} 없음. 먼저 jira-find-mismatched-issues.py 실행",
            file=sys.stderr,
        )
        sys.exit(1)

    with open(MISMATCH_FILE, "r", encoding="utf-8") as f:
        mismatch_issues = json.load(f)

    if not mismatch_issues:
        print("완료 처리할 불일치 이슈 없음.")
        return

    print(f"일괄 완료 처리 대상: {len(mismatch_issues)}개")
    for issue in mismatch_issues:
        label = issue.get("title") or issue.get("description", "")
        label_short = label[:60] + "..." if len(label) > 60 else label
        print(f"  {issue['key']}: {label_short}")

    if args.dry_run:
        print("\n[DRY RUN] 실제 전환 없이 종료")
        return

    auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
    }

    success, fail = 0, 0
    for issue in mismatch_issues:
        key = issue["key"]
        if transition_to_done(jira_url, headers, key):
            print(f"  ✓ {key} 완료")
            success += 1
        else:
            print(f"  ✗ {key} 전환 실패")
            fail += 1
        time.sleep(0.4)

    print(f"\n완료: 성공 {success}개, 실패 {fail}개.")


if __name__ == "__main__":
    main()
