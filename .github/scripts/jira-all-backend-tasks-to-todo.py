#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GAM-1·GAM-2 제외, GAM-3·GAM-4·GAM-5·GAM-6 하위 **작업**만 '해야 할 일'로 전환.

- GAM-1, GAM-2 및 그 하위: 변경 없음
- GAM-3, GAM-4, GAM-5, GAM-6 하위 중 type=작업만 대상 (에픽 자체는 제외)
- 현재 상태와 무관하게 모두 To Do로 전환 시도
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
REPORT_FILE = PROJECT_ROOT / "reports" / "jira-all-backend-tasks-to-todo-report.md"

TARGET_EPICS = {"GAM-3", "GAM-4", "GAM-5", "GAM-6"}  # GAM-1, GAM-2 제외
TODO_NAMES = ("해야 할 일", "to do", "to_do", "open", "backlog")


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


def get_transitions(jira_url: str, headers: dict, issue_key: str) -> list:
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
            headers=headers,
            timeout=10,
        )
        return r.json().get("transitions", []) if r.status_code == 200 else []
    except Exception:
        return []


def find_todo_transition_id(transitions: list) -> str | None:
    for t in transitions:
        name = (t.get("name") or "").strip().lower()
        to_status = (t.get("to", {}) or {}).get("name") or ""
        to_status_lower = to_status.strip().lower()
        if any(todo in name or todo in to_status_lower for todo in TODO_NAMES):
            return t.get("id")
    return None


def transition_to_todo(jira_url: str, headers: dict, issue_key: str) -> bool:
    transitions = get_transitions(jira_url, headers, issue_key)
    tid = find_todo_transition_id(transitions)
    if not tid:
        return False
    r = requests.post(
        f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
        headers=headers,
        json={"transition": {"id": tid}},
        timeout=10,
    )
    return r.status_code == 204


def main() -> None:
    parser = argparse.ArgumentParser(
        description="GAM-1·GAM-2 제외, GAM-3~6 하위 작업만 '해야 할 일'로 전환"
    )
    parser.add_argument("--dry-run", action="store_true", help="API 호출 없이 대상만 출력")
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    if not JIRA_ISSUES_FILE.exists():
        print(f"오류: {JIRA_ISSUES_FILE} 없음", file=sys.stderr)
        sys.exit(1)

    with open(JIRA_ISSUES_FILE, "r", encoding="utf-8") as f:
        raw = json.load(f)
    issues = raw if isinstance(raw, list) else list(raw.values()) if isinstance(raw, dict) else []

    # type=작업, parent ∈ GAM-3, GAM-4, GAM-5, GAM-6 만 (GAM-1·GAM-2 하위 제외)
    to_transition = [
        i for i in issues
        if i.get("type") == "작업" and i.get("parent") in TARGET_EPICS
    ]

    print(f"GAM-1·GAM-2 제외, GAM-3~6 하위 작업: {len(to_transition)}개")
    for i in to_transition[:40]:
        print(f"  {i['key']} ({i.get('parent','')}) | {i.get('summary','')[:45]} | {i.get('status','')}")
    if len(to_transition) > 40:
        print(f"  ... 외 {len(to_transition) - 40}개")

    if args.dry_run:
        print("\n[DRY RUN] API 호출 없이 종료")
        REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
        with open(REPORT_FILE, "w", encoding="utf-8") as f:
            f.write("# GAM-3~6 하위 작업 → 해야 할 일 전환 대상 (Dry Run)\n\n")
            f.write(f"대상: {len(to_transition)}개 (GAM-1·GAM-2 제외)\n\n")
            f.write("| 키 | 에픽 | 요약 | 현재 상태 |\n")
            f.write("|----|------|------|----------|\n")
            for i in to_transition:
                f.write(f"| {i['key']} | {i.get('parent','')} | {(i.get('summary') or '')[:50]} | {i.get('status','')} |\n")
        print(f"보고서: {REPORT_FILE}")
        return

    if not to_transition:
        print("전환할 작업 없음. 종료.")
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
    for i in to_transition:
        key = i["key"]
        if transition_to_todo(jira_url, headers, key):
            print(f"  ✓ {key} → 해야 할 일")
            success += 1
        else:
            print(f"  ✗ {key} 전환 실패 (이미 To Do이거나 전환 불가)")
            fail += 1
        time.sleep(0.35)

    REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write("# GAM-3~6 하위 작업 → 해야 할 일 전환 결과\n\n")
        f.write(f"- 대상: {len(to_transition)}개 (GAM-1·GAM-2 제외)\n")
        f.write(f"- 성공: {success}개\n")
        f.write(f"- 실패: {fail}개\n")
    print(f"\n완료: 성공 {success}개, 실패 {fail}개")
    print(f"보고서: {REPORT_FILE}")


if __name__ == "__main__":
    main()
