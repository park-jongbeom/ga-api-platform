#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
백로그 문서의 키를 JIRA 실제 키로 전면 변경.
.github/backlog-to-jira-mapping.json 매핑 사용.
"""
import json
import re
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
MAPPING_FILE = PROJECT_ROOT / ".github" / "backlog-to-jira-mapping.json"
BACKLOG_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG.md"


def main():
    if not MAPPING_FILE.exists():
        print(f"오류: 매핑 파일 없음 — {MAPPING_FILE}", file=sys.stderr)
        return 1
    with open(MAPPING_FILE, "r", encoding="utf-8") as f:
        mapping = json.load(f)
    if not mapping:
        print("매핑이 비어 있어 변경 없음.")
        return 0

    if not BACKLOG_FILE.exists():
        print(f"오류: 백로그 파일 없음 — {BACKLOG_FILE}", file=sys.stderr)
        return 1
    with open(BACKLOG_FILE, "r", encoding="utf-8") as f:
        content = f.read()

    # 긴 키부터 치환 (GAM-22-3 → GAM-51 먼저). 하위 키(GAM-11-1 등)는 치환 제외.
    sorted_keys = sorted(
        mapping.keys(),
        key=lambda x: (-x.count("-"), x),
        reverse=True,
    )
    for backlog_key in sorted_keys:
        jira_key = mapping[backlog_key]
        # 키 뒤에 '-'+숫자가 오면 하위 작업이므로 치환하지 않음 (예: GAM-11-1)
        pattern = rf"\b{re.escape(backlog_key)}\b(?!-\d)"
        content = re.sub(pattern, jira_key, content)

    old_comment = (
        "**키와 JIRA 실제 이슈 키**: 백로그에 적힌 키(GAM-11, GAM-12 등)는 **문서용 식별자**이며, "
        "JIRA에 등록된 실제 이슈 키(GAM-7, GAM-8 등)와 다를 수 있습니다. "
        "동일 여부는 **이름(제목) 및 내용**으로 판단하며, 필요 시 `.github/jira-mapping.json`의 "
        "`_jiraToBacklog`로 JIRA 실제 키 → 백로그 키 매핑을 명시합니다."
    )
    new_comment = (
        "**키 체계**: 백로그 문서의 키(GAM-7, GAM-51 등)는 **JIRA 실제 이슈 키**와 동일합니다. "
        "매핑 파일 없이 직접 참조 가능합니다."
    )
    if old_comment in content:
        content = content.replace(old_comment, new_comment)
    else:
        # 부분 일치로 헤더 문단만 교체 시도
        content = re.sub(
            r"\*\*키와 JIRA 실제 이슈 키\*\*:.*?명시합니다\.",
            new_comment,
            content,
            count=1,
            flags=re.DOTALL,
        )

    with open(BACKLOG_FILE, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"백로그 문서 변환 완료: {len(mapping)}개 키 치환")
    return 0


if __name__ == "__main__":
    sys.exit(main())
