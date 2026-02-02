#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 이슈에 작업 일정(시작일/종료일) 설정 스크립트
백로그 문서의 Epic/Story/Task 구조와 주차 정보를 파싱하여 날짜를 계산하고 JIRA API로 설정
"""
import re
import json
import os
import argparse
import base64
import requests
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
from pathlib import Path


def parse_week_number(sprint_text: str) -> Optional[int]:
    """Sprint/Week 텍스트에서 주차 번호 추출.
    범위 형식 (Week 1-2, Week 5-6)인 경우 마지막 주차 번호 반환.
    예: Week 1 -> 1, Week 1-2 -> 2, Week 5-6 -> 6
    """
    if not sprint_text:
        return None
    # 먼저 범위 형식 체크 (Week 1-2, Week 5-6)
    range_match = re.search(r'Week\s*(\d+)\s*-\s*(\d+)', sprint_text, re.IGNORECASE)
    if range_match:
        return int(range_match.group(2))  # 마지막 주차 반환
    # 단일 주차 (Week 1, Week 4 등)
    m = re.search(r'Week\s*(\d+)', sprint_text, re.IGNORECASE)
    return int(m.group(1)) if m else None


def work_days_in_range(start: datetime, end: datetime) -> List[datetime]:
    """시작일~종료일 사이의 평일(월~목) 목록"""
    days = []
    d = start
    while d <= end:
        if d.weekday() < 4:  # 0=월, 3=목
            days.append(d)
        d += timedelta(days=1)
    return days


def week_range(start_date: datetime, week_index: int, days_per_week: int = 4) -> Tuple[datetime, datetime]:
    """시작일(월요일) 기준 N주차의 (시작일, 종료일) 반환. 주당 평일 4일(월~목)."""
    # week_index는 1-based (Week 1, Week 2, ...)
    week_start = start_date + timedelta(weeks=week_index - 1)
    week_end = week_start + timedelta(days=3)  # 월~목
    return (week_start, week_end)


def parse_backlog_structure(backlog_path: str, is_frontend: bool) -> List[Dict]:
    """
    백로그 문서를 파싱하여 (id, week_number, type, explicit_date) 목록 반환.
    type: 'epic' | 'story' | 'task'
    explicit_date: 명시적으로 지정된 날짜 (YYYY-MM-DD) 또는 None
    """
    with open(backlog_path, 'r', encoding='utf-8') as f:
        content = f.read()

    items = []
    # Epic 패턴 (GAM-1 또는 GAMF-1 형식)
    epic_pattern = r'\*\*Epic ID\*\*:\s*(\S+).*?\*\*Target Sprint\*\*:\s*(.+?)(?:\n|$)'
    for m in re.finditer(epic_pattern, content, re.DOTALL):
        epic_id = m.group(1).strip()
        sprint = m.group(2).strip()
        
        # "기한: YYYY-MM-DD" 형식이 있는지 먼저 확인
        explicit_date = None
        date_match = re.search(r'기한:\s*(\d{4}-\d{2}-\d{2})', sprint)
        if date_match:
            explicit_date = date_match.group(1)
        
        week = parse_week_number(sprint)
        if week or explicit_date:
            items.append({'id': epic_id, 'week': week, 'type': 'epic', 'explicit_date': explicit_date})

    # Story 패턴: ### Story GAM-11: ... **Sprint**: Week 1
    story_pattern = r'### Story (\S+):\s*(.+?)(?=### Story \S+:|## Epic \d+:|$)'
    for m in re.finditer(story_pattern, content, re.DOTALL):
        story_id = m.group(1).strip()
        story_text = m.group(2)
        sprint_match = re.search(r'\*\*Sprint\*\*:\s*(.+?)(?:\n|$)', story_text)
        sprint = sprint_match.group(1).strip() if sprint_match else ""
        
        # "기한: YYYY-MM-DD" 형식이 있는지 먼저 확인
        explicit_date = None
        date_match = re.search(r'기한:\s*(\d{4}-\d{2}-\d{2})', sprint)
        if date_match:
            explicit_date = date_match.group(1)
        
        week = parse_week_number(sprint)
        if week or explicit_date:
            items.append({'id': story_id, 'week': week, 'type': 'story', 'explicit_date': explicit_date})

    # Task 패턴: - [ ] GAM-11-1: ...
    # Task는 부모 Story의 주차를 사용하므로 Story 목록을 먼저 만들어서 story_id -> (week, explicit_date) 매핑
    story_info = {}
    for it in items:
        if it['type'] == 'story':
            story_info[it['id']] = {'week': it.get('week'), 'explicit_date': it.get('explicit_date')}

    task_pattern = r'- \[ \] (\S+):\s*'
    for m in re.finditer(task_pattern, content):
        task_id = m.group(1).strip()
        # GAM-11-1 -> story_id = GAM-11
        parts = task_id.split('-')
        if len(parts) >= 2 and re.match(r'^[\w]+-\d+-\d+$', task_id):
            story_id = '-'.join(parts[:-1])  # GAM-11
            info = story_info.get(story_id)
            if info and (info['week'] or info['explicit_date']):
                items.append({
                    'id': task_id, 
                    'week': info['week'], 
                    'type': 'task',
                    'explicit_date': info['explicit_date']
                })

    return items


def build_week_calendar(start_date_str: str, backend_weeks: int, frontend_weeks: float) -> Dict:
    """주차별 (시작일, 종료일) 캘린더 생성. 평일만 (월~목)."""
    start = datetime.strptime(start_date_str, '%Y-%m-%d')
    calendar = {}
    # 백엔드와 프론트엔드 모두 같은 주차 캘린더 사용 (최대 6주)
    max_weeks = max(backend_weeks, int(frontend_weeks) if frontend_weeks == int(frontend_weeks) else int(frontend_weeks) + 1)
    for w in range(1, max_weeks + 1):
        week_start, week_end = week_range(start, w)
        calendar[w] = (week_start.strftime('%Y-%m-%d'), week_end.strftime('%Y-%m-%d'))
    return calendar


class JiraSetDates:
    def __init__(self, jira_url: str, jira_email: str, jira_api_token: str, project_key: str,
                 mapping_file: str, start_date: str, backend_weeks: int, frontend_weeks: float):
        self.jira_url = jira_url.rstrip('/')
        self.jira_email = jira_email
        self.jira_api_token = jira_api_token
        self.project_key = project_key
        self.mapping_file = mapping_file
        self.start_date = start_date
        self.backend_weeks = backend_weeks
        self.frontend_weeks = frontend_weeks

        auth_string = f"{jira_email}:{jira_api_token}"
        self.auth_header = base64.b64encode(auth_string.encode()).decode()
        self.headers = {
            "Authorization": f"Basic {self.auth_header}",
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        self.mapping: Dict[str, str] = {}
        self.calendar = build_week_calendar(start_date, backend_weeks, frontend_weeks)

    def load_mapping(self) -> None:
        with open(self.mapping_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        for k, v in data.items():
            if not k.startswith('_') and isinstance(v, str):
                self.mapping[k] = v

    def get_dates_for_week(self, week: int, is_frontend: bool, index_in_week: int = 0, total_in_week: int = 1) -> Tuple[str, str]:
        """해당 주차의 시작일/종료일. 동일 주 내에서 여러 이슈가 있으면 일자 분배."""
        range_key = week
        start_str, end_str = self.calendar[range_key]
        start_d = datetime.strptime(start_str, '%Y-%m-%d')
        end_d = datetime.strptime(end_str, '%Y-%m-%d')
        days = work_days_in_range(start_d, end_d)
        if not days:
            return (start_str, end_str)
        if total_in_week <= 1:
            return (days[0].strftime('%Y-%m-%d'), days[-1].strftime('%Y-%m-%d'))
        # 주 내에서 균등 분배
        step = max(1, len(days) // total_in_week)
        i = min(index_in_week * step, len(days) - 1)
        j = min(i + step, len(days)) - 1
        return (days[i].strftime('%Y-%m-%d'), days[j].strftime('%Y-%m-%d'))

    def get_issue_duedate(self, issue_key: str) -> Optional[str]:
        """이슈의 현재 duedate 조회 (중복 설정 방지용)."""
        url = f"{self.jira_url}/rest/api/3/issue/{issue_key}"
        try:
            r = requests.get(url, headers=self.headers, params={"fields": "duedate"})
            if r.status_code != 200:
                return None
            data = r.json()
            return (data.get("fields") or {}).get("duedate")
        except Exception:
            return None

    def set_issue_dates(self, issue_key: str, start_date: str, due_date: str) -> bool:
        """JIRA 이슈에 시작일/종료일 설정."""
        url = f"{self.jira_url}/rest/api/3/issue/{issue_key}"
        payload = {"fields": {"duedate": due_date}}
        # Start date: customfield_10015 등 프로젝트별로 다를 수 있음
        try:
            r = requests.put(url, headers=self.headers, json=payload)
            if r.status_code == 204:
                return True
            # duedate만 실패할 수 있으므로 400이면 로그만
            if r.status_code != 204:
                return False
        except Exception:
            return False
        return True

    def run(self, backlog_backend: str, backlog_frontend: str, dry_run: bool = False,
            skip_if_set: bool = False) -> None:
        self.load_mapping()
        backend_items = parse_backlog_structure(backlog_backend, False)
        frontend_items = parse_backlog_structure(backlog_frontend, True)

        # 주차별 개수 먼저 계산 후, 주 내에서 인덱스로 일자 분배
        def assign_dates(items: List[Dict], is_frontend: bool) -> List[Tuple[str, str, str]]:
            week_totals: Dict[int, int] = {}
            for it in items:
                w = it.get('week')
                if w:
                    week_totals[w] = week_totals.get(w, 0) + 1
            week_indices: Dict[int, int] = {}
            result = []
            for it in items:
                # 명시적 날짜가 있으면 그것을 우선 사용
                if it.get('explicit_date'):
                    explicit = it['explicit_date']
                    result.append((it['id'], explicit, explicit))
                    continue
                    
                w = it.get('week')
                if not w:
                    continue
                idx = week_indices.get(w, 0)
                total = week_totals[w]
                start_d, end_d = self.get_dates_for_week(w, is_frontend, idx, total)
                week_indices[w] = idx + 1
                result.append((it['id'], start_d, end_d))
            return result

        backend_dates = assign_dates(backend_items, False)
        frontend_dates = assign_dates(frontend_items, True)
        all_dates = backend_dates + frontend_dates

        updated = 0
        skipped = 0
        for backlog_id, start_d, end_d in all_dates:
            jira_key = self.mapping.get(backlog_id)
            # 매핑에 없으면 백엔드(GAM-*)는 이슈 키가 백로그 ID와 동일
            if not jira_key and backlog_id.startswith(self.project_key + '-') and not backlog_id.startswith('GAMF-'):
                jira_key = backlog_id
            if not jira_key:
                continue
            if dry_run:
                print(f"[DRY RUN] {backlog_id} -> {jira_key}: {start_d} ~ {end_d}")
                updated += 1
                continue
            # 중복 일정 방지: 이미 duedate가 설정된 경우 스킵
            current_due = self.get_issue_duedate(jira_key)
            if current_due is not None:
                if skip_if_set:
                    print(f"  ⊘ {backlog_id} -> {jira_key}: 이미 설정됨(duedate={current_due}), 스킵")
                    skipped += 1
                    time.sleep(0.2)
                    continue
                if current_due == end_d:
                    print(f"  ⊘ {backlog_id} -> {jira_key}: 동일한 duedate({end_d}), 스킵")
                    skipped += 1
                    time.sleep(0.2)
                    continue
            if self.set_issue_dates(jira_key, start_d, end_d):
                print(f"  ✓ {backlog_id} -> {jira_key}: duedate={end_d}")
                updated += 1
            else:
                print(f"  ✗ {backlog_id} -> {jira_key}: 설정 실패")
            time.sleep(0.3)
        print(f"\n총 {updated}개 이슈 날짜 설정, {skipped}개 스킵(중복 방지).")


def main():
    parser = argparse.ArgumentParser(description='JIRA 이슈에 작업 일정 날짜 설정')
    parser.add_argument('--jira-url', default=os.getenv('JIRA_URL'), help='JIRA URL')
    parser.add_argument('--jira-email', default=os.getenv('JIRA_EMAIL'), help='JIRA 이메일')
    parser.add_argument('--jira-api-token', default=os.getenv('JIRA_API_TOKEN'), help='JIRA API 토큰')
    parser.add_argument('--project-key', default='GAM', help='프로젝트 키')
    parser.add_argument('--mapping-file', default='.github/jira-mapping.json', help='매핑 파일')
    parser.add_argument('--start-date', default='2026-01-27', help='시작일 (YYYY-MM-DD)')
    parser.add_argument('--backend-weeks', type=int, default=6, help='백엔드 주차 수')
    parser.add_argument('--frontend-weeks', type=float, default=2.5, help='프론트엔드 주차 수')
    parser.add_argument('--backlog-backend', default='docs/jira/JIRA_BACKLOG.md', help='백엔드 백로그 경로')
    parser.add_argument('--backlog-frontend', default='docs/jira/FRONT_JIRA_BACKLOG.md', help='프론트엔드 백로그 경로')
    parser.add_argument('--dry-run', action='store_true', help='실제 API 호출 없이 미리보기')
    parser.add_argument('--skip-if-set', action='store_true',
                        help='이미 duedate가 설정된 이슈는 건너뛰기 (중복 일정 방지)')
    args = parser.parse_args()

    if not args.jira_url or not args.jira_email or not args.jira_api_token:
        print("오류: JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 환경 변수 또는 인자가 필요합니다.")
        return

    setter = JiraSetDates(
        jira_url=args.jira_url,
        jira_email=args.jira_email,
        jira_api_token=args.jira_api_token,
        project_key=args.project_key,
        mapping_file=args.mapping_file,
        start_date=args.start_date,
        backend_weeks=args.backend_weeks,
        frontend_weeks=args.frontend_weeks
    )
    print("JIRA 일정 설정 (시작일: {}, 백엔드 {}주, 프론트 {}주)".format(args.start_date, args.backend_weeks, args.frontend_weeks))
    if args.skip_if_set:
        print("옵션: 이미 duedate가 있는 이슈는 스킵 (중복 일정 방지)")
    setter.run(args.backlog_backend, args.backlog_frontend, dry_run=args.dry_run, skip_if_set=args.skip_if_set)


if __name__ == '__main__':
    main()
