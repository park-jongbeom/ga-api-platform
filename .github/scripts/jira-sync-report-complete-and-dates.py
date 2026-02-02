#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
리포트와 JIRA 일정 동기화: GAM-7,8,9,10 완료 처리 + GAM-7,8,9,10 및 GAM-70,71 기한(duedate) 설정.
"""
import os
import sys
import argparse
import base64
import time
import requests
from typing import List, Tuple

# 완료 처리할 이슈 (리포트상 이미 완료로 반영된 스토리)
TO_DONE_KEYS: List[str] = ["GAM-7", "GAM-8", "GAM-9", "GAM-10"]

# 기한만 설정할 이슈 (key, duedate YYYY-MM-DD)
DUEDATE_ONLY: List[Tuple[str, str]] = [
    ("GAM-7", "2026-01-29"),
    ("GAM-8", "2026-01-29"),
    ("GAM-9", "2026-01-29"),
    ("GAM-10", "2026-01-29"),
    ("GAM-70", "2026-02-12"),
    ("GAM-71", "2026-02-12"),
]


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


def set_duedate(jira_url: str, headers: dict, issue_key: str, duedate: str) -> bool:
    """JIRA 이슈에 duedate 설정."""
    url = f"{jira_url}/rest/api/3/issue/{issue_key}"
    payload = {"fields": {"duedate": duedate}}
    try:
        r = requests.put(url, headers=headers, json=payload)
        return r.status_code == 204
    except Exception:
        return False


def main():
    parser = argparse.ArgumentParser(
        description="GAM-7,8,9,10 완료 처리 및 GAM-7,8,9,10, GAM-70,71 기한 설정"
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

    if args.dry_run:
        print("[DRY RUN] 완료 처리 대상:", TO_DONE_KEYS)
        print("[DRY RUN] 기한 설정:", DUEDATE_ONLY)
        return

    # 1) GAM-7,8,9,10 중 To Do 등 → 완료 전환
    print("1) 완료 처리 (GAM-7, 8, 9, 10)")
    for key in TO_DONE_KEYS:
        status = get_issue_status(jira_url, headers, key)
        time.sleep(0.2)
        if not status:
            print(f"  ✗ {key} 조회 실패")
            continue
        if is_done_status(status):
            print(f"  ⊘ {key} 이미 완료 ({status})")
        else:
            if transition_to_done(jira_url, headers, key):
                print(f"  ✓ {key} 완료")
            else:
                print(f"  ✗ {key} 전환 실패")
        time.sleep(0.35)

    # 2) 기한(duedate) 설정
    print("\n2) 기한(duedate) 설정")
    for key, duedate in DUEDATE_ONLY:
        if set_duedate(jira_url, headers, key, duedate):
            print(f"  ✓ {key} duedate={duedate}")
        else:
            print(f"  ✗ {key} duedate 설정 실패")
        time.sleep(0.3)

    print("\n완료.")


if __name__ == "__main__":
    main()
