#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
작업(Task) 이슈의 parent를 스토리에서 에픽으로 변경.
jira-task-to-epic-mapping.json의 task_to_epic 기준으로 JIRA API 호출.
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
MAPPING_FILE = PROJECT_ROOT / ".github" / "jira-task-to-epic-mapping.json"
JIRA_ISSUES_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"


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
        description="작업 이슈의 parent를 스토리에서 에픽으로 변경"
    )
    parser.add_argument("--dry-run", action="store_true", help="API 호출 없이 대상만 출력")
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    if not MAPPING_FILE.exists():
        print(f"오류: {MAPPING_FILE} 없음. 먼저 jira-build-task-epic-mapping.py 실행", file=sys.stderr)
        sys.exit(1)

    with open(MAPPING_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)
    task_to_epic = data.get("task_to_epic", {})

    # JIRA에 실제로 존재하는 작업만 필터 (선택: jira-backend-issues.json에 있는 키만)
    issues_by_key = {}
    if JIRA_ISSUES_FILE.exists():
        with open(JIRA_ISSUES_FILE, "r", encoding="utf-8") as f:
            issues = json.load(f)
        if isinstance(issues, list):
            issues_by_key = {i["key"]: i for i in issues}
        elif isinstance(issues, dict):
            issues_by_key = issues

    # parent가 이미 에픽(GAM-1~6)이면 스킵
    epic_keys = {f"GAM-{i}" for i in range(1, 7)}
    to_reparent = []
    for task_key, epic_key in task_to_epic.items():
        if task_key in issues_by_key:
            current_parent = issues_by_key[task_key].get("parent")
            if current_parent in epic_keys:
                continue
        to_reparent.append((task_key, epic_key))

    print(f"에픽 직속으로 재배치할 작업: {len(to_reparent)}개")
    for task_key, epic_key in to_reparent[:25]:
        print(f"  {task_key} → parent {epic_key}")
    if len(to_reparent) > 25:
        print(f"  ... 외 {len(to_reparent) - 25}개")

    if args.dry_run:
        print("\n[DRY RUN] API 호출 없이 종료")
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
