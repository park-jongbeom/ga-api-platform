#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA summary가 백로그 키(GAM-XX)로 되어 있는 이슈를
실제 작업 제목으로 수정.
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


def load_jira_env(paths: list) -> None:
    for p in paths:
        path = PROJECT_ROOT / p if not os.path.isabs(p) else Path(p)
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    if "=" in line and not line.startswith("#"):
                        k, v = line.strip().split("=", 1)
                        os.environ[k.strip()] = v.strip().strip("'\"")


def update_issue_summary(jira_url: str, headers: dict, issue_key: str, new_summary: str) -> bool:
    """이슈의 summary 필드를 업데이트."""
    try:
        r = requests.put(
            f"{jira_url}/rest/api/3/issue/{issue_key}",
            headers=headers,
            json={"fields": {"summary": new_summary}},
            timeout=10,
        )
        return r.status_code == 204
    except Exception as e:
        print(f"  오류: {e}")
        return False


def main() -> None:
    parser = argparse.ArgumentParser(description="JIRA summary 일괄 수정")
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

    # summary 수정 필요한 이슈만 필터
    to_fix = [
        (k, v)
        for k, v in mapping.items()
        if v.get("needs_summary_fix") and v.get("summary_correct")
    ]

    print(f"Summary 수정 대상: {len(to_fix)}개")
    for jira_key, info in to_fix:
        current = info["summary_current"]
        correct = info["summary_correct"]
        print(f"  {jira_key}: {current} → {correct[:60]}")

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
        new_summary = info["summary_correct"]
        if update_issue_summary(jira_url, headers, jira_key, new_summary):
            print(f"  ✓ {jira_key} summary 업데이트")
            success += 1
        else:
            print(f"  ✗ {jira_key} 업데이트 실패")
            fail += 1
        time.sleep(0.3)

    print(f"\n완료: 성공 {success}개, 실패 {fail}개")


if __name__ == "__main__":
    main()
