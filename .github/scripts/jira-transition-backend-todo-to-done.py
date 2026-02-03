#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
백엔드 완료 항목(코드 검증됨) 중 JIRA에서 '해야 할 일(To Do)' 상태인 이슈만 '완료(Done)'로 전환.
reports/backend-completion-verification.md 기준. 백로그 문서 키 = JIRA 키 (매핑 불필요).
"""
import os
import sys
import argparse
import base64
import time
import requests
from typing import List

# 코드 검증 완료된 백엔드 이슈 (JIRA 키 = 백로그 키; GAM-55, 70, 71 제외)
BACKEND_VERIFIED_DONE_JIRA_KEYS: List[str] = [
    "GAM-1",
    "GAM-7", "GAM-8", "GAM-9", "GAM-10",
    "GAM-11", "GAM-12", "GAM-13",
    "GAM-20", "GAM-21", "GAM-22", "GAM-23",
    "GAM-51",  # UserPreference Entity (user_preferences 테이블)
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
    """이슈 현재 상태 이름 반환. 실패 시 빈 문자열."""
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
        description="백엔드 완료 항목 중 To Do 상태인 이슈만 JIRA에서 Done으로 전환"
    )
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
    parser.add_argument("--dry-run", action="store_true", help="전환 없이 대상만 출력")
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

    jira_keys_to_check = sorted(set(BACKEND_VERIFIED_DONE_JIRA_KEYS))
    to_transition: List[str] = []
    already_done: List[str] = []
    not_found_or_error: List[str] = []

    for key in sorted(jira_keys_to_check):
        status = get_issue_status(jira_url, headers, key)
        time.sleep(0.2)
        if not status:
            not_found_or_error.append(key)
            continue
        if is_done_status(status):
            already_done.append(f"{key} ({status})")
        else:
            to_transition.append(key)

    print(f"백엔드 검증 완료 이슈 (JIRA 키 {len(jira_keys_to_check)}개)")
    print(f"  이미 완료: {len(already_done)}개 — 전환 생략")
    for x in already_done:
        print(f"    - {x}")
    if not_found_or_error:
        print(f"  조회 실패: {not_found_or_error}")
    print(f"  To Do 등 → 완료 전환 대상: {len(to_transition)}개 — {to_transition}")

    if not to_transition:
        print("\n전환할 이슈 없음.")
        return

    if args.dry_run:
        for k in to_transition:
            print(f"  [DRY RUN] {k} → 완료")
        return

    ok, fail = 0, 0
    for k in to_transition:
        if transition_to_done(jira_url, headers, k):
            print(f"  ✓ {k} 완료")
            ok += 1
        else:
            print(f"  ✗ {k} 전환 실패")
            fail += 1
        time.sleep(0.35)
    print(f"\n완료: 성공 {ok}개, 실패 {fail}개.")


if __name__ == "__main__":
    main()
