#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA_BACKLOG.md 변환: ### Story GAM-XX: 제목 → ### [제목] (관련 작업: GAM-aa, ...)
Story 메타(Description, AC 등) 제거, **Tasks**: 및 체크리스트만 유지.
"""
import json
import re
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
BACKLOG_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG.md"
MAPPING_FILE = PROJECT_ROOT / ".github" / "jira-task-to-epic-mapping.json"
TASK_LINE_RE = re.compile(r"^-\s*\[\s*[x ]\s*\]\s+(GAM-\d+(?:-\d+)?)\s*:.+$")


def main() -> None:
    if not MAPPING_FILE.exists():
        raise SystemExit(f"먼저 jira-build-task-epic-mapping.py 실행: {MAPPING_FILE} 없음")
    with open(MAPPING_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)
    sections = data.get("sections", [])

    with open(BACKLOG_FILE, "r", encoding="utf-8") as f:
        lines = f.readlines()

    out: list[str] = []
    i = 0
    section_index = 0

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        # ### Story GAM-XX: Title (순서대로 section 매칭)
        if stripped.startswith("### Story ") and section_index < len(sections):
            m = re.match(r"^### Story (GAM-\d+):\s*(.+)$", stripped)
            if m:
                sec = sections[section_index]
                story_title = sec["story_title"]
                task_keys = sec.get("task_keys", [])
                related = ", ".join(task_keys) if task_keys else "-"
                out.append(f"### {story_title} (관련 작업: {related})\n")
                out.append("\n")
                i += 1
                # Skip until **Tasks**:
                while i < len(lines) and "**Tasks**" not in lines[i]:
                    i += 1
                if i < len(lines):
                    out.append(lines[i])  # **Tasks**:
                    i += 1
                # Collect task lines until next ### or ##
                while i < len(lines):
                    if TASK_LINE_RE.match(lines[i].strip()):
                        out.append(lines[i])
                        i += 1
                    elif lines[i].strip().startswith("###") or lines[i].strip().startswith("##"):
                        out.append("\n")
                        break
                    else:
                        i += 1
                section_index += 1
                continue
        out.append(line)
        i += 1
    with open(BACKLOG_FILE, "w", encoding="utf-8") as f:
        f.writelines(out)
    print(f"변환 완료: {BACKLOG_FILE}")


if __name__ == "__main__":
    main()
