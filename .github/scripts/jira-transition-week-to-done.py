#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 백로그 기준 1주차(Week 1) 이슈를 모두 '완료' 상태로 전환.
백엔드(JIRA_BACKLOG) + 프론트(FRONT_JIRA_BACKLOG) Week 1 항목을 매핑하여 JIRA API로 전환.
"""
import os
import sys
import json
import argparse
import base64
import time
import requests
from typing import Set, List


# 백엔드 Week 1 (JIRA_BACKLOG): Epic 1, Story 11,12,13 + Tasks
BACKEND_WEEK1_IDS = [
    "GAM-1",
    "GAM-11", "GAM-12", "GAM-13",
    "GAM-11-1", "GAM-11-2", "GAM-11-3", "GAM-11-4", "GAM-11-5", "GAM-11-6",
    "GAM-12-1", "GAM-12-2", "GAM-12-3", "GAM-12-4", "GAM-12-5",
    "GAM-13-1", "GAM-13-2", "GAM-13-3", "GAM-13-4", "GAM-13-5",
]

# 프론트 Week 1 (FRONT_JIRA_BACKLOG): Epic GAMF-1, Stories GAMF-11~18 + Tasks
FRONT_WEEK1_IDS = [
    "GAMF-1",
    "GAMF-11", "GAMF-12", "GAMF-13", "GAMF-14", "GAMF-15", "GAMF-16", "GAMF-17", "GAMF-18",
    "GAMF-11-1", "GAMF-11-2", "GAMF-11-3", "GAMF-11-4",
    "GAMF-12-1", "GAMF-12-2", "GAMF-12-3", "GAMF-12-4",
    "GAMF-13-1", "GAMF-13-2", "GAMF-13-3",
    "GAMF-14-1", "GAMF-14-2", "GAMF-14-3", "GAMF-14-4",
    "GAMF-15-1", "GAMF-15-2", "GAMF-15-3", "GAMF-15-4",
    "GAMF-16-1", "GAMF-16-2", "GAMF-16-3", "GAMF-16-4",
    "GAMF-17-1", "GAMF-17-2", "GAMF-17-3",
    "GAMF-18-1", "GAMF-18-2", "GAMF-18-3",
]


def load_mapping(mapping_file: str) -> dict:
    m = {}
    if os.path.exists(mapping_file):
        with open(mapping_file, "r", encoding="utf-8") as f:
            data = json.load(f)
        for k, v in data.items():
            if not k.startswith("_") and isinstance(v, str):
                m[k] = v
    return m


def get_transitions(jira_url: str, headers: dict, issue_key: str) -> list:
    try:
        r = requests.get(f"{jira_url}/rest/api/3/issue/{issue_key}/transitions", headers=headers)
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


def main():
    parser = argparse.ArgumentParser(description="JIRA 1주차 이슈를 완료 상태로 전환")
    parser.add_argument("--jira-url", default=os.getenv("JIRA_URL"))
    parser.add_argument("--jira-email", default=os.getenv("JIRA_EMAIL"))
    parser.add_argument("--jira-api-token", default=os.getenv("JIRA_API_TOKEN"))
    parser.add_argument("--mapping-file", default=".github/jira-mapping.json")
    parser.add_argument("--project-key", default="GAM")
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

    mapping = load_mapping(args.mapping_file)
    jira_keys: Set[str] = set()

    for bid in BACKEND_WEEK1_IDS:
        key = mapping.get(bid) or (bid if bid.startswith(args.project_key + "-") and "GAMF" not in bid else None)
        if key:
            jira_keys.add(key)

    for fid in FRONT_WEEK1_IDS:
        key = mapping.get(fid)
        if key:
            jira_keys.add(key)

    ordered = sorted(jira_keys, key=lambda k: (int(k.split("-")[1]) if len(k.split("-")) >= 2 and k.split("-")[1].isdigit() else 0, k))
    print(f"1주차(Week 1) JIRA 이슈: {len(ordered)}개 → 완료 전환")
    if args.dry_run:
        for k in ordered:
            print(f"  [DRY RUN] {k}")
        return

    ok, fail = 0, 0
    for k in ordered:
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
