#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
매핑 테이블 기준으로 잘못된 Epic Link를 일괄 수정.
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
MAPPING_FILE = PROJECT_ROOT / ".github" / "jira-to-backlog-mapping.json"

# Epic Link custom field ID 후보
EPIC_LINK_FIELD_IDS = ["customfield_10011", "customfield_10014", "customfield_10015", "customfield_10016"]


def load_jira_env(paths: list) -> None:
    for p in paths:
        path = PROJECT_ROOT / p if not os.path.isabs(p) else Path(p)
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    if "=" in line and not line.startswith("#"):
                        k, v = line.strip().split("=", 1)
                        os.environ[k.strip()] = v.strip().strip("'\"")


def link_to_epic(jira_url: str, headers: dict, issue_key: str, epic_key: str) -> bool:
    """이슈를 에픽에 연결. parent 필드 또는 Epic Link custom field 사용."""
    # 1) parent 필드
    try:
        r = requests.put(
            f"{jira_url}/rest/api/3/issue/{issue_key}",
            headers=headers,
            json={"fields": {"parent": {"key": epic_key}}},
            timeout=10,
        )
        if r.status_code == 204:
            return True
    except Exception:
        pass
    # 2) Epic Link custom field
    for field_id in EPIC_LINK_FIELD_IDS:
        try:
            r = requests.put(
                f"{jira_url}/rest/api/3/issue/{issue_key}",
                headers=headers,
                json={"fields": {field_id: epic_key}},
                timeout=10,
            )
            if r.status_code == 204:
                return True
        except Exception:
            continue
    return False


def main() -> None:
    parser = argparse.ArgumentParser(description="Epic 매핑 일괄 수정")
    parser.add_argument("--dry-run", action="store_true", help="실제 수정 없이 대상만 출력")
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""

    if not all([jira_url, jira_email, jira_token]):
        print("오류: JIRA 인증 정보 필요", file=sys.stderr)
        sys.exit(1)

    if not MAPPING_FILE.exists():
        print(
            f"오류: {MAPPING_FILE} 없음. 먼저 jira-build-full-mapping.py 실행",
            file=sys.stderr,
        )
        sys.exit(1)

    with open(MAPPING_FILE, "r", encoding="utf-8") as f:
        mapping = json.load(f)

    # Epic 수정 필요한 이슈만 필터
    to_fix = [
        (k, v)
        for k, v in mapping.items()
        if v.get("needs_epic_fix") and v.get("epic_correct")
    ]

    print(f"Epic 매핑 수정 대상: {len(to_fix)}개")
    for jira_key, info in to_fix[:20]:
        current = info["epic_current"] or "없음"
        correct = info["epic_correct"]
        backlog = info.get("backlog_key", "-")
        print(f"  {jira_key} ({backlog}): {current} → {correct}")

    if args.dry_run:
        print("\n[DRY RUN] 실제 수정 없이 종료")
        return

    auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
    }

    success, fail = 0, 0
    for jira_key, info in to_fix:
        epic_key = info["epic_correct"]
        if link_to_epic(jira_url, headers, jira_key, epic_key):
            print(f"  ✓ {jira_key} → Epic {epic_key}")
            success += 1
        else:
            print(f"  ✗ {jira_key} Epic 연결 실패")
            fail += 1
        time.sleep(0.3)

    print(f"\n완료: 성공 {success}개, 실패 {fail}개")


if __name__ == "__main__":
    main()
