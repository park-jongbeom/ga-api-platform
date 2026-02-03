#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA_BACKLOG.md 파싱: Epic → Story → Tasks 구조에서
작업 키 → 에픽 키 매핑 및 Story별 구역 정보 생성.
"""
import json
import re
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
BACKLOG_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG.md"
OUTPUT_JSON = PROJECT_ROOT / ".github" / "jira-task-to-epic-mapping.json"


def parse_backlog() -> tuple[dict, list]:
    """
    백로그 파싱.
    Returns:
        task_to_epic: { task_key: epic_key }
        sections: [ { "epic_key", "story_key", "story_title", "task_keys" }, ... ]
    """
    if not BACKLOG_FILE.exists():
        return {}, []

    with open(BACKLOG_FILE, "r", encoding="utf-8") as f:
        lines = f.readlines()

    task_to_epic: dict = {}
    sections: list = []
    current_epic_key: str | None = None
    current_story_key: str | None = None
    current_story_title: str | None = None
    in_tasks = False
    task_line_re = re.compile(r"^-\s*\[\s*[x ]\s*\]\s+(GAM-\d+(?:-\d+)?)\s*:\s*(.+)$")
    epic_id_re = re.compile(r"^\*\*Epic ID\*\*:\s*(GAM-\d+)\s*$")
    story_re = re.compile(r"^### Story (GAM-\d+):\s*(.+)$")

    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # ## Epic N: ... → 다음 **Epic ID**: GAM-X 까지 탐색
        if stripped.startswith("## Epic ") and ":" in stripped:
            current_epic_key = None
            current_story_key = None
            in_tasks = False
            j = i + 1
            while j < len(lines) and j < i + 20:
                m = epic_id_re.match(lines[j].strip())
                if m:
                    current_epic_key = m.group(1)
                    break
                if lines[j].strip().startswith("### Story "):
                    break
                j += 1
            i += 1
            continue

        # ### Story GAM-XX: Title
        if stripped.startswith("### Story "):
            m = story_re.match(stripped)
            in_tasks = False
            if m and current_epic_key:
                current_story_key = m.group(1)
                current_story_title = m.group(2).strip()
                sections.append({
                    "epic_key": current_epic_key,
                    "story_key": current_story_key,
                    "story_title": current_story_title,
                    "task_keys": [],
                })
            i += 1
            continue

        # **Tasks**:
        if stripped == "**Tasks**:" or (stripped.startswith("**Tasks**") and stripped.endswith(":")):
            in_tasks = True
            i += 1
            continue

        # - [ ] GAM-YY: description
        if in_tasks and sections:
            tm = task_line_re.match(stripped)
            if tm:
                task_key, _ = tm.group(1), tm.group(2)
                task_to_epic[task_key] = current_epic_key
                sections[-1]["task_keys"].append(task_key)
                i += 1
                continue
            # 비태스크 줄(빈 줄, ** 로 시작 등) → Tasks 구간 종료
            if stripped.startswith("**") or stripped.startswith("###") or stripped.startswith("##"):
                in_tasks = False

        i += 1

    return task_to_epic, sections


def main() -> None:
    task_to_epic, sections = parse_backlog()
    payload = {
        "task_to_epic": task_to_epic,
        "sections": sections,
        "task_count": len(task_to_epic),
        "section_count": len(sections),
    }
    OUTPUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)
    print(f"작업 → 에픽 매핑: {len(task_to_epic)}개")
    print(f"구역(Story) 수: {len(sections)}개")
    print(f"저장: {OUTPUT_JSON}")


if __name__ == "__main__":
    main()
