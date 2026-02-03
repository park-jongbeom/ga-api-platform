#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA_BACKLOG.md에 기록된 키를 실제 JIRA 이슈 키로 동기화.
매핑 테이블(jira-to-backlog-mapping.json) 기준으로 백로그 키를 JIRA 키로 치환.
"""
import json
import re
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
MAPPING_FILE = PROJECT_ROOT / ".github" / "jira-to-backlog-mapping.json"
BACKLOG_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG.md"


def main() -> None:
    if not MAPPING_FILE.exists():
        print(f"오류: {MAPPING_FILE} 없음", file=sys.stderr)
        sys.exit(1)
    if not BACKLOG_FILE.exists():
        print(f"오류: {BACKLOG_FILE} 없음", file=sys.stderr)
        sys.exit(1)

    with open(MAPPING_FILE, "r", encoding="utf-8") as f:
        mapping = json.load(f)

    # 백로그 키 → JIRA 키 (불일치만)
    backlog_to_jira = {}
    for jira_key, info in mapping.items():
        backlog_key = info.get("backlog_key")
        if backlog_key and backlog_key != jira_key:
            backlog_to_jira[backlog_key] = jira_key

    # 긴 키 먼저 치환 (GAM-11-1 → GAM-27, GAM-22-1 → ... 충돌 방지)
    sorted_backlog_keys = sorted(backlog_to_jira.keys(), key=lambda k: (-len(k), k))
    # 어떤 키가 다른 키의 접두어인지 (GAM-22는 GAM-22-1의 접두어)
    prefix_keys = {k for k in backlog_to_jira if any(
        o != k and o.startswith(k + "-") for o in backlog_to_jira
    )}

    content = BACKLOG_FILE.read_text(encoding="utf-8")

    # 1단계: 백로그 키 → 임시 플레이스홀더
    for backlog_key in sorted_backlog_keys:
        jira_key = backlog_to_jira[backlog_key]
        temp_key = f"__TEMP_{jira_key}__"
        if backlog_key in prefix_keys:
            # GAM-22가 GAM-22-1 안에 매칭되지 않도록: \bGAM-22(?!-\d)\b
            pattern = rf"\b{re.escape(backlog_key)}(?!-\d)\b"
        else:
            pattern = rf"\b{re.escape(backlog_key)}\b"
        content = re.sub(pattern, temp_key, content)

    # 2단계: 임시 플레이스홀더 → JIRA 키
    for backlog_key, jira_key in backlog_to_jira.items():
        temp_key = f"__TEMP_{jira_key}__"
        content = content.replace(temp_key, jira_key)

    BACKLOG_FILE.write_text(content, encoding="utf-8")
    print(f"백로그 키 동기화 완료: {len(backlog_to_jira)}개 키 치환")
    print(f"  파일: {BACKLOG_FILE}")


if __name__ == "__main__":
    main()
