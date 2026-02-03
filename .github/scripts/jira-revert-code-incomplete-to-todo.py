#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
코드 검증 결과에서 미구현(implemented=false) Task를 JIRA에서 '해야 할 일'로 전환.
엄격 검증(strict-code-completion-verification.json)이 있으면 우선 사용.
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
STRICT_VERIFICATION_FILE = PROJECT_ROOT / ".github" / "strict-code-completion-verification.json"
VERIFICATION_FILE = PROJECT_ROOT / ".github" / "code-completion-verification.json"
REPORT_FILE = PROJECT_ROOT / "reports" / "jira-revert-code-incomplete-report.md"

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
        description="코드 미구현 Task를 JIRA에서 '해야 할 일'로 되돌림"
    )
    parser.add_argument("--dry-run", action="store_true", help="전환 없이 대상만 출력")
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    verification_file = STRICT_VERIFICATION_FILE if STRICT_VERIFICATION_FILE.exists() else VERIFICATION_FILE
    if not verification_file.exists():
        print(f"오류: 검증 결과 없음. 먼저 jira-verify-code-strict.py 또는 jira-verify-code-completion.py 실행", file=sys.stderr)
        sys.exit(1)
    print(f"사용 검증 파일: {verification_file.name}")

    with open(verification_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    candidates = [r for r in data.get("results", []) if r.get("implemented") is False]
    if not candidates:
        print("되돌릴 미구현 Task가 없습니다.")
        return

    print(f"코드 미구현 Task (완료 → To Do): {len(candidates)}개")
    for c in candidates[:30]:
        print(f"  {c['key']} ({c.get('parent')}): {(c.get('summary') or '')[:50]}")
    if len(candidates) > 30:
        print(f"  ... 외 {len(candidates) - 30}개")

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
        f.write("# 코드 미구현 Task To Do 되돌림 보고서\n\n")
        f.write(f"- **대상**: 코드 검증 결과 미구현 Task {len(candidates)}개\n")
        f.write(f"- **성공**: {success}개\n")
        f.write(f"- **실패**: {fail}개\n\n")
        f.write("## 되돌린 이슈\n\n")
        for c in candidates:
            f.write(f"- {c['key']} ({c.get('parent')}): {c.get('summary', '')}\n")
    print(f"보고서: {REPORT_FILE}")


if __name__ == "__main__":
    main()
