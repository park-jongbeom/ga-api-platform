#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 최신 이슈 데이터 새로고침
"""
import os
import sys
import json
import base64
import requests
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
OUTPUT_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"

def load_jira_env():
    """jira.env 파일 로드"""
    for env_path in [
        PROJECT_ROOT / "docs" / "jira" / "jira.env",
        PROJECT_ROOT / "jira.env"
    ]:
        if env_path.exists():
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and "=" in line and not line.startswith("#"):
                        k, _, v = line.partition("=")
                        os.environ[k.strip()] = v.strip().strip('"\'')

def fetch_all_issues():
    """JIRA API로 모든 GAM 이슈 조회"""
    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""
    
    if not all([jira_url, jira_email, jira_token]):
        print("❌ JIRA 인증 정보 없음", file=sys.stderr)
        sys.exit(1)
    
    auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }
    
    all_issues = []
    start_at = 0
    max_results = 100
    
    while True:
        try:
            r = requests.get(
                f"{jira_url}/rest/api/2/search",
                headers=headers,
                params={
                    "jql": "project = GAM ORDER BY created ASC",
                    "fields": "summary,issuetype,status,parent",
                    "startAt": start_at,
                    "maxResults": max_results,
                },
                timeout=30,
            )
            
            if r.status_code != 200:
                print(f"❌ JIRA API 오류: {r.status_code}", file=sys.stderr)
                print(r.text[:500], file=sys.stderr)
                sys.exit(1)
            
            data = r.json()
            issues = data.get("issues", [])
            
            if not issues:
                break
            
            for issue in issues:
                key = issue["key"]
                fields = issue.get("fields", {})
                summary = fields.get("summary", "")
                issuetype = fields.get("issuetype", {}).get("name", "")
                status = fields.get("status", {}).get("name", "")
                parent = fields.get("parent", {})
                parent_key = parent.get("key") if parent else None
                
                all_issues.append({
                    "key": key,
                    "summary": summary,
                    "type": issuetype,
                    "status": status,
                    "parent": parent_key
                })
            
            print(f"조회 중: {len(all_issues)}개", end="\r")
            
            if len(issues) < max_results:
                break
            
            start_at += max_results
        
        except Exception as e:
            print(f"\n❌ 오류: {e}", file=sys.stderr)
            sys.exit(1)
    
    print(f"\n✅ 총 {len(all_issues)}개 이슈 조회 완료")
    return all_issues

def main():
    load_jira_env()
    issues = fetch_all_issues()
    
    # JSON 저장
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(issues, f, ensure_ascii=False, indent=2)
    
    print(f"✅ 저장: {OUTPUT_FILE}")
    
    # 통계 출력
    by_type = {}
    for issue in issues:
        itype = issue["type"]
        by_type[itype] = by_type.get(itype, 0) + 1
    
    print("\n=== 이슈 타입별 통계 ===")
    for itype, count in sorted(by_type.items()):
        print(f"  {itype}: {count}개")
    
    # 7개 Task 확인
    print("\n=== 7개 Task Parent 확인 ===")
    check_tasks = ["GAM-31", "GAM-32", "GAM-33", "GAM-41", "GAM-51", "GAM-61", "GAM-62"]
    for issue in issues:
        if issue["key"] in check_tasks:
            print(f"  {issue['key']}: parent={issue['parent']}, summary={issue['summary'][:50]}")

if __name__ == "__main__":
    main()
