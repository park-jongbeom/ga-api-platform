#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 백로그 문서 파싱 및 자동 이슈 생성 스크립트

사용법:
    python3 jira-backlog-importer.py \
        --jira-url https://your-domain.atlassian.net \
        --jira-email your-email@example.com \
        --jira-api-token YOUR_API_TOKEN \
        --project-key QK54R \
        --backlog-file docs/jira/JIRA_BACKLOG.md

또는 환경 변수 사용:
    export JIRA_URL=https://your-domain.atlassian.net
    export JIRA_EMAIL=your-email@example.com
    export JIRA_API_TOKEN=YOUR_API_TOKEN
    python3 jira-backlog-importer.py
"""

import re
import json
import sys
import os
import argparse
import base64
import requests
from typing import Dict, List, Optional, Tuple
from pathlib import Path

class JiraBacklogImporter:
    def __init__(self, jira_url: str, jira_email: str, jira_api_token: str, project_key: str, backlog_file: str, 
                 backend_assignee_email: str = None, frontend_assignee_account_id: str = None):
        self.jira_url = jira_url.rstrip('/')
        self.jira_email = jira_email
        self.jira_api_token = jira_api_token
        self.project_key = project_key
        self.backlog_file = backlog_file
        self.backend_assignee_email = backend_assignee_email or jira_email
        self.frontend_assignee_account_id = frontend_assignee_account_id
        
        # 인증 헤더 생성
        auth_string = f"{jira_email}:{jira_api_token}"
        self.auth_header = base64.b64encode(auth_string.encode()).decode()
        self.headers = {
            "Authorization": f"Basic {self.auth_header}",
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        
        # 매핑 저장소
        self.mapping: Dict[str, str] = {}
        self.epic_keys: Dict[str, str] = {}  # GAM-1 -> GAM-1
        
        # Assignee 매핑 (백로그 파일명으로 구분)
        self.assignee_cache: Dict[str, str] = {}  # email -> accountId 캐시
    
    def get_assignee_account_id(self, assignee_text: str) -> Optional[str]:
        """Assignee 텍스트를 accountId로 변환"""
        assignee_text = assignee_text.strip().lower()
        
        # 백로그 파일명으로 구분
        is_frontend = 'front' in self.backlog_file.lower()
        
        # Backend Developer -> 백엔드 담당자
        if 'backend' in assignee_text or '백엔드' in assignee_text:
            return self.get_account_id_by_email(self.backend_assignee_email)
        
        # Frontend Developer -> 프론트엔드 담당자
        if 'frontend' in assignee_text or '프론트' in assignee_text:
            if self.frontend_assignee_account_id:
                return self.frontend_assignee_account_id
        
        return None
    
    def get_account_id_by_email(self, email: str) -> Optional[str]:
        """이메일로 accountId 조회"""
        if email in self.assignee_cache:
            return self.assignee_cache[email]
        
        try:
            url = f"{self.jira_url}/rest/api/3/user/search"
            params = {"query": email}
            response = requests.get(url, headers=self.headers, params=params)
            
            if response.status_code == 200:
                users = response.json()
                if users:
                    account_id = users[0].get('accountId')
                    if account_id:
                        self.assignee_cache[email] = account_id
                        return account_id
        except:
            pass
        
        return None
        
    def parse_backlog(self) -> Tuple[List[Dict], List[Dict], List[Dict]]:
        """백로그 문서를 파싱하여 Epic, Story, Task 정보 추출"""
        with open(self.backlog_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        epics = []
        stories = []
        tasks = []
        
        # Epic 파싱 (다음 Epic이 나올 때까지 또는 문서 끝까지)
        epic_pattern = r'## Epic \d+:(.*?)(?=## Epic \d+:|$)'
        epic_matches = re.finditer(epic_pattern, content, re.DOTALL)
        
        for epic_match in epic_matches:
            epic_text = epic_match.group(1)
            
            # Epic ID 추출
            epic_id_match = re.search(r'\*\*Epic ID\*\*:\s*(\S+)', epic_text)
            if not epic_id_match:
                continue
            
            epic_id = epic_id_match.group(1).strip()
            
            # Epic Name 추출
            epic_name_match = re.search(r'\*\*Epic Name\*\*:\s*(.+?)(?:\n|$)', epic_text)
            epic_name = epic_name_match.group(1).strip() if epic_name_match else ""
            
            # Business Value 추출
            business_value_match = re.search(r'\*\*Business Value\*\*:\s*(.+?)(?:\n|$)', epic_text)
            business_value = business_value_match.group(1).strip() if business_value_match else ""
            
            # Target Sprint 추출
            target_sprint_match = re.search(r'\*\*Target Sprint\*\*:\s*(.+?)(?:\n|$)', epic_text)
            target_sprint = target_sprint_match.group(1).strip() if target_sprint_match else ""
            
            # Total Story Points 추출
            story_points_match = re.search(r'\*\*Total Story Points\*\*:\s*(\d+)\s*SP', epic_text)
            total_sp = story_points_match.group(1) if story_points_match else ""
            
            epics.append({
                'id': epic_id,
                'name': epic_name,
                'business_value': business_value,
                'target_sprint': target_sprint,
                'total_story_points': total_sp,
                'text': epic_text
            })
            
            # Story 파싱 (Epic 내부)
            # Epic 섹션의 끝까지 파싱 (다음 Epic 또는 문서 끝까지)
            story_pattern = r'### Story (\S+):\s*(.+?)(?=### Story \S+:|## Epic \d+:|$)'
            story_matches = re.finditer(story_pattern, epic_text, re.DOTALL)
            
            for story_match in story_matches:
                story_id = story_match.group(1).strip()
                story_text = story_match.group(2)
                
                # Story Type 추출
                story_type_match = re.search(r'\*\*Story Type\*\*:\s*(.+?)(?:\n|$)', story_text)
                story_type = story_type_match.group(1).strip() if story_type_match else "Story"
                
                # Priority 추출
                priority_match = re.search(r'\*\*Priority\*\*:\s*(.+?)(?:\n|$)', story_text)
                priority = priority_match.group(1).strip() if priority_match else "Medium"
                
                # Story Points 추출
                sp_match = re.search(r'\*\*Story Points\*\*:\s*(\d+)\s*SP', story_text)
                story_points = sp_match.group(1) if sp_match else ""
                
                # Assignee 추출
                assignee_match = re.search(r'\*\*Assignee\*\*:\s*(.+?)(?:\n|$)', story_text)
                assignee = assignee_match.group(1).strip() if assignee_match else ""
                
                # Sprint 추출
                sprint_match = re.search(r'\*\*Sprint\*\*:\s*(.+?)(?:\n|$)', story_text)
                sprint = sprint_match.group(1).strip() if sprint_match else ""
                
                # Labels 추출
                labels_match = re.search(r'\*\*Labels\*\*:\s*(.+?)(?:\n|$)', story_text)
                labels_str = labels_match.group(1).strip() if labels_match else ""
                labels = [label.strip().strip('`') for label in labels_str.split(',')] if labels_str else []
                
                # Description 추출
                desc_match = re.search(r'\*\*Description\*\*:\s*(.+?)(?:\*\*User Story\*\*:|$)', story_text, re.DOTALL)
                description = desc_match.group(1).strip() if desc_match else ""
                
                # User Story 추출
                user_story_match = re.search(r'\*\*User Story\*\*:\s*```\s*(.+?)\s*```', story_text, re.DOTALL)
                user_story = user_story_match.group(1).strip() if user_story_match else ""
                
                # Acceptance Criteria 추출
                ac_match = re.search(r'\*\*Acceptance Criteria\*\*:\s*(.+?)(?:\*\*Tasks\*\*:|$)', story_text, re.DOTALL)
                acceptance_criteria = ac_match.group(1).strip() if ac_match else ""
                
                # Tasks 추출
                tasks_match = re.search(r'\*\*Tasks\*\*:\s*(.+?)(?:\*\*Technical Notes\*\*:|\*\*Definition of Done\*\*:|$)', story_text, re.DOTALL)
                tasks_text = tasks_match.group(1) if tasks_match else ""
                
                story_tasks = []
                if tasks_text:
                    task_pattern = r'- \[ \] (\S+):\s*(.+?)(?=\n- \[ \]|\n\n|$)'
                    task_matches = re.finditer(task_pattern, tasks_text, re.DOTALL)
                    for task_match in task_matches:
                        task_id = task_match.group(1).strip()
                        task_desc = task_match.group(2).strip()
                        story_tasks.append({
                            'id': task_id,
                            'description': task_desc
                        })
                
                # Technical Notes 추출
                tech_notes_match = re.search(r'\*\*Technical Notes\*\*:\s*```\w+\s*(.+?)\s*```', story_text, re.DOTALL)
                technical_notes = tech_notes_match.group(1).strip() if tech_notes_match else ""
                
                # Definition of Done 추출
                dod_match = re.search(r'\*\*Definition of Done\*\*:\s*(.+?)(?=---|$)', story_text, re.DOTALL)
                definition_of_done = dod_match.group(1).strip() if dod_match else ""
                
                # Story 제목 추출 (Story ID 다음)
                title_match = re.search(r'### Story \S+:\s*(.+?)(?:\n|$)', story_text)
                title = title_match.group(1).strip() if title_match else story_id
                
                stories.append({
                    'id': story_id,
                    'epic_id': epic_id,
                    'title': title,
                    'type': story_type,
                    'priority': priority,
                    'story_points': story_points,
                    'assignee': assignee,
                    'sprint': sprint,
                    'labels': labels,
                    'description': description,
                    'user_story': user_story,
                    'acceptance_criteria': acceptance_criteria,
                    'tasks': story_tasks,
                    'technical_notes': technical_notes,
                    'definition_of_done': definition_of_done
                })
                
                # Tasks를 tasks 리스트에 추가
                for task in story_tasks:
                    tasks.append({
                        'id': task['id'],
                        'story_id': story_id,
                        'description': task['description']
                    })
        
        return epics, stories, tasks
    
    def create_epic(self, epic: Dict) -> Optional[str]:
        """Epic 생성"""
        epic_name = epic['name']
        business_value = epic.get('business_value', '')
        target_sprint = epic.get('target_sprint', '')
        
        # Epic 설명 생성
        description = f"**Business Value**: {business_value}\n\n**Target Sprint**: {target_sprint}\n\n**Total Story Points**: {epic.get('total_story_points', '')} SP"
        
        payload = {
            "fields": {
                "project": {"key": self.project_key},
                "summary": epic_name,
                "description": {
                    "type": "doc",
                    "version": 1,
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": description
                                }
                            ]
                        }
                    ]
                },
                "issuetype": {"id": "10079"}  # 에픽 (Next-Gen 프로젝트는 ID 사용)
            }
        }
        
        # Epic Name 필드 설정 (JIRA 버전에 따라 다를 수 있음)
        # customfield_10011은 Epic Link 필드이므로 제거
        # Epic Name은 summary 필드에 이미 포함되어 있음
        
        try:
            response = requests.post(
                f"{self.jira_url}/rest/api/3/issue",
                headers=self.headers,
                json=payload
            )
            
            if response.status_code == 201:
                created_issue = response.json()
                issue_key = created_issue['key']
                print(f"✓ Epic 생성 성공: {epic['id']} -> {issue_key} ({epic_name})")
                return issue_key
            else:
                print(f"✗ Epic 생성 실패: {epic['id']} - {response.status_code} {response.text}")
                return None
        except Exception as e:
            print(f"✗ Epic 생성 오류: {epic['id']} - {str(e)}")
            return None
    
    def link_story_to_epic(self, story_key: str, epic_key: str) -> bool:
        """Story를 Epic에 연결 (Story 생성 후 호출)"""
        # JIRA API를 통한 Epic Link 설정 시도
        # 여러 가능한 Epic Link 필드 ID 시도
        epic_link_field_ids = ["customfield_10011", "customfield_10014", "customfield_10015"]
        
        for field_id in epic_link_field_ids:
            try:
                payload = {
                    "fields": {
                        field_id: epic_key
                    }
                }
                
                response = requests.put(
                    f"{self.jira_url}/rest/api/3/issue/{story_key}",
                    headers=self.headers,
                    json=payload
                )
                
                if response.status_code == 204:
                    print(f"    Epic 연결 성공: {story_key} -> {epic_key} (필드: {field_id})")
                    return True
            except:
                continue
        
        # Epic Link 필드로 연결 실패 시, Issue Link로 연결 시도
        try:
            payload = {
                "type": {"name": "Epic-Story Link"},
                "inwardIssue": {"key": story_key},
                "outwardIssue": {"key": epic_key}
            }
            
            response = requests.post(
                f"{self.jira_url}/rest/api/3/issueLink",
                headers=self.headers,
                json=payload
            )
            
            if response.status_code == 201:
                print(f"    Epic 연결 성공 (Issue Link): {story_key} -> {epic_key}")
                return True
        except:
            pass
        
        print(f"    ⚠ Epic 연결 실패: {story_key} -> {epic_key} (수동 연결 필요)")
        return False
    
    def create_story(self, story: Dict, epic_key: str) -> Optional[str]:
        """Story 생성 및 Epic 연결"""
        title = story['title']
        description = story.get('description', '')
        user_story = story.get('user_story', '')
        acceptance_criteria = story.get('acceptance_criteria', '')
        technical_notes = story.get('technical_notes', '')
        definition_of_done = story.get('definition_of_done', '')
        
        # Story 설명 생성
        story_description = ""
        if description:
            story_description += f"{description}\n\n"
        if user_story:
            story_description += f"**User Story:**\n```\n{user_story}\n```\n\n"
        if acceptance_criteria:
            story_description += f"**Acceptance Criteria:**\n{acceptance_criteria}\n\n"
        if technical_notes:
            story_description += f"**Technical Notes:**\n```\n{technical_notes}\n```\n\n"
        if definition_of_done:
            story_description += f"**Definition of Done:**\n{definition_of_done}"
        
        # 이슈 타입 결정 (ID로 변환)
        issue_type_id = "10078"  # 스토리 (기본값)
        issue_type = story.get('type', 'Story')
        if issue_type == 'Documentation' or issue_type == 'Story' or issue_type.lower() == 'story':
            issue_type_id = "10078"  # 스토리
        
        payload = {
            "fields": {
                "project": {"key": self.project_key},
                "summary": title,
                "description": {
                    "type": "doc",
                    "version": 1,
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": story_description
                                }
                            ]
                        }
                    ]
                },
                "issuetype": {"id": issue_type_id}  # Next-Gen 프로젝트는 ID 사용
            }
        }
        
        # Epic Link 필드 시도 (여러 가능한 필드 ID 시도)
        # Next-Gen 프로젝트의 경우 Epic Link 필드가 없을 수 있으므로
        # 일단 Story를 생성하고 나중에 Epic에 연결하는 방법 사용
        # customfield_10011은 일반적인 Epic Link 필드이지만, 이 프로젝트에서는 사용 불가능할 수 있음
        
        # Priority 설정
        priority_map = {
            "Critical": "Highest",
            "High": "High",
            "Medium": "Medium",
            "Low": "Low"
        }
        priority = priority_map.get(story.get('priority', 'Medium'), 'Medium')
        payload["fields"]["priority"] = {"name": priority}
        
        # Story Points 설정 (필드 ID는 프로젝트마다 다를 수 있음)
        if story.get('story_points'):
            # 일반적인 Story Points 필드 ID들 시도
            story_points_field_ids = ["customfield_10016", "customfield_10021"]
            for field_id in story_points_field_ids:
                try:
                    payload["fields"][field_id] = int(story['story_points'])
                    break
                except:
                    pass
        
        # Labels 설정
        if story.get('labels'):
            payload["fields"]["labels"] = story['labels']
        
        # Assignee 설정
        assignee = story.get('assignee', '')
        if assignee:
            assignee_account_id = self.get_assignee_account_id(assignee)
            if assignee_account_id:
                payload["fields"]["assignee"] = {"accountId": assignee_account_id}
        
        try:
            response = requests.post(
                f"{self.jira_url}/rest/api/3/issue",
                headers=self.headers,
                json=payload
            )
            
            if response.status_code == 201:
                created_issue = response.json()
                issue_key = created_issue['key']
                print(f"  ✓ Story 생성 성공: {story['id']} -> {issue_key} ({title})")
                
                # Epic에 연결 시도
                self.link_story_to_epic(issue_key, epic_key)
                
                return issue_key
            else:
                print(f"  ✗ Story 생성 실패: {story['id']} - {response.status_code} {response.text}")
                return None
        except Exception as e:
            print(f"  ✗ Story 생성 오류: {story['id']} - {str(e)}")
            return None
    
    def create_task(self, task: Dict, story_key: str, epic_key: str) -> Optional[str]:
        """Task 생성 및 Story 연결 (Next-Gen 프로젝트 대응)"""
        description = task['description']
        
        # Next-Gen 프로젝트에서는 Story가 Sub-task의 부모가 될 수 없으므로
        # 일반 Task로 생성하고 Story와 Issue Link로 연결
        # Task에는 Epic Link 필드가 없을 수 있으므로 제거
        payload = {
            "fields": {
                "project": {"key": self.project_key},
                "summary": description,
                "issuetype": {"id": "10076"}  # 작업 (Next-Gen 프로젝트는 ID 사용)
            }
        }
        
        try:
            response = requests.post(
                f"{self.jira_url}/rest/api/3/issue",
                headers=self.headers,
                json=payload
            )
            
            if response.status_code == 201:
                created_issue = response.json()
                issue_key = created_issue['key']
                print(f"    ✓ Task 생성 성공: {task['id']} -> {issue_key} ({description[:50]}...)")
                
                # Story와 Issue Link로 연결
                self.link_task_to_story(issue_key, story_key)
                
                return issue_key
            else:
                print(f"    ✗ Task 생성 실패: {task['id']} - {response.status_code} {response.text}")
                return None
        except Exception as e:
            print(f"    ✗ Task 생성 오류: {task['id']} - {str(e)}")
            return None
    
    def link_task_to_story(self, task_key: str, story_key: str) -> bool:
        """Task를 Story와 Issue Link로 연결"""
        try:
            payload = {
                "type": {"name": "Relates"},
                "inwardIssue": {"key": task_key},
                "outwardIssue": {"key": story_key}
            }
            
            response = requests.post(
                f"{self.jira_url}/rest/api/3/issueLink",
                headers=self.headers,
                json=payload
            )
            
            if response.status_code == 201:
                print(f"      Story 연결 성공: {task_key} -> {story_key}")
                return True
            else:
                print(f"      ⚠ Story 연결 실패: {task_key} -> {story_key} ({response.status_code})")
                return False
        except Exception as e:
            print(f"      ⚠ Story 연결 오류: {task_key} -> {story_key} ({str(e)})")
            return False
    
    def get_epic_link_field(self) -> Optional[str]:
        """Epic Link 필드 ID 조회"""
        try:
            response = requests.get(
                f"{self.jira_url}/rest/api/3/issue/createmeta?projectKeys={self.project_key}&issuetypeNames=Story&expand=projects.issuetypes.fields",
                headers=self.headers
            )
            
            if response.status_code == 200:
                meta = response.json()
                # Epic Link 필드 찾기 (일반적으로 customfield_10011)
                # 실제로는 메타데이터에서 찾아야 함
                return "customfield_10011"  # 기본값
            return None
        except:
            return "customfield_10011"  # 기본값
    
    def load_existing_mapping(self, mapping_file: str) -> None:
        """기존 매핑 파일 로드 (중복 이슈 생성 방지)."""
        if not os.path.exists(mapping_file):
            return
        try:
            with open(mapping_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            for k, v in data.items():
                if not k.startswith('_') and isinstance(v, str):
                    self.mapping[k] = v
            print(f"기존 매핑 로드: {len(self.mapping)}개 항목 (재실행 시 중복 생성 스킵)")
        except Exception as e:
            print(f"매핑 파일 로드 실패 (무시): {e}")

    def run(self):
        """백로그 파싱 및 JIRA 이슈 생성 실행"""
        print("=" * 60)
        print("JIRA 백로그 문서 파싱 및 자동 이슈 생성")
        print("=" * 60)
        print(f"프로젝트: {self.project_key}")
        print(f"백로그 파일: {self.backlog_file}")
        print()

        mapping_file = ".github/jira-mapping.json"
        # 기존 매핑 로드 (중복 일정/이슈 방지)
        self.load_existing_mapping(mapping_file)
        print()

        # 백로그 파싱
        print("백로그 문서 파싱 중...")
        epics, stories, tasks = self.parse_backlog()
        print(f"파싱 완료: Epic {len(epics)}개, Story {len(stories)}개, Task {len(tasks)}개")
        print()

        # Epic: 이미 매핑에 있으면 생성 스킵
        print("Epic 생성 중...")
        for epic in epics:
            if epic['id'] in self.mapping:
                epic_key = self.mapping[epic['id']]
                self.epic_keys[epic['id']] = epic_key
                print(f"  ⊘ Epic 스킵 (기존 이슈): {epic['id']} -> {epic_key}")
                continue
            epic_key = self.create_epic(epic)
            if epic_key:
                self.epic_keys[epic['id']] = epic_key
                self.mapping[epic['id']] = epic_key

        print()

        # Story: 이미 매핑에 있으면 생성 스킵
        print("Story 생성 중...")
        story_keys = {}
        for story in stories:
            epic_id = story['epic_id']
            epic_key = self.epic_keys.get(epic_id)

            if not epic_key:
                print(f"  ✗ Story 생성 실패: {story['id']} - Epic {epic_id}를 찾을 수 없음")
                continue

            if story['id'] in self.mapping:
                story_key = self.mapping[story['id']]
                story_keys[story['id']] = story_key
                print(f"  ⊘ Story 스킵 (기존 이슈): {story['id']} -> {story_key}")
                continue

            story_key = self.create_story(story, epic_key)
            if story_key:
                story_keys[story['id']] = story_key
                self.mapping[story['id']] = story_key

        print()

        # Task: 이미 매핑에 있으면 생성 스킵
        print("Task 생성 중...")
        for task in tasks:
            story_id = task['story_id']
            story_key = story_keys.get(story_id)

            if not story_key:
                print(f"    ✗ Task 생성 실패: {task['id']} - Story {story_id}를 찾을 수 없음")
                continue

            if task['id'] in self.mapping:
                print(f"    ⊘ Task 스킵 (기존 이슈): {task['id']} -> {self.mapping[task['id']]}")
                continue

            # Story의 Epic 찾기
            epic_id = None
            for story in stories:
                if story['id'] == story_id:
                    epic_id = story['epic_id']
                    break

            epic_key = self.epic_keys.get(epic_id) if epic_id else None

            task_key = self.create_task(task, story_key, epic_key)
            if task_key:
                self.mapping[task['id']] = task_key

        print()
        # 매핑 파일 저장 (기존 + 신규 병합)
        with open(mapping_file, 'w', encoding='utf-8') as f:
            json.dump(self.mapping, f, indent=2, ensure_ascii=False)
        
        print(f"매핑 테이블 저장 완료: {mapping_file}")
        print()
        print("=" * 60)
        print("완료!")
        print(f"생성된 이슈: {len(self.mapping)}개")
        print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description='JIRA 백로그 문서 파싱 및 자동 이슈 생성')
    parser.add_argument('--jira-url', help='JIRA 인스턴스 URL', default=os.getenv('JIRA_URL'))
    parser.add_argument('--jira-email', help='JIRA 계정 이메일', default=os.getenv('JIRA_EMAIL'))
    parser.add_argument('--jira-api-token', help='JIRA API 토큰', default=os.getenv('JIRA_API_TOKEN'))
    parser.add_argument('--project-key', help='JIRA 프로젝트 키', default='GAM')
    parser.add_argument('--backlog-file', help='백로그 문서 경로', default='docs/jira/JIRA_BACKLOG.md')
    parser.add_argument('--backend-assignee-email', help='백엔드 담당자 이메일', default=os.getenv('JIRA_EMAIL'))
    parser.add_argument('--frontend-assignee-account-id', help='프론트엔드 담당자 Account ID', 
                       default='557058:e1565656-70eb-4dcb-ac30-a2880e81a8db')  # 홍지운
    
    args = parser.parse_args()
    
    # 필수 파라미터 확인
    if not args.jira_url or not args.jira_email or not args.jira_api_token:
        print("오류: JIRA 정보가 필요합니다.")
        print("환경 변수 또는 명령줄 인자로 제공하세요:")
        print("  --jira-url, --jira-email, --jira-api-token")
        print("\n또는 환경 변수:")
        print("  export JIRA_URL=https://your-domain.atlassian.net")
        print("  export JIRA_EMAIL=your-email@example.com")
        print("  export JIRA_API_TOKEN=YOUR_API_TOKEN")
        sys.exit(1)
    
    # 백로그 파일 확인
    if not os.path.exists(args.backlog_file):
        print(f"오류: 백로그 파일을 찾을 수 없습니다: {args.backlog_file}")
        sys.exit(1)
    
    # Importer 실행
    importer = JiraBacklogImporter(
        jira_url=args.jira_url,
        jira_email=args.jira_email,
        jira_api_token=args.jira_api_token,
        project_key=args.project_key,
        backlog_file=args.backlog_file,
        backend_assignee_email=args.backend_assignee_email,
        frontend_assignee_account_id=args.frontend_assignee_account_id
    )
    
    importer.run()


if __name__ == '__main__':
    main()
