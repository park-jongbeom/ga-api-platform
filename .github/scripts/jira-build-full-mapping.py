#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 백엔드 이슈(GAM-1~71)와 백로그 Story/Task를 비교해서
완전 매핑 테이블 생성. summary 오류, Epic 오류 분류.
"""
import json
import re
from pathlib import Path
from typing import Dict, List, Tuple

PROJECT_ROOT = Path(__file__).resolve().parents[2]
JIRA_ISSUES_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
BACKLOG_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG.md"
CODE_VERIFICATION_FILE = PROJECT_ROOT / ".github" / "code-completion-verification.json"
OUTPUT_MAPPING = PROJECT_ROOT / ".github" / "jira-to-backlog-mapping.json"
OUTPUT_REPORT = PROJECT_ROOT / "reports" / "jira-mapping-analysis.md"

# Epic 구조 (백로그 기준)
BACKLOG_EPIC_STRUCTURE = {
    "GAM-1": ["GAM-7", "GAM-8", "GAM-9"],
    "GAM-2": ["GAM-10", "GAM-20", "GAM-21", "GAM-22", "GAM-23"],
    "GAM-3": ["GAM-31", "GAM-32", "GAM-33"],
    "GAM-4": ["GAM-41", "GAM-42", "GAM-43", "GAM-44"],
    "GAM-5": ["GAM-51", "GAM-52", "GAM-53", "GAM-54", "GAM-55"],
    "GAM-6": ["GAM-61", "GAM-62", "GAM-63"],
}


def parse_backlog_all() -> Tuple[Dict[str, Dict], Dict[str, Dict]]:
    """백로그에서 Story와 Task 모두 파싱. 키로 접근 가능한 dict 반환."""
    if not BACKLOG_FILE.exists():
        return {}, {}
    with open(BACKLOG_FILE, "r", encoding="utf-8") as f:
        content = f.read()

    # Story 파싱
    stories: Dict[str, Dict] = {}
    for m in re.finditer(r"### Story (GAM-\d+):\s*(.+)", content):
        key, title = m.group(1), m.group(2).strip()
        stories[key] = {"key": key, "title": title, "type": "Story"}

    # Task 파싱 (모든 체크리스트)
    tasks: Dict[str, Dict] = {}
    for m in re.finditer(r"-\s*\[\s*\]\s+(GAM-\d+(?:-\d+)?)\s*:\s*([^\n]+)", content):
        key, description = m.group(1), m.group(2).strip()
        tasks[key] = {"key": key, "description": description, "type": "Task"}

    return stories, tasks


def find_correct_epic_for_story(story_key: str) -> str | None:
    """백로그 Story 키로 올바른 Epic 찾기."""
    for epic, children in BACKLOG_EPIC_STRUCTURE.items():
        if story_key in children:
            return epic
    return None


def find_correct_epic_for_task(task_key: str, stories: Dict) -> str | None:
    """Task 키로 올바른 Epic 찾기. Task는 Story 하위이므로 Story의 Epic 사용."""
    parts = task_key.split("-")
    if len(parts) >= 3:  # GAM-22-4 형식
        parent_story = f"GAM-{parts[1]}"
        if parent_story in stories:
            return find_correct_epic_for_story(parent_story)
    return None


def main() -> None:
    if not JIRA_ISSUES_FILE.exists():
        print(f"오류: {JIRA_ISSUES_FILE} 없음")
        return

    with open(JIRA_ISSUES_FILE, "r", encoding="utf-8") as f:
        jira_issues = json.load(f)

    stories, tasks = parse_backlog_all()
    all_backlog_items = {**stories, **tasks}

    with open(CODE_VERIFICATION_FILE, "r", encoding="utf-8") as f:
        code_verification = json.load(f)
    code_completed_keys = {t["key"] for t in code_verification.get("tasks_completed", [])}

    mapping: Dict[str, Dict] = {}
    summary_fixes = []
    epic_fixes = []
    completion_needed = []

    for issue in jira_issues:
        jira_key = issue["key"]
        jira_summary = issue["summary"]
        jira_epic = issue.get("parent")
        jira_status = issue.get("status", "")
        jira_type = issue.get("type", "")

        entry = {
            "jira_key": jira_key,
            "summary_current": jira_summary,
            "summary_correct": None,
            "backlog_key": None,
            "backlog_title": None,
            "epic_current": jira_epic,
            "epic_correct": None,
            "status_current": jira_status,
            "code_completed": False,
            "needs_summary_fix": False,
            "needs_epic_fix": False,
            "needs_completion": False,
        }

        # summary가 "GAM-XX" 형식이면 → 백로그 키를 summary에 잘못 넣은 것
        summary_key_match = re.match(r"^(GAM-\d+(?:-\d+)?)$", jira_summary.strip())
        if summary_key_match:
            backlog_key = summary_key_match.group(1)
            if backlog_key in all_backlog_items:
                item = all_backlog_items[backlog_key]
                entry["backlog_key"] = backlog_key
                entry["summary_correct"] = item.get("title") or item.get("description")
                entry["backlog_title"] = entry["summary_correct"]
                entry["needs_summary_fix"] = True
                summary_fixes.append(jira_key)
        else:
            # summary가 정상 제목인 경우 → 작업 내용으로 백로그 키 추론
            for bl_key, bl_item in all_backlog_items.items():
                bl_text = bl_item.get("title") or bl_item.get("description", "")
                if bl_text and (bl_text in jira_summary or jira_summary in bl_text):
                    entry["backlog_key"] = bl_key
                    entry["backlog_title"] = bl_text
                    entry["summary_correct"] = jira_summary
                    break

        # 백로그 키를 찾았으면 올바른 Epic 계산
        if entry["backlog_key"]:
            bl_key = entry["backlog_key"]
            if bl_key in stories:
                entry["epic_correct"] = find_correct_epic_for_story(bl_key)
            elif bl_key in tasks:
                entry["epic_correct"] = find_correct_epic_for_task(bl_key, stories)

        # Epic 오류 확인
        if entry["epic_correct"] and entry["epic_current"] != entry["epic_correct"]:
            entry["needs_epic_fix"] = True
            epic_fixes.append(jira_key)

        # 완료 처리 필요 여부
        if entry["backlog_key"] in code_completed_keys and jira_status == "해야 할 일":
            entry["needs_completion"] = True
            entry["code_completed"] = True
            completion_needed.append(jira_key)

        mapping[jira_key] = entry

    # 매핑 파일 저장
    OUTPUT_MAPPING.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_MAPPING, "w", encoding="utf-8") as f:
        json.dump(mapping, f, indent=2, ensure_ascii=False)

    # 분석 보고서 생성
    OUTPUT_REPORT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_REPORT, "w", encoding="utf-8") as f:
        f.write("# JIRA-백로그 매핑 분석 보고서\n\n")
        f.write(f"총 JIRA 이슈: {len(jira_issues)}개\n\n")
        f.write("---\n\n")
        f.write(f"## 1. Summary 수정 필요 ({len(summary_fixes)}개)\n\n")
        f.write("JIRA summary가 백로그 키(GAM-XX)로 되어 있는 경우:\n\n")
        f.write("| JIRA 키 | 현재 Summary | 올바른 Summary |\n")
        f.write("|---------|--------------|-----------------|\n")
        for jk in summary_fixes[:20]:
            e = mapping[jk]
            f.write(f"| {jk} | {e['summary_current']} | {e['summary_correct'][:50] if e['summary_correct'] else '-'} |\n")
        f.write(f"\n**총 {len(summary_fixes)}개**\n\n")

        f.write(f"## 2. Epic 매핑 수정 필요 ({len(epic_fixes)}개)\n\n")
        f.write("| JIRA 키 | 백로그 키 | 현재 Epic | 올바른 Epic | 작업 |\n")
        f.write("|---------|----------|-----------|-------------|------|\n")
        for jk in epic_fixes[:30]:
            e = mapping[jk]
            bl = e.get('backlog_key', '-')
            title = e.get('backlog_title', '')[:40] if e.get('backlog_title') else '-'
            f.write(f"| {jk} | {bl} | {e['epic_current'] or '-'} | {e['epic_correct'] or '-'} | {title} |\n")
        f.write(f"\n**총 {len(epic_fixes)}개**\n\n")

        f.write(f"## 3. 완료 처리 필요 ({len(completion_needed)}개)\n\n")
        f.write("코드 완료인데 JIRA 상태가 '해야 할 일':\n\n")
        f.write("| JIRA 키 | 백로그 키 | 작업 |\n")
        f.write("|---------|-----------|---------|\n")
        for jk in completion_needed[:20]:
            e = mapping[jk]
            bl = e.get('backlog_key', '-')
            title = e.get('backlog_title', '')[:50] if e.get('backlog_title') else '-'
            f.write(f"| {jk} | {bl} | {title} |\n")
        f.write(f"\n**총 {len(completion_needed)}개**\n\n")

    print("매핑 분석 완료:")
    print(f"  Summary 수정 필요: {len(summary_fixes)}개")
    print(f"  Epic 매핑 수정 필요: {len(epic_fixes)}개")
    print(f"  완료 처리 필요: {len(completion_needed)}개")
    print(f"\n결과 저장:")
    print(f"  매핑: {OUTPUT_MAPPING}")
    print(f"  보고서: {OUTPUT_REPORT}")


if __name__ == "__main__":
    main()
