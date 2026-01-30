#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 프로젝트에서 이전에 생성된 이슈들을 삭제하는 스크립트
(현재 시스템으로 자동 생성된 이슈 범위를 제외)
"""
import re
import json
import sys
import argparse
import base64
import requests
from typing import List, Set
from pathlib import Path

class JiraCleanupOldIssues:
    def __init__(self, jira_url: str, jira_email: str, jira_api_token: str, project_key: str, 
                 exclude_start: int, exclude_end: int):
        self.jira_url = jira_url.rstrip('/')
        self.jira_email = jira_email
        self.jira_api_token = jira_api_token
        self.project_key = project_key
        self.exclude_start = exclude_start
        self.exclude_end = exclude_end
        
        auth_string = f"{jira_email}:{jira_api_token}"
        self.auth_header = base64.b64encode(auth_string.encode()).decode()
        self.headers = {
            "Authorization": f"Basic {self.auth_header}",
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    
    def get_all_issues(self) -> List[dict]:
        """프로젝트의 모든 이슈 조회"""
        all_issues = []
        start_at = 0
        max_results = 100
        
        print(f"프로젝트 {self.project_key}의 모든 이슈 조회 중...")
        
        while True:
            url = f"{self.jira_url}/rest/api/3/search/jql"
            params = {
                "jql": f"project = {self.project_key} ORDER BY key ASC",
                "startAt": start_at,
                "maxResults": max_results,
                "fields": "*all"
            }
            
            try:
                response = requests.get(url, headers=self.headers, params=params)
                
                if response.status_code != 200:
                    print(f"✗ 이슈 조회 실패: {response.status_code} {response.text}")
                    break
                
                data = response.json()
                issues = data.get('issues', [])
                
                if not issues:
                    break
                
                all_issues.extend(issues)
                print(f"  조회됨: {len(all_issues)}개 (전체: {data.get('total', 0)}개)")
                
                if len(issues) < max_results:
                    break
                
                start_at += max_results
                
            except Exception as e:
                print(f"✗ 이슈 조회 오류: {str(e)}")
                break
        
        return all_issues
    
    def should_exclude_issue(self, issue_key: str) -> bool:
        """이슈 키가 제외 범위에 있는지 확인"""
        # GA-XXX 형식에서 숫자 추출
        match = re.match(rf"{self.project_key}-(\d+)", issue_key)
        if not match:
            return False
        
        issue_num = int(match.group(1))
        return self.exclude_start <= issue_num <= self.exclude_end
    
    def delete_issue(self, issue_key: str) -> bool:
        """이슈 삭제"""
        try:
            # 먼저 이슈 정보 조회
            response = requests.get(
                f"{self.jira_url}/rest/api/3/issue/{issue_key}",
                headers=self.headers
            )
            
            if response.status_code != 200:
                print(f"  ⚠ 이슈 조회 실패: {issue_key} - {response.status_code}")
                return False
            
            issue_data = response.json()
            summary = issue_data['fields'].get('summary', 'N/A')
            issue_type = issue_data['fields']['issuetype']['name']
            
            # 이슈 삭제
            response = requests.delete(
                f"{self.jira_url}/rest/api/3/issue/{issue_key}",
                headers=self.headers,
                params={"deleteSubtasks": "true"}  # 하위 작업도 함께 삭제
            )
            
            if response.status_code == 204:
                print(f"  ✓ 삭제 성공: {issue_key} [{issue_type}] {summary[:50]}")
                return True
            else:
                print(f"  ✗ 삭제 실패: {issue_key} - {response.status_code} {response.text}")
                return False
                
        except Exception as e:
            print(f"  ✗ 삭제 오류: {issue_key} - {str(e)}")
            return False
    
    def run(self, dry_run: bool = False):
        """이전 이슈 삭제 실행"""
        print("=" * 60)
        print("JIRA 이전 이슈 삭제")
        print("=" * 60)
        print(f"프로젝트: {self.project_key}")
        print(f"제외 범위: {self.project_key}-{self.exclude_start} ~ {self.project_key}-{self.exclude_end}")
        print(f"모드: {'DRY RUN (실제 삭제 안 함)' if dry_run else '실제 삭제'}")
        print()
        
        # 모든 이슈 조회
        all_issues = self.get_all_issues()
        print(f"\n전체 이슈 수: {len(all_issues)}개")
        
        # 제외할 이슈와 삭제할 이슈 분류
        exclude_issues = []
        delete_issues = []
        
        for issue in all_issues:
            issue_key = issue['key']
            if self.should_exclude_issue(issue_key):
                exclude_issues.append(issue)
            else:
                delete_issues.append(issue)
        
        print(f"\n제외할 이슈 (유지): {len(exclude_issues)}개")
        print(f"삭제할 이슈: {len(delete_issues)}개")
        
        if delete_issues:
            print("\n삭제 대상 이슈 목록:")
            for issue in delete_issues[:20]:  # 처음 20개만 미리보기
                issue_key = issue['key']
                summary = issue['fields'].get('summary', 'N/A')
                issue_type = issue['fields']['issuetype']['name']
                print(f"  - {issue_key} [{issue_type}] {summary[:60]}")
            
            if len(delete_issues) > 20:
                print(f"  ... 외 {len(delete_issues) - 20}개")
        
        if dry_run:
            print("\n[DRY RUN] 실제 삭제는 수행하지 않았습니다.")
            return
        
        # 확인
        print("\n" + "=" * 60)
        print("⚠ 경고: 이 작업은 되돌릴 수 없습니다!")
        print("=" * 60)
        response = input(f"\n정말로 {len(delete_issues)}개의 이슈를 삭제하시겠습니까? (yes 입력): ")
        
        if response.lower() != 'yes':
            print("취소되었습니다.")
            return
        
        # 삭제 실행
        print("\n이슈 삭제 중...")
        deleted_count = 0
        failed_count = 0
        
        for issue in delete_issues:
            issue_key = issue['key']
            if self.delete_issue(issue_key):
                deleted_count += 1
            else:
                failed_count += 1
        
        print("\n" + "=" * 60)
        print("삭제 완료!")
        print("=" * 60)
        print(f"삭제 성공: {deleted_count}개")
        print(f"삭제 실패: {failed_count}개")
        print(f"유지된 이슈: {len(exclude_issues)}개")


def main():
    parser = argparse.ArgumentParser(description="JIRA 프로젝트에서 이전 이슈 삭제")
    parser.add_argument("--jira-url", required=True, help="JIRA URL (예: https://your-domain.atlassian.net)")
    parser.add_argument("--jira-email", required=True, help="JIRA 이메일")
    parser.add_argument("--jira-api-token", required=True, help="JIRA API Token")
    parser.add_argument("--project-key", required=True, help="JIRA 프로젝트 키 (예: GA)")
    parser.add_argument("--exclude-start", type=int, required=True, help="제외할 이슈 번호 시작 (예: 292)")
    parser.add_argument("--exclude-end", type=int, required=True, help="제외할 이슈 번호 끝 (예: 432)")
    parser.add_argument("--dry-run", action="store_true", help="실제 삭제 없이 미리보기만 수행")
    
    args = parser.parse_args()
    
    cleaner = JiraCleanupOldIssues(
        jira_url=args.jira_url,
        jira_email=args.jira_email,
        jira_api_token=args.jira_api_token,
        project_key=args.project_key,
        exclude_start=args.exclude_start,
        exclude_end=args.exclude_end
    )
    
    cleaner.run(dry_run=args.dry_run)


if __name__ == "__main__":
    main()
