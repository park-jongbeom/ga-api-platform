#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA_BACKLOG_ORIGIN.md 문서를 JIRA 실제 키로 업데이트
"""
import json
import re
from pathlib import Path
from collections import defaultdict

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DOC_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG_ORIGIN.md"
ISSUES_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
OUTPUT_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG_SYNCED.md"

def load_jira_issues():
    """JIRA 이슈 데이터 로드"""
    with open(ISSUES_FILE, "r", encoding="utf-8") as f:
        issues = json.load(f)
    
    # Epic별로 Task 분류
    epic_tasks = defaultdict(list)
    for issue in issues:
        if issue["type"] == "작업" and issue.get("parent"):
            parent = issue["parent"]
            if parent in [f"GAM-{i}" for i in range(1, 7)]:
                epic_tasks[parent].append({
                    "key": issue["key"],
                    "summary": issue["summary"]
                })
    
    # 키 순서로 정렬
    for epic in epic_tasks:
        epic_tasks[epic].sort(key=lambda x: int(x["key"].split("-")[1]))
    
    return dict(epic_tasks)


def update_document(epic_tasks):
    """문서를 JIRA 실제 키로 업데이트"""
    with open(DOC_FILE, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Epic 1 ~ 6 처리
    for epic_num in range(1, 7):
        epic_key = f"GAM-{epic_num}"
        tasks = epic_tasks.get(epic_key, [])
        
        if not tasks:
            print(f"⚠️  {epic_key}: Task 없음, 건너뜀")
            continue
        
        # Epic 섹션 찾기
        epic_pattern = rf"## Epic {epic_num}:.*?\n"
        epic_match = re.search(epic_pattern, content, re.IGNORECASE)
        
        if not epic_match:
            print(f"⚠️  {epic_key}: 문서에서 섹션을 찾을 수 없음")
            continue
        
        # 작업 목록 섹션 찾기
        task_section_start = content.find("### 작업 목록", epic_match.end())
        if task_section_start == -1:
            print(f"⚠️  {epic_key}: '작업 목록' 섹션을 찾을 수 없음")
            continue
        
        # 다음 Epic 또는 EOF까지
        next_epic_match = re.search(r"\n## Epic \d+:", content[epic_match.end():])
        if next_epic_match:
            section_end = epic_match.end() + next_epic_match.start()
        else:
            section_end = len(content)
        
        # 기존 작업 목록 찾기
        tasks_start = content.find("**Tasks**:", task_section_start)
        if tasks_start == -1 or tasks_start > section_end:
            print(f"⚠️  {epic_key}: '**Tasks**:' 를 찾을 수 없음")
            continue
        
        # 다음 섹션 시작점 찾기 (### 또는 ---)
        old_tasks_end = content.find("\n---", tasks_start)
        if old_tasks_end == -1 or old_tasks_end > section_end:
            old_tasks_end = content.find("\n##", tasks_start)
        if old_tasks_end == -1 or old_tasks_end > section_end:
            old_tasks_end = section_end
        
        # 새 작업 목록 생성
        new_tasks_list = "\n**Tasks**:\n"
        for task in tasks:
            new_tasks_list += f"- [ ] {task['key']}: {task['summary']}\n"
        new_tasks_list += "\n"
        
        # 교체
        content = content[:tasks_start] + new_tasks_list + content[old_tasks_end:]
        
        print(f"✅ {epic_key}: {len(tasks)}개 Task 업데이트")
    
    return content


def main():
    print("📖 JIRA 이슈 로드")
    epic_tasks = load_jira_issues()
    
    print("\n=== Epic별 Task 개수 ===")
    for epic_key in sorted(epic_tasks.keys()):
        print(f"{epic_key}: {len(epic_tasks[epic_key])}개")
    
    print("\n📝 문서 업데이트 시작")
    updated_content = update_document(epic_tasks)
    
    # 저장
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(updated_content)
    
    print(f"\n✅ 업데이트 완료: {OUTPUT_FILE}")
    print(f"   원본: {DOC_FILE}")


if __name__ == "__main__":
    main()
