#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 실제 상태 또는 코드 검증 결과에 맞춰 docs/jira/JIRA_BACKLOG.md 체크박스 동기화.

--by-code: 실제 코드 검증(strict-all-tasks-verification.json) 기준으로 [x]/[ ] 설정.
미사용: JIRA JSON + revert 대상 기준으로 "완료" 표시.
"""
import argparse
import json
import re
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
STRICT_VERIFICATION = PROJECT_ROOT / ".github" / "strict-code-completion-verification.json"
STRICT_ALL_TASKS = PROJECT_ROOT / ".github" / "strict-all-tasks-verification.json"
JIRA_ISSUES = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
BACKLOG_MD = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG.md"
TARGET_EPICS_TODO = {"GAM-3", "GAM-4", "GAM-5", "GAM-6"}  # 이 에픽 하위는 전부 To Do로 전환됨


def main():
    parser = argparse.ArgumentParser(description="JIRA_BACKLOG.md 체크박스 동기화")
    parser.add_argument("--by-code", action="store_true", help="실제 코드 검증 결과 기준으로 완료/미완료 표시")
    args = parser.parse_args()

    completed_keys = set()

    if args.by_code:
        # 실제 코드 검증 결과: implemented=true 인 키만 [x]
        path = STRICT_ALL_TASKS if STRICT_ALL_TASKS.exists() else STRICT_VERIFICATION
        if not path.exists():
            print(f"오류: {path} 없음. 먼저 python3 .github/scripts/jira-verify-code-strict.py --all-tasks 실행", file=__import__("sys").stderr)
            return 1
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        completed_keys = {r["key"] for r in data.get("results", []) if r.get("implemented") is True}
        print(f"코드 검증 기준: 구현됨 {len(completed_keys)}개 → [x] 표시")
    else:
        # JIRA 상태 + revert 대상 기준
        reverted_42 = set()
        if STRICT_VERIFICATION.exists():
            with open(STRICT_VERIFICATION, "r", encoding="utf-8") as f:
                data = json.load(f)
            reverted_42 = {r["key"] for r in data.get("results", []) if r.get("implemented") is False}
        if JIRA_ISSUES.exists():
            with open(JIRA_ISSUES, "r", encoding="utf-8") as f:
                issues = json.load(f)
            for i in issues:
                if i.get("type") != "작업":
                    continue
                key = i.get("key")
                parent = i.get("parent")
                status = (i.get("status") or "").strip()
                if parent in TARGET_EPICS_TODO:
                    continue
                if parent in ("GAM-1", "GAM-2") and status == "완료" and key not in reverted_42:
                    completed_keys.add(key)

    # JIRA_BACKLOG.md에서 `- [ ] GAM-XX:` / `- [x] GAM-XX:` 라인만 체크 상태 갱신
    if not BACKLOG_MD.exists():
        print(f"오류: {BACKLOG_MD} 없음", file=__import__("sys").stderr)
        return 1

    text = BACKLOG_MD.read_text(encoding="utf-8")
    pattern = re.compile(r"^(\s*-\s)\[([ x])\]\s+(GAM-\d+):", re.MULTILINE)

    def replace(m):
        prefix, _old, key = m.group(1), m.group(2), m.group(3)
        new_check = "x" if key in completed_keys else " "
        return f"{prefix}[{new_check}] {key}:"

    new_text = pattern.sub(replace, text)
    if new_text != text:
        BACKLOG_MD.write_text(new_text, encoding="utf-8")
        print(f"동기화 완료: 완료로 표시된 작업 {len(completed_keys)}개")
        print(f"  완료 키: {sorted(completed_keys)}")
    else:
        print("변경 없음 (이미 동기화됨)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
