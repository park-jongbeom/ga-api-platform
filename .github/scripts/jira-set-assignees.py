#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 이슈에 담당자 설정 스크립트
"""
import os
import sys
import json
import base64
import requests
import time
import argparse

class JiraAssigneeUpdater:
    def __init__(self, jira_url: str, jira_email: str, jira_api_token: str):
        self.jira_url = jira_url.rstrip('/')
        self.jira_email = jira_email
        self.jira_api_token = jira_api_token
        
        auth_string = f"{jira_email}:{jira_api_token}"
        self.auth_header = base64.b64encode(auth_string.encode()).decode()
        self.headers = {
            "Authorization": f"Basic {self.auth_header}",
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    
    def find_user_by_name(self, display_name: str):
        """이름으로 사용자 검색"""
        try:
            url = f"{self.jira_url}/rest/api/3/user/search"
            params = {"query": display_name}
            r = requests.get(url, headers=self.headers, params=params, timeout=10)
            if r.status_code == 200:
                users = r.json()
                for user in users:
                    if user.get('displayName') == display_name:
                        return user.get('accountId')
                # 정확히 일치하지 않으면 첫 번째 결과 반환
                if users:
                    print(f"  ℹ️ '{display_name}' 정확히 일치하는 사용자 없음. 첫 번째 결과 사용: {users[0].get('displayName')}")
                    return users[0].get('accountId')
            return None
        except Exception as e:
            print(f"  ✗ 사용자 검색 실패 ({display_name}): {e}")
            return None
    
    def set_assignee(self, issue_key: str, account_id: str) -> bool:
        """이슈에 담당자 설정"""
        url = f"{self.jira_url}/rest/api/3/issue/{issue_key}/assignee"
        payload = {"accountId": account_id}
        try:
            r = requests.put(url, headers=self.headers, json=payload, timeout=10)
            if r.status_code == 204:
                return True
            else:
                print(f"  ✗ {issue_key}: 담당자 설정 실패 (status: {r.status_code})")
                return False
        except Exception as e:
            print(f"  ✗ {issue_key}: 담당자 설정 실패 - {e}")
            return False
    
    def get_current_assignee(self, issue_key: str):
        """현재 담당자 조회"""
        url = f"{self.jira_url}/rest/api/3/issue/{issue_key}"
        params = {"fields": "assignee"}
        try:
            r = requests.get(url, headers=self.headers, params=params, timeout=10)
            if r.status_code == 200:
                data = r.json()
                assignee = data.get('fields', {}).get('assignee')
                if assignee:
                    return assignee.get('displayName'), assignee.get('accountId')
            return None, None
        except Exception:
            return None, None
    
    def run(self, backend_assignee: str, frontend_assignee: str, dry_run: bool = False):
        """담당자 일괄 설정"""
        # 사용자 계정 ID 조회
        print(f"사용자 검색 중...")
        backend_account_id = self.find_user_by_name(backend_assignee)
        frontend_account_id = self.find_user_by_name(frontend_assignee)
        
        if not backend_account_id:
            print(f"✗ 백엔드 담당자 '{backend_assignee}'를 찾을 수 없습니다.")
            return
        if not frontend_account_id:
            print(f"✗ 프론트엔드 담당자 '{frontend_assignee}'를 찾을 수 없습니다.")
            return
        
        print(f"✓ 백엔드 담당자: {backend_assignee} (ID: {backend_account_id})")
        print(f"✓ 프론트엔드 담당자: {frontend_assignee} (ID: {frontend_account_id})")
        print()
        
        # 백엔드 이슈: GAM-1 ~ GAM-141 (프론트는 GAM-142부터 시작)
        backend_issues = []
        for i in range(1, 142):
            backend_issues.append(f"GAM-{i}")
        
        # 프론트엔드 이슈: GAM-142 ~ GAM-228
        frontend_issues = []
        for i in range(142, 229):
            frontend_issues.append(f"GAM-{i}")
        
        updated = 0
        skipped = 0
        failed = 0
        
        # 백엔드 이슈 처리
        print(f"백엔드 이슈 처리 중 ({len(backend_issues)}개)...")
        for issue_key in backend_issues:
            current_name, current_id = self.get_current_assignee(issue_key)
            
            if current_id == backend_account_id:
                print(f"  ⊘ {issue_key}: 이미 {backend_assignee}에게 할당됨, 스킵")
                skipped += 1
                time.sleep(0.1)
                continue
            
            if dry_run:
                print(f"  [DRY RUN] {issue_key}: {current_name or '미할당'} → {backend_assignee}")
                updated += 1
                continue
            
            success = self.set_assignee(issue_key, backend_account_id)
            if success:
                print(f"  ✓ {issue_key}: {backend_assignee}에게 할당 완료")
                updated += 1
            else:
                failed += 1
            
            time.sleep(0.3)  # API rate limit 고려
        
        # 프론트엔드 이슈 처리
        print(f"\n프론트엔드 이슈 처리 중 ({len(frontend_issues)}개)...")
        for issue_key in frontend_issues:
            current_name, current_id = self.get_current_assignee(issue_key)
            
            if current_id == frontend_account_id:
                print(f"  ⊘ {issue_key}: 이미 {frontend_assignee}에게 할당됨, 스킵")
                skipped += 1
                time.sleep(0.1)
                continue
            
            if dry_run:
                print(f"  [DRY RUN] {issue_key}: {current_name or '미할당'} → {frontend_assignee}")
                updated += 1
                continue
            
            success = self.set_assignee(issue_key, frontend_account_id)
            if success:
                print(f"  ✓ {issue_key}: {frontend_assignee}에게 할당 완료")
                updated += 1
            else:
                failed += 1
            
            time.sleep(0.3)  # API rate limit 고려
        
        print(f"\n총 {updated}개 이슈 담당자 설정, {skipped}개 스킵, {failed}개 실패.")


def main():
    parser = argparse.ArgumentParser(description='JIRA 이슈 담당자 설정')
    parser.add_argument('--backend-assignee', default='박종범', help='백엔드 담당자 이름')
    parser.add_argument('--frontend-assignee', default='홍지운', help='프론트엔드 담당자 이름')
    parser.add_argument('--dry-run', action='store_true', help='실제 변경 없이 미리보기만')
    args = parser.parse_args()
    
    jira_url = os.getenv('JIRA_URL')
    jira_email = os.getenv('JIRA_EMAIL')
    jira_api_token = os.getenv('JIRA_API_TOKEN')
    
    # 환경 변수가 없으면 docs/jira/jira.env 파일 읽기
    if not all([jira_url, jira_email, jira_api_token]):
        env_file = 'docs/jira/jira.env'
        if os.path.exists(env_file):
            with open(env_file, 'r') as f:
                for line in f:
                    line = line.strip()
                    if '=' in line and not line.startswith('#'):
                        key, value = line.split('=', 1)
                        if key == 'JIRA_URL':
                            jira_url = value
                        elif key == 'JIRA_EMAIL':
                            jira_email = value
                        elif key == 'JIRA_API_TOKEN':
                            jira_api_token = value
    
    if not all([jira_url, jira_email, jira_api_token]):
        print("오류: JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 환경 변수 또는 인자가 필요합니다.")
        sys.exit(1)
    
    updater = JiraAssigneeUpdater(jira_url, jira_email, jira_api_token)
    updater.run(args.backend_assignee, args.frontend_assignee, args.dry_run)


if __name__ == '__main__':
    main()
