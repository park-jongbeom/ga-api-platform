#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
매핑 파일 없이 JIRA 백엔드 이슈만 조회해 연결 구조로 Task 재배치.

- jira-backend-issues.json(또는 JIRA API)에서 이슈 목록 로드
- Task인 이슈만 대상: 현재 parent가 Story면, 그 Story의 parent(Epic)로 재배치
- 이미 parent가 Epic이면 스킵. KEY 매핑 테이블 불필요.
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
JIRA_ISSUES_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"

# 이슈 타입 이름 (한국어 JIRA)
TYPE_EPIC = "에픽"
TYPE_STORY = "스토리"
TYPE_TASK = "작업"


def load_jira_env(paths: list) -> None:
    for p in paths:
        path = PROJECT_ROOT / p if p and not os.path.isabs(p) else Path(p or "")
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith("#") or "=" not in line:
                        continue
                    k, _, v = line.partition("=")
                    if k.strip():
                        os.environ[k.strip()] = v.strip().strip("'\"")


def set_parent(jira_url: str, headers: dict, issue_key: str, parent_key: str) -> bool:
    try:
        r = requests.put(
            f"{jira_url}/rest/api/3/issue/{issue_key}",
            headers=headers,
            json={"fields": {"parent": {"key": parent_key}}},
            timeout=10,
        )
        return r.status_code == 204
    except Exception as e:
        print(f"  오류 {issue_key}: {e}")
        return False


def main() -> None:
    parser = argparse.ArgumentParser(
        description="JIRA 연결 구조만으로 Task의 parent를 스토리 → 에픽으로 변경 (매핑 파일 불필요)"
    )
    parser.add_argument("--dry-run", action="store_true", help="API 호출 없이 대상만 출력")
    parser.add_argument(
        "--fetch",
        action="store_true",
        help="jira-backend-issues.json 대신 JIRA API로 백엔드 이슈 조회 (구현 시 JQL 사용)",
    )
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    if not JIRA_ISSUES_FILE.exists() and not args.fetch:
        print(
            f"오류: {JIRA_ISSUES_FILE} 없음. 파일을 생성하거나 --fetch 사용",
            file=sys.stderr,
        )
        sys.exit(1)

    if args.fetch:
        print("오류: --fetch는 아직 미구현. jira-backend-issues.json을 사용하세요.", file=sys.stderr)
        sys.exit(1)

    with open(JIRA_ISSUES_FILE, "r", encoding="utf-8") as f:
        raw = json.load(f)
    issues = raw if isinstance(raw, list) else list(raw.values()) if isinstance(raw, dict) else []
    issues_by_key = {i["key"]: i for i in issues}

    # Task 중 parent가 Story인 것만: Story의 parent(Epic)로 재배치 대상
    to_reparent = []
    for issue in issues:
        if (issue.get("type") or "").strip() != TYPE_TASK:
            continue
        task_key = issue["key"]
        parent_key = issue.get("parent")
        if not parent_key:
            continue
        parent_issue = issues_by_key.get(parent_key)
        if not parent_issue:
            continue
        parent_type = (parent_issue.get("type") or "").strip()
        if parent_type == TYPE_EPIC:
            continue  # 이미 에픽 직속
        if parent_type != TYPE_STORY:
            continue
        grandparent_key = parent_issue.get("parent")
        if not grandparent_key:
            continue
        grandparent = issues_by_key.get(grandparent_key)
        if not grandparent or (grandparent.get("type") or "").strip() != TYPE_EPIC:
            continue
        to_reparent.append((task_key, grandparent_key))

    print(f"에픽 직속으로 재배치할 작업(구조 기반): {len(to_reparent)}개")
    for task_key, epic_key in to_reparent[:30]:
        print(f"  {task_key} → parent {epic_key}")
    if len(to_reparent) > 30:
        print(f"  ... 외 {len(to_reparent) - 30}개")

    if args.dry_run:
        print("\n[DRY RUN] API 호출 없이 종료")
        return

    if not to_reparent:
        print("\n재배치할 작업 없음. 종료.")
        return

    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""
    if not all([jira_url, jira_email, jira_token]):
        print("오류: JIRA 인증 정보 필요 (JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN)", file=sys.stderr)
        sys.exit(1)

    auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    success, fail = 0, 0
    for task_key, epic_key in to_reparent:
        if set_parent(jira_url, headers, task_key, epic_key):
            print(f"  ✓ {task_key} → Epic {epic_key}")
            success += 1
        else:
            print(f"  ✗ {task_key} 실패")
            fail += 1
        time.sleep(0.35)
    print(f"\n완료: 성공 {success}개, 실패 {fail}개")


if __name__ == "__main__":
    main()
