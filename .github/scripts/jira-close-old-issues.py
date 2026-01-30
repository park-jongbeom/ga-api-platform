#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 이전 이슈를 취소 상태로 변경하는 스크립트 (무료 플랜 대응)
삭제 권한이 없는 경우 대안으로 사용
"""
import re
import sys
import argparse
import base64
import requests
from typing import List, Optional

def get_available_transitions(jira_url: str, headers: dict, issue_key: str) -> List[dict]:
    """이슈의 가능한 상태 전환 목록 조회"""
    try:
        url = f"{jira_url}/rest/api/3/issue/{issue_key}/transitions"
        response = requests.get(url, headers=headers)
        
        if response.status_code == 200:
            data = response.json()
            return data.get('transitions', [])
        else:
            return []
    except:
        return []

def transition_issue(jira_url: str, headers: dict, issue_key: str, transition_name: str) -> bool:
    """이슈 상태 전환"""
    try:
        # 먼저 가능한 전환 목록 조회
        transitions = get_available_transitions(jira_url, headers, issue_key)
        
        # 취소 관련 상태 찾기 (한국어/영어 모두 확인)
        cancel_transitions = ['취소', 'Cancel', 'Done', '완료', 'Closed', '종료', 'Resolved']
        
        transition_id = None
        for trans in transitions:
            trans_name = trans.get('name', '').lower()
            if any(cancel.lower() in trans_name for cancel in cancel_transitions):
                transition_id = trans.get('id')
                break
        
        # 찾지 못한 경우 첫 번째 전환 사용
        if not transition_id and transitions:
            transition_id = transitions[0].get('id')
        
        if not transition_id:
            print(f"  ⚠ 상태 전환 불가: {issue_key} - 전환 가능한 상태가 없음")
            return False
        
        # 상태 전환 실행
        url = f"{jira_url}/rest/api/3/issue/{issue_key}/transitions"
        payload = {
            "transition": {"id": transition_id}
        }
        
        response = requests.post(url, headers=headers, json=payload)
        
        if response.status_code == 204:
            return True
        else:
            return False
            
    except Exception as e:
        return False

def close_old_issues(jira_url: str, jira_email: str, jira_api_token: str, 
                     project_key: str, exclude_start: int, exclude_end: int, dry_run: bool = False):
    """이전 이슈를 취소 상태로 변경"""
    
    jira_url = jira_url.rstrip('/')
    
    # 인증 헤더
    auth_string = f"{jira_email}:{jira_api_token}"
    auth_header = base64.b64encode(auth_string.encode()).decode()
    headers = {
        "Authorization": f"Basic {auth_header}",
        "Accept": "application/json",
        "Content-Type": "application/json"
    }
    
    print("=" * 60)
    print("JIRA 이전 이슈 취소 처리 (무료 플랜 대응)")
    print("=" * 60)
    print(f"프로젝트: {project_key}")
    print(f"제외 범위: {project_key}-{exclude_start} ~ {project_key}-{exclude_end}")
    print(f"모드: {'DRY RUN (실제 변경 안 함)' if dry_run else '실제 상태 변경'}")
    print()
    
    # 이슈 조회 (API v3 search/jql 사용)
    print(f"프로젝트 {project_key}의 이전 이슈 조회 중...")
    issues_to_close = []
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
                issue_response = requests.get(issue_url, headers=headers, params={"fields": "key,summary,issuetype,status"})
                
                if issue_response.status_code == 200:
                    issue_data = issue_response.json()
                    issue_key = issue_data['key']
                    summary = issue_data['fields'].get('summary', 'N/A')
                    issue_type = issue_data['fields']['issuetype']['name']
                    current_status = issue_data['fields']['status']['name']
                    
                    # 이미 완료/취소 상태가 아닌 경우만 추가
                    if current_status.lower() not in ['done', '완료', 'closed', '취소', '종료', 'resolved', '해결됨']:
                        issues_to_close.append({
                            'key': issue_key,
                            'summary': summary,
                            'type': issue_type,
                            'status': current_status
                        })
            
            print(f"  조회됨: {len(issues_to_close)}개 (처리 대상)")
            
            # 다음 페이지 확인
            next_page_token = data.get('nextPageToken')
            if not next_page_token or data.get('isLast', False):
                break
                
        except Exception as e:
            print(f"✗ 이슈 조회 오류: {str(e)}")
            break
    
    print(f"\n취소 처리 대상 이슈: {len(issues_to_close)}개")
    
    if not issues_to_close:
        print("취소 처리할 이슈가 없습니다. (이미 모두 완료/취소 상태일 수 있음)")
        return
    
    # 처리 대상 목록 표시
    print("\n취소 처리 대상 이슈 목록:")
    for issue in issues_to_close[:20]:
        print(f"  - {issue['key']} [{issue['type']}] [{issue['status']}] {issue['summary'][:40]}")
    
    if len(issues_to_close) > 20:
        print(f"  ... 외 {len(issues_to_close) - 20}개")
    
    if dry_run:
        print("\n[DRY RUN] 실제 상태 변경은 수행하지 않았습니다.")
        return
    
    # 확인
    print("\n" + "=" * 60)
    print("⚠ 경고: 이 작업은 이슈 상태를 변경합니다!")
    print("=" * 60)
    response = input(f"\n정말로 {len(issues_to_close)}개의 이슈를 취소 상태로 변경하시겠습니까? (yes 입력): ")
    
    if response.lower() != 'yes':
        print("취소되었습니다.")
        return
    
    # 상태 변경 실행
    print("\n이슈 상태 변경 중...")
    print("(처리 중... API rate limit을 피하기 위해 각 요청 사이에 지연 시간이 있습니다)")
    closed_count = 0
    failed_count = 0
    skipped_count = 0
    
    import time
    
    for idx, issue in enumerate(issues_to_close, 1):
        issue_key = issue['key']
        
        # 진행 상황 표시 (10개마다)
        if idx % 10 == 0:
            print(f"  진행 중: {idx}/{len(issues_to_close)} ({closed_count}개 성공, {failed_count}개 실패)")
        
        try:
            # 상태 전환 시도
            if transition_issue(jira_url, headers, issue_key, "취소"):
                if idx % 10 == 0:  # 10개마다만 상세 출력
                    print(f"  ✓ 취소 처리 성공: {issue_key} [{issue['status']} → 취소]")
                closed_count += 1
            else:
                # 상태 전환 실패 시 댓글 추가로 표시
                comment_url = f"{jira_url}/rest/api/3/issue/{issue_key}/comment"
                comment_payload = {
                    "body": {
                        "type": "doc",
                        "version": 1,
                        "content": [
                            {
                                "type": "paragraph",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "[자동 처리] 이 이슈는 이전 작업으로 인해 취소 처리되었습니다."
                                    }
                                ]
                            }
                        ]
                    }
                }
                
                comment_response = requests.post(comment_url, headers=headers, json=comment_payload)
                
                if comment_response.status_code == 201:
                    if idx % 10 == 0:
                        print(f"  ⚠ 댓글 추가: {issue_key} (상태 변경 불가)")
                    skipped_count += 1
                else:
                    print(f"  ✗ 처리 실패: {issue_key} - 상태 변경 및 댓글 추가 모두 실패")
                    failed_count += 1
            
            # API rate limit 방지를 위한 지연 (요청당 0.5초)
            time.sleep(0.5)
                
        except Exception as e:
            print(f"  ✗ 처리 오류: {issue_key} - {str(e)}")
            failed_count += 1
            time.sleep(0.5)
    
    print("\n" + "=" * 60)
    print("처리 완료!")
    print("=" * 60)
    print(f"취소 처리 성공: {closed_count}개")
    print(f"댓글 추가 (상태 변경 불가): {skipped_count}개")
    print(f"처리 실패: {failed_count}개")
    print(f"유지된 이슈: {project_key}-{exclude_start} 이상")


def main():
    parser = argparse.ArgumentParser(description="JIRA 프로젝트에서 이전 이슈를 취소 상태로 변경")
    parser.add_argument("--jira-url", required=True, help="JIRA URL (예: https://your-domain.atlassian.net)")
    parser.add_argument("--jira-email", required=True, help="JIRA 이메일")
    parser.add_argument("--jira-api-token", required=True, help="JIRA API Token")
    parser.add_argument("--project-key", required=True, help="JIRA 프로젝트 키 (예: GA)")
    parser.add_argument("--exclude-start", type=int, required=True, help="제외할 이슈 번호 시작 (예: 433)")
    parser.add_argument("--exclude-end", type=int, required=True, help="제외할 이슈 번호 끝 (예: 573)")
    parser.add_argument("--dry-run", action="store_true", help="실제 변경 없이 미리보기만 수행")
    
    args = parser.parse_args()
    
    close_old_issues(
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
