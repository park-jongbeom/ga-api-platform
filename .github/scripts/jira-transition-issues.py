#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
지정된 JIRA 이슈를 '완료' 상태로 전환.
사용: python3 jira-transition-issues.py --issues GAM-21,GAM-22,GAM-23
"""
import os
import sys
import argparse
import base64
import time
import requests


def load_jira_env(paths):
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


def get_transitions(jira_url, headers, issue_key):
    try:
        r = requests.get(f"{jira_url}/rest/api/3/issue/{issue_key}/transitions", headers=headers)
        return r.json().get("transitions", []) if r.status_code == 200 else []
    except Exception:
        return []


def transition_to_done(jira_url, headers, issue_key):
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


def main():
    parser = argparse.ArgumentParser(description="지정 JIRA 이슈를 완료 상태로 전환")
    parser.add_argument("--issues", required=True, help="쉼표 구분 이슈 키 (예: GAM-21,GAM-22,GAM-23)")
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not args.jira_url:
        load_jira_env(["docs/jira/jira.env", "jira.env"])

    jira_url = (args.jira_url or os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = args.jira_email or os.getenv("JIRA_EMAIL", "")
    jira_api_token = args.jira_api_token or os.getenv("JIRA_API_TOKEN", "")

    if not jira_url or not jira_email or not jira_api_token:
        print("오류: JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 필요. docs/jira/jira.env 참조.", file=sys.stderr)
        sys.exit(1)

    keys = [k.strip() for k in args.issues.split(",") if k.strip()]
    if not keys:
        print("오류: --issues에 이슈 키를 입력하세요.", file=sys.stderr)
        sys.exit(1)

    auth = base64.b64encode(f"{jira_email}:{jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    print(f"이슈 {len(keys)}개 → 완료 전환: {', '.join(keys)}")
    if args.dry_run:
        for k in keys:
            print(f"  [DRY RUN] {k}")
        return

    ok, fail = 0, 0
    for k in keys:
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
