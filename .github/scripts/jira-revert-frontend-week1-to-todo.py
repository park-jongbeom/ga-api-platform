#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
프론트 1주차 JIRA 이슈만 완료(Done) → 해야 할 일(To Do)로 원복.
백엔드 1주차(GAM-1, GAM-11, GAM-12, GAM-13)는 건드리지 않음.
"""
import os
import sys
import argparse
import base64
import time
import requests
from typing import List


# 프론트 1주차 JIRA 키 (jira-mapping.json 기준: GAMF-1, GAMF-11~18, GAMF-11-1~GAMF-18-3)
FRONT_WEEK1_JIRA_KEYS = [
    "GAM-142",  # GAMF-1 Epic
    "GAM-145", "GAM-146", "GAM-147", "GAM-148", "GAM-149", "GAM-150", "GAM-151", "GAM-152",  # Stories
    "GAM-165", "GAM-166", "GAM-167", "GAM-168",
    "GAM-169", "GAM-170", "GAM-171", "GAM-172",
    "GAM-173", "GAM-174", "GAM-175",
    "GAM-176", "GAM-177", "GAM-178", "GAM-179",
    "GAM-180", "GAM-181", "GAM-182", "GAM-183",
    "GAM-184", "GAM-185", "GAM-186", "GAM-187",
    "GAM-188", "GAM-189", "GAM-190",
    "GAM-191", "GAM-192", "GAM-193",
]


def get_available_transitions(jira_url: str, headers: dict, issue_key: str) -> List[dict]:
    try:
        r = requests.get(f"{jira_url}/rest/api/3/issue/{issue_key}/transitions", headers=headers)
        return r.json().get("transitions", []) if r.status_code == 200 else []
    except Exception:
        return []


def transition_to_todo(jira_url: str, headers: dict, issue_key: str) -> bool:
    """이슈를 해야 할 일(To Do) 또는 Reopen 등 열림 상태로 전환."""
    transitions = get_available_transitions(jira_url, headers, issue_key)
    todo_names = ["to do", "해야 할 일", "reopen", "open", "backlog", "되돌리기"]
    tid = None
    for t in transitions:
        name = (t.get("name") or "").lower()
        if any(x in name for x in todo_names):
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


def main():
    parser = argparse.ArgumentParser(description="프론트 1주차 JIRA 이슈만 완료 → 해야 할 일로 원복")
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
    parser.add_argument("--dry-run", action="store_true", help="실제 전환 없이 대상만 출력")
    args = parser.parse_args()

    if not args.jira_url or not args.jira_email or not args.jira_api_token:
        print("오류: JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 필요.", file=sys.stderr)
        sys.exit(1)

    jira_url = args.jira_url.rstrip("/")
    auth = base64.b64encode(f"{args.jira_email}:{args.jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    print(f"프론트 1주차 원복 대상: {len(FRONT_WEEK1_JIRA_KEYS)}개 (Done → 해야 할 일)")
    if args.dry_run:
        for k in FRONT_WEEK1_JIRA_KEYS:
            print(f"  [DRY RUN] {k}")
        return

    ok, fail = 0, 0
    for k in FRONT_WEEK1_JIRA_KEYS:
        if transition_to_todo(jira_url, headers, k):
            print(f"  ✓ {k} → 해야 할 일")
            ok += 1
        else:
            print(f"  ✗ {k} 전환 실패")
            fail += 1
        time.sleep(0.35)
    print(f"\n완료: 성공 {ok}개, 실패 {fail}개.")


if __name__ == "__main__":
    main()
