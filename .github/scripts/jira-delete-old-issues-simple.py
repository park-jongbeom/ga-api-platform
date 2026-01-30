#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 이전 이슈 삭제 스크립트 (API v2 사용 - 권한 문제 우회)
"""
import re
import sys
import argparse
import base64
import requests
from typing import List

def delete_old_issues(jira_url: str, jira_email: str, jira_api_token: str, 
                     project_key: str, exclude_start: int, exclude_end: int, dry_run: bool = False):
    """이전 이슈 삭제"""
    
    jira_url = jira_url.rstrip('/')
    
    # 인증 헤더
    auth_string = f"{jira_email}:{jira_api_token}"
    auth_header = base64.b64encode(auth_string.encode()).decode()
    headers = {
        "Authorization": f"Basic {auth_header}",
        "Accept": "application/json"
    }
    
    print("=" * 60)
    print("JIRA 이전 이슈 삭제 (API 사용)")
    print("=" * 60)
    print(f"프로젝트: {project_key}")
    print(f"제외 범위: {project_key}-{exclude_start} ~ {project_key}-{exclude_end}")
    print(f"모드: {'DRY RUN (실제 삭제 안 함)' if dry_run else '실제 삭제'}")
    print()
    
    # 이슈 조회 (API v3 search/jql 사용)
    print(f"프로젝트 {project_key}의 이전 이슈 조회 중...")
    issues_to_delete = []
    next_page_token = None
    max_results = 100
    
    while True:
        url = f"{jira_url}/rest/api/3/search/jql"
        payload = {
            "jql": f"project = {project_key} AND key < {project_key}-{exclude_start} ORDER BY key ASC",
            "maxResults": max_results
        }
        
        if next_page_token:
            payload["nextPageToken"] = next_page_token
        
        try:
            response = requests.post(url, headers=headers, json=payload)
            
            if response.status_code != 200:
                print(f"✗ 이슈 조회 실패: {response.status_code} {response.text}")
                break
            
            data = response.json()
            batch_issue_ids = [issue.get('id') for issue in data.get('issues', [])]
            
            if not batch_issue_ids:
                break
            
            # 각 이슈의 상세 정보 조회
            for issue_id in batch_issue_ids:
                issue_url = f"{jira_url}/rest/api/3/issue/{issue_id}"
                issue_response = requests.get(issue_url, headers=headers, params={"fields": "key,summary,issuetype"})
                
                if issue_response.status_code == 200:
                    issue_data = issue_response.json()
                    issue_key = issue_data['key']
                    summary = issue_data['fields'].get('summary', 'N/A')
                    issue_type = issue_data['fields']['issuetype']['name']
                    issues_to_delete.append({
                        'key': issue_key,
                        'summary': summary,
                        'type': issue_type
                    })
            
            print(f"  조회됨: {len(issues_to_delete)}개")
            
            # 다음 페이지 확인
            next_page_token = data.get('nextPageToken')
            if not next_page_token or data.get('isLast', False):
                break
            
        except Exception as e:
            print(f"✗ 이슈 조회 오류: {str(e)}")
            break
    
    print(f"\n삭제 대상 이슈: {len(issues_to_delete)}개")
    
    if not issues_to_delete:
        print("삭제할 이슈가 없습니다.")
        return
    
    # 삭제 대상 목록 표시
    print("\n삭제 대상 이슈 목록:")
    for issue in issues_to_delete[:20]:
        print(f"  - {issue['key']} [{issue['type']}] {issue['summary'][:50]}")
    
    if len(issues_to_delete) > 20:
        print(f"  ... 외 {len(issues_to_delete) - 20}개")
    
    if dry_run:
        print("\n[DRY RUN] 실제 삭제는 수행하지 않았습니다.")
        return
    
    # 확인
    print("\n" + "=" * 60)
    print("⚠ 경고: 이 작업은 되돌릴 수 없습니다!")
    print("=" * 60)
    response = input(f"\n정말로 {len(issues_to_delete)}개의 이슈를 삭제하시겠습니까? (yes 입력): ")
    
    if response.lower() != 'yes':
        print("취소되었습니다.")
        return
    
    # 삭제 실행
    print("\n이슈 삭제 중...")
    deleted_count = 0
    failed_count = 0
    
    for issue in issues_to_delete:
        issue_key = issue['key']
        
        try:
            # API v3로 삭제 (하위 작업도 함께 삭제)
            url = f"{jira_url}/rest/api/3/issue/{issue_key}"
            params = {"deleteSubtasks": "true"}
            
            response = requests.delete(url, headers=headers, params=params)
            
            if response.status_code == 204:
                print(f"  ✓ 삭제 성공: {issue_key} [{issue['type']}] {issue['summary'][:50]}")
                deleted_count += 1
            else:
                print(f"  ✗ 삭제 실패: {issue_key} - {response.status_code} {response.text[:100]}")
                failed_count += 1
                
        except Exception as e:
            print(f"  ✗ 삭제 오류: {issue_key} - {str(e)}")
            failed_count += 1
    
    print("\n" + "=" * 60)
    print("삭제 완료!")
    print("=" * 60)
    print(f"삭제 성공: {deleted_count}개")
    print(f"삭제 실패: {failed_count}개")
    print(f"유지된 이슈: {project_key}-{exclude_start} 이상")


def main():
    parser = argparse.ArgumentParser(description="JIRA 프로젝트에서 이전 이슈 삭제 (API 사용)")
    parser.add_argument("--jira-url", required=True, help="JIRA URL (예: https://your-domain.atlassian.net)")
    parser.add_argument("--jira-email", required=True, help="JIRA 이메일")
    parser.add_argument("--jira-api-token", required=True, help="JIRA API Token")
    parser.add_argument("--project-key", required=True, help="JIRA 프로젝트 키 (예: GA)")
    parser.add_argument("--exclude-start", type=int, required=True, help="제외할 이슈 번호 시작 (예: 433)")
    parser.add_argument("--exclude-end", type=int, required=True, help="제외할 이슈 번호 끝 (예: 573)")
    parser.add_argument("--dry-run", action="store_true", help="실제 삭제 없이 미리보기만 수행")
    
    args = parser.parse_args()
    
    delete_old_issues(
        jira_url=args.jira_url,
        jira_email=args.jira_email,
        jira_api_token=args.jira_api_token,
        project_key=args.project_key,
        exclude_start=args.exclude_start,
        exclude_end=args.exclude_end,
        dry_run=args.dry_run
    )


if __name__ == "__main__":
    main()
