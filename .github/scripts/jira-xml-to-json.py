#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA XML 내보내기 파일을 JSON으로 변환
"""
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
XML_FILE = PROJECT_ROOT / "docs" / "jira" / "Jira_backend_issues.xml"
OUTPUT_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"


def parse_jira_xml(xml_path: Path) -> list:
    """JIRA XML 파일 파싱"""
    tree = ET.parse(xml_path)
    root = tree.getroot()
    
    issues = []
    
    # RSS 형식이면 channel/item, 아니면 직접 item 찾기
    items = root.findall(".//item")
    
    for item in items:
        key_elem = item.find("key")
        summary_elem = item.find("summary")
        type_elem = item.find("type")
        status_elem = item.find("status")
        parent_elem = item.find("parent")
        
        if key_elem is None or key_elem.text is None:
            continue
        
        key = key_elem.text.strip()
        summary = summary_elem.text.strip() if summary_elem is not None and summary_elem.text else ""
        issue_type = type_elem.text.strip() if type_elem is not None and type_elem.text else ""
        status = status_elem.text.strip() if status_elem is not None and status_elem.text else ""
        parent = parent_elem.text.strip() if parent_elem is not None and parent_elem.text else None
        
        issues.append({
            "key": key,
            "summary": summary,
            "type": issue_type,
            "status": status,
            "parent": parent
        })
    
    return issues


def main():
    if not XML_FILE.exists():
        print(f"❌ 오류: {XML_FILE} 파일이 없습니다", file=sys.stderr)
        sys.exit(1)
    
    print(f"📖 XML 파일 읽기: {XML_FILE}")
    issues = parse_jira_xml(XML_FILE)
    
    print(f"✅ {len(issues)}개 이슈 파싱 완료")
    
    # 타입별 통계
    by_type = {}
    for issue in issues:
        itype = issue["type"]
        by_type[itype] = by_type.get(itype, 0) + 1
    
    print("\n=== 이슈 타입별 통계 ===")
    for itype, count in sorted(by_type.items()):
        print(f"  {itype}: {count}개")
    
    # JSON 저장
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(issues, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ JSON 저장: {OUTPUT_FILE}")
    
    # 7개 Task Parent 확인
    check_tasks = ["GAM-31", "GAM-32", "GAM-33", "GAM-41", "GAM-51", "GAM-61", "GAM-62"]
    print("\n=== 7개 Task Parent 확인 ===")
    for issue in issues:
        if issue["key"] in check_tasks:
            print(f"  {issue['key']}: parent={issue['parent']}, summary={issue['summary'][:50]}")
    
    # Task 범위 확인
    tasks = [i for i in issues if i["type"] == "작업"]
    if tasks:
        task_nums = [int(t["key"].split("-")[1]) for t in tasks if "-" in t["key"]]
        if task_nums:
            print(f"\n=== Task 키 범위 ===")
            print(f"  최소: GAM-{min(task_nums)}")
            print(f"  최대: GAM-{max(task_nums)}")
            print(f"  총 개수: {len(tasks)}개")


if __name__ == "__main__":
    main()
