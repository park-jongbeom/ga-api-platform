#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
2주차 에픽(GAM-2)과 그 하위만 완료로 두고, 그 외 JIRA에서 '완료'인 이슈를 '해야 할 일'로 되돌림.
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
CANDIDATES_FILE = PROJECT_ROOT / ".github" / "jira-revert-to-todo-candidates.json"
REPORT_FILE = PROJECT_ROOT / "reports" / "jira-revert-non-week2-summary.md"

WEEK2_EPIC_KEY = "GAM-2"
DONE_STATUSES = frozenset(
    {"완료", "done", "complete", "closed", "resolved", "해결됨", "종료"}
)
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


def is_done_status(status: str) -> bool:
    return (status or "").strip().lower() in {s.lower() for s in DONE_STATUSES}


def compute_week2_descendants(issues: list) -> set:
    """GAM-2와 그 하위(직·간접 자식) JIRA 키 집합."""
    key_to_parent = {i["key"]: i.get("parent") for i in issues}
    allowed = {WEEK2_EPIC_KEY}
    changed = True
    while changed:
        changed = False
        for key, parent in key_to_parent.items():
            if key in allowed:
                continue
            if parent in allowed:
                allowed.add(key)
                changed = True
    return allowed


def get_revert_candidates(issues: list, allowed_done: set) -> list:
    """완료 상태이면서 allowed_done에 없는 이슈 목록."""
    return [
        i
        for i in issues
        if is_done_status(i.get("status") or "")
        and i["key"] not in allowed_done
    ]


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
        description="2주차(GAM-2) 외 완료 이슈를 JIRA에서 '해야 할 일'로 되돌림"
    )
    parser.add_argument("--dry-run", action="store_true", help="전환 없이 대상만 출력")
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    if not JIRA_ISSUES_FILE.exists():
        print(f"오류: {JIRA_ISSUES_FILE} 없음", file=sys.stderr)
        sys.exit(1)

    with open(JIRA_ISSUES_FILE, "r", encoding="utf-8") as f:
        issues = json.load(f)
    if not isinstance(issues, list):
        issues = list(issues.values()) if isinstance(issues, dict) else []

    allowed_done = compute_week2_descendants(issues)
    candidates = get_revert_candidates(issues, allowed_done)

    payload = {
        "week2_epic": WEEK2_EPIC_KEY,
        "allowed_done_count": len(allowed_done),
        "allowed_done_keys": sorted(allowed_done),
        "revert_candidates_count": len(candidates),
        "revert_candidates": [
            {"key": i["key"], "summary": i.get("summary"), "type": i.get("type"), "status": i.get("status")}
            for i in candidates
        ],
    }
    CANDIDATES_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(CANDIDATES_FILE, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)

    print(f"2주차(GAM-2) 하위: {len(allowed_done)}개 (완료 유지)")
    print(f"되돌릴 대상(완료 → To Do): {len(candidates)}개")
    for c in candidates[:25]:
        print(f"  {c['key']} ({c.get('type')}): {(c.get('summary') or '')[:50]}")
    if len(candidates) > 25:
        print(f"  ... 외 {len(candidates) - 25}개")
    print(f"\n후보 저장: {CANDIDATES_FILE}")

    if args.dry_run:
        print("\n[DRY RUN] 실제 전환 없이 종료")
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
    for c in candidates:
        key = c["key"]
        if transition_to_todo(jira_url, headers, key):
            print(f"  ✓ {key} → To Do")
            success += 1
        else:
            print(f"  ✗ {key} 전환 실패")
            fail += 1
        time.sleep(0.35)

    print(f"\n완료: 성공 {success}개, 실패 {fail}개")

    REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write("# 2주차 외 완료 이슈 To Do 되돌림 요약\n\n")
        f.write(f"- **2주차(GAM-2) 하위 유지**: {len(allowed_done)}개\n")
        f.write(f"- **To Do로 되돌림**: {success}개 성공, {fail}개 실패\n\n")
        f.write("## 되돌린 이슈 (요약)\n\n")
        for c in candidates[:50]:
            f.write(f"- {c['key']}: {(c.get('summary') or '')[:60]}\n")
        if len(candidates) > 50:
            f.write(f"- ... 외 {len(candidates) - 50}개\n")
    print(f"보고서: {REPORT_FILE}")


if __name__ == "__main__":
    main()
