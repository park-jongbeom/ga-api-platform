#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GAM-1, GAM-2 외 에픽(GAM-3~GAM-6) 하위 이슈를 모두 '해야 할 일'로 전환.

- GAM-1, GAM-2 및 그 하위: 변경 없음
- GAM-3, GAM-4, GAM-5, GAM-6 및 그 직·간접 하위: 상태가 '해야 할 일'이 아니면 전환
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
REPORT_FILE = PROJECT_ROOT / "reports" / "jira-non-week1-2-to-todo-report.md"

KEEP_EPICS = {"GAM-1", "GAM-2"}  # 이 에픽과 그 하위는 건드리지 않음
OTHER_EPICS = {"GAM-3", "GAM-4", "GAM-5", "GAM-6"}
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


def is_todo_status(status: str) -> bool:
    if not status:
        return False
    s = status.strip().lower()
    return any(todo.lower() in s or s in todo.lower() for todo in TODO_NAMES)


def compute_descendants(issues: list, root_keys: set) -> set:
    """root_keys에 속한 이슈와 그 직·간접 하위(자식) JIRA 키 집합."""
    key_to_parent = {i["key"]: i.get("parent") for i in issues}
    result = set(root_keys)
    changed = True
    while changed:
        changed = False
        for key, parent in key_to_parent.items():
            if key in result:
                continue
            if parent in result:
                result.add(key)
                changed = True
    return result


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
        description="GAM-1/GAM-2 외 에픽 하위 이슈를 '해야 할 일'로 전환"
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

    keep_set = compute_descendants(issues, KEEP_EPICS)
    other_descendants = compute_descendants(issues, OTHER_EPICS)

    # GAM-1/GAM-2 하위가 아닌 것 중, GAM-3~6 하위만 대상 (이미 other_descendants가 GAM-3~6 및 그 하위)
    to_transition = [
        i
        for i in issues
        if i["key"] in other_descendants
        and i["key"] not in keep_set
        and not is_todo_status(i.get("status") or "")
    ]

    print(f"GAM-1/GAM-2 제외 에픽(GAM-3~6) 하위 이슈: {len(other_descendants)}개")
    print(f"그 중 '해야 할 일'로 전환할 이슈: {len(to_transition)}개")
    for i in to_transition[:30]:
        print(f"  {i['key']} | {i.get('type','')} | {i.get('summary','')[:40]} | {i.get('status','')}")
    if len(to_transition) > 30:
        print(f"  ... 외 {len(to_transition) - 30}개")

    if args.dry_run:
        print("\n[DRY RUN] API 호출 없이 종료")
        REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
        with open(REPORT_FILE, "w", encoding="utf-8") as f:
            f.write("# GAM-1/GAM-2 외 에픽 하위 → 해야 할 일 전환 대상 (Dry Run)\n\n")
            f.write(f"대상: {len(to_transition)}개\n\n")
            f.write("| 키 | 타입 | 요약 | 현재 상태 |\n")
            f.write("|----|------|------|----------|\n")
            for i in to_transition:
                f.write(f"| {i['key']} | {i.get('type','')} | {(i.get('summary') or '')[:50]} | {i.get('status','')} |\n")
        print(f"보고서: {REPORT_FILE}")
        return

    if not to_transition:
        print("전환할 이슈 없음. 종료.")
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
            print(f"  ✗ {key} 전환 실패")
            fail += 1
        time.sleep(0.35)

    REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write("# GAM-1/GAM-2 외 에픽 하위 → 해야 할 일 전환 결과\n\n")
        f.write(f"- 성공: {success}개\n")
        f.write(f"- 실패: {fail}개\n")
    print(f"\n완료: 성공 {success}개, 실패 {fail}개")
    print(f"보고서: {REPORT_FILE}")


if __name__ == "__main__":
    main()
