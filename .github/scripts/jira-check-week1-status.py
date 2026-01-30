#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
백엔드 1주차(Week 1) JIRA 이슈만 조회해 완료/미완료 상태를 출력.
백엔드 개발자용. 프론트 1주차는 제외.
"""
import os
import sys
import argparse
import base64
import time
import requests
from typing import List, Tuple

# 백엔드 1주차: 에픽·스토리만 (JIRA에 키가 존재하는 항목)
BACKEND_WEEK1_KEYS = ["GAM-1", "GAM-11", "GAM-12", "GAM-13"]

DONE_NAMES = ("done", "완료", "complete", "closed", "종료", "resolved", "해결됨")


def is_done(status_name: str) -> bool:
    if not status_name:
        return False
    s = status_name.strip().lower()
    return any(d in s for d in DONE_NAMES)


def fetch_issue(jira_url: str, headers: dict, key: str) -> Tuple[str, str, str]:
    """(key, summary, status_name) 반환. 없으면 (key, '', '')"""
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{key}",
            headers=headers,
            params={"fields": "key,summary,status"},
        )
        if r.status_code != 200:
            return (key, "", "")
        d = r.json()
        key = d.get("key", key)
        fields = d.get("fields", {})
        summary = (fields.get("summary") or "").strip()
        status = fields.get("status") or {}
        status_name = (status.get("name") or "").strip()
        return (key, summary, status_name)
    except Exception:
        return (key, "", "")


def main():
    parser = argparse.ArgumentParser(description="백엔드 1주차 JIRA 완료/미완료 확인")
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
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

    print("백엔드 1주차(Week 1) JIRA 상태 조회 중...")
    done_list: List[Tuple[str, str, str]] = []
    not_done_list: List[Tuple[str, str, str]] = []
    for key in BACKEND_WEEK1_KEYS:
        k, summary, status_name = fetch_issue(jira_url, headers, key)
        time.sleep(0.25)
        if not status_name:
            not_done_list.append((k, summary or "(조회 실패)", status_name or "?"))
            continue
        if is_done(status_name):
            done_list.append((k, summary, status_name))
        else:
            not_done_list.append((k, summary, status_name))

    total = len(BACKEND_WEEK1_KEYS)
    done_count = len(done_list)
    not_done_count = len(not_done_list)
    print(f"\n백엔드 1주차: 완료 {done_count}개 / 미완료 {not_done_count}개 (총 {total}개)")
    if not_done_list:
        print("\n[ 미완료 작업 ]")
        for k, summary, status_name in not_done_list:
            title = (summary[:50] + "…") if len(summary) > 50 else summary
            print(f"  - {k} [{status_name}] {title}")
        print("\n백엔드 1주차 미완료: 위 항목을 진행해 주세요.")
    else:
        print("\n백엔드 1주차 모두 완료.")


if __name__ == "__main__":
    main()
