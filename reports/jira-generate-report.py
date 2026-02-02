#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 프로젝트 진행 상황 보고서 생성 스크립트.
전체/완료/진행중/남은 작업을 분석하여 마크다운 보고서를 생성하고,
파일로 저장하거나 GitHub Issue 본문으로 사용할 수 있음.
"""
import os
import re
import sys
import json
import argparse
import base64
import requests
import time
from datetime import datetime
from typing import Dict, List, Optional
from pathlib import Path


DONE_STATUSES = {'done', '완료', 'complete', 'closed', '종료', 'resolved', '해결됨'}
IN_PROGRESS_STATUSES = {'in progress', '진행 중', 'in progress', 'code review', 'testing'}
TO_DO_STATUSES = {'to do', '해야 할 일', 'open', 'backlog'}


def normalize_status(s: str) -> str:
    t = (s or '').strip().lower()
    if t in DONE_STATUSES or any(d in t for d in ['done', '완료', 'complete', 'closed', '종료']):
        return 'done'
    if t in IN_PROGRESS_STATUSES or any(p in t for p in ['진행 중', 'in progress']):
        return 'in_progress'
    return 'to_do'


def load_display_titles_from_backlogs(
    backend_backlog_path: str,
    frontend_backlog_path: str,
    mapping_file: str,
) -> Dict[str, str]:
    """
    백로그 문서에서 이슈 키별 표시 제목을 파싱.
    반환: jira_key -> 표시 제목 (예: GAM-11 -> "Mock API 명세 구현", GAM-145 -> "회원가입/로그인")
    """
    titles: Dict[str, str] = {}
    # 백엔드: Epic ID/Name, Story KEY: Title, Task KEY: Title
    if os.path.exists(backend_backlog_path):
        content = Path(backend_backlog_path).read_text(encoding='utf-8')
        for m in re.finditer(r'\*\*Epic ID\*\*:\s*(\S+).*?\*\*Epic Name\*\*:\s*(.+?)(?:\n|$)', content, re.DOTALL):
            key, name = m.group(1).strip(), m.group(2).strip()
            if key and name:
                titles[key] = name
        for m in re.finditer(r'### Story (GAM-\d+):\s*(.+?)(?:\n|$)', content):
            key, name = m.group(1).strip(), m.group(2).strip()
            if key and name:
                titles[key] = name
        for m in re.finditer(r'- \[ \] (GAM-\d+-\d+):\s*(.+?)(?:\n|$)', content):
            key, name = m.group(1).strip(), m.group(2).strip()
            if key and name:
                titles[key] = name
    # 프론트: Epic ID/Name, Story GAMF-xx: Title; 매핑(백로그 ID -> JIRA 키)으로 표시 제목 대입
    mapping_backlog_to_jira: Dict[str, str] = {}
    if os.path.exists(mapping_file):
        with open(mapping_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        for k, v in data.items():
            if not k.startswith('_') and isinstance(v, str):
                mapping_backlog_to_jira[k] = v  # GAMF-11 -> GAM-145
    if os.path.exists(frontend_backlog_path):
        content = Path(frontend_backlog_path).read_text(encoding='utf-8')
        for m in re.finditer(r'\*\*Epic ID\*\*:\s*(\S+).*?\*\*Epic Name\*\*:\s*(.+?)(?:\n|$)', content, re.DOTALL):
            key, name = m.group(1).strip(), m.group(2).strip()
            if key and name:
                jira_key = mapping_backlog_to_jira.get(key)
                if jira_key:
                    titles[jira_key] = name
        for m in re.finditer(r'### Story (GAMF-\d+):\s*(.+?)(?:\n|$)', content):
            key, name = m.group(1).strip(), m.group(2).strip()
            if key and name:
                jira_key = mapping_backlog_to_jira.get(key)
                if jira_key:
                    titles[jira_key] = name
    return titles


def load_frontend_jira_keys(mapping_file: str) -> set:
    """매핑에서 GAMF-* 키에 대응하는 JIRA 이슈 키(GAM-xxx) 집합 반환. 프론트엔드 구분용."""
    frontend = set()
    if not os.path.exists(mapping_file):
        return frontend
    with open(mapping_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    for k, v in data.items():
        if not k.startswith('_') and isinstance(v, str) and k.startswith('GAMF-'):
            frontend.add(v)
    return frontend


def load_canonical_keys(mapping_file: str, backend_backlog_path: Optional[str] = None) -> set:
    """
    정규(참조할) 이슈 키 집합 로드.
    - 매핑 파일의 값(GAM-xxx) + 백엔드 백로그에 등장하는 ID(GAM-*, GAM-*-*)를 정규로 간주.
    """
    canonical = set()
    if os.path.exists(mapping_file):
        with open(mapping_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        for k, v in data.items():
            if not k.startswith('_') and isinstance(v, str):
                canonical.add(v)
    if backend_backlog_path and os.path.exists(backend_backlog_path):
        content = Path(backend_backlog_path).read_text(encoding='utf-8')
        # GAM-1, GAM-11, GAM-11-1 등 백엔드 ID 추출 (GAMF- 제외)
        for m in re.finditer(r'\b(GAM-\d+(?:-\d+)?)\b', content):
            canonical.add(m.group(1))
    return canonical


def fetch_all_issues(jira_url: str, headers: dict, project_key: str) -> List[dict]:
    """JIRA 프로젝트의 모든 이슈 조회 (search/jql, 페이지네이션)."""
    url = f"{jira_url}/rest/api/3/search/jql"
    all_issues = []
    next_token = None
    max_results = 100
    while True:
        payload = {
            "jql": f"project = {project_key} ORDER BY key ASC",
            "maxResults": max_results
        }
        if next_token:
            payload["nextPageToken"] = next_token
        try:
            r = requests.post(url, headers=headers, json=payload)
            if r.status_code != 200:
                break
            data = r.json()
            batch = data.get('issues', [])
            if not batch:
                break
            for issue in batch:
                issue_id = issue.get('id')
                if not issue_id:
                    continue
                # 상세 필드가 없을 수 있으므로 개별 조회
                detail_url = f"{jira_url}/rest/api/3/issue/{issue_id}"
                dr = requests.get(detail_url, headers=headers, params={"fields": "key,summary,issuetype,status,duedate,created"})
                if dr.status_code == 200:
                    all_issues.append(dr.json())
            next_token = data.get('nextPageToken')
            if not next_token or data.get('isLast'):
                break
            time.sleep(0.3)
        except Exception:
            break
    return all_issues


def build_report_markdown(
    issues: List[dict],
    report_date: str,
    report_web_url: str,
    project_name: str = "Go Almond Matching",
    display_titles: Optional[Dict[str, str]] = None,
    frontend_keys: Optional[set] = None,
) -> str:
    """마크다운 보고서 본문 생성. display_titles가 있으면 백로그 제목 우선. frontend_keys가 있으면 백엔드/프론트 구분 표시."""
    done_backend: List[str] = []
    done_frontend: List[str] = []
    in_progress_backend: List[str] = []
    in_progress_frontend: List[str] = []
    to_do_backend: List[str] = []
    to_do_frontend: List[str] = []
    epic_backend: List[str] = []
    epic_frontend: List[str] = []
    titles = display_titles or {}
    is_frontend = (frontend_keys or set())

    for raw in issues:
        key = raw.get('key', '')
        fields = raw.get('fields', {})
        summary = (fields.get('summary') or '').strip()
        display_name = titles.get(key) or (titles.get(summary) if summary and re.match(r'^[A-Z]+-\d+$', summary) else None) or summary or key
        itype = (fields.get('issuetype') or {}).get('name', '')
        status = (fields.get('status') or {}).get('name', '')
        duedate = fields.get('duedate') or ''
        normalized = normalize_status(status)
        entry = f"- **{key}** [{itype}] {display_name[:60]}" + (f" (기한: {duedate})" if duedate else "")
        fe = key in is_frontend
        if normalized == 'done':
            (done_frontend if fe else done_backend).append(entry)
        elif normalized == 'in_progress':
            (in_progress_frontend if fe else in_progress_backend).append(entry)
        else:
            (to_do_frontend if fe else to_do_backend).append(entry)
        if '에픽' in itype or 'Epic' in itype:
            epic_entry = f"- **{key}** {display_name[:50]}" + (f" ~ {duedate}" if duedate else "")
            (epic_frontend if fe else epic_backend).append(epic_entry)

    total = len(issues)
    done_count = len(done_backend) + len(done_frontend)
    in_progress_count = len(in_progress_backend) + len(in_progress_frontend)
    to_do_count = len(to_do_backend) + len(to_do_frontend)
    progress_pct = round(100 * done_count / total, 1) if total else 0
    bar_len = 20
    filled = int(bar_len * done_count / total) if total else 0
    bar = "█" * filled + "░" * (bar_len - filled)

    lines = [
        f"# 프로젝트 진행 상황 보고서",
        f"",
        f"**프로젝트**: {project_name}  ",
        f"**보고일**: {report_date}  ",
        f"**작업물 확인**: [{report_web_url}]({report_web_url})",
        f"",
        f"---",
        f"",
        f"## 진행도 요약",
        f"",
        f"| 구분 | 건수 |",
        f"|------|------|",
        f"| 전체 이슈 | {total} |",
        f"| 완료 | {done_count} |",
        f"| 진행 중 | {in_progress_count} |",
        f"| 남은 작업 | {to_do_count} |",
        f"",
    ]
    if is_frontend:
        lines.extend([
            f"| 백엔드 | {len(done_backend) + len(in_progress_backend) + len(to_do_backend)} |",
            f"| 프론트엔드 | {len(done_frontend) + len(in_progress_frontend) + len(to_do_frontend)} |",
            f"",
        ])
    lines.extend([
        f"**진행률**: `{bar}` **{progress_pct}%**",
        f"",
        f"---",
        f"",
        f"## 전체 일정 (Epic 타임라인)",
        f"",
    ])
    if is_frontend and (epic_backend or epic_frontend):
        lines.append(f"### 백엔드")
        lines.append(f"")
        lines.extend(epic_backend if epic_backend else ["- (없음)"])
        lines.append(f"")
        lines.append(f"### 프론트엔드")
        lines.append(f"")
        lines.extend(epic_frontend if epic_frontend else ["- (없음)"])
    else:
        all_epic = epic_backend + epic_frontend
        lines.extend(all_epic if all_epic else ["- (Epic 없음)"])
    lines.extend([
        f"",
        f"---",
        f"",
        f"## 백엔드",
        f"",
        f"### 완료된 작업",
        f"",
    ])
    lines.extend(done_backend if done_backend else ["- 없음"])
    lines.extend([f"", f"### 진행 중인 작업", f""])
    lines.extend(in_progress_backend if in_progress_backend else ["- 없음"])
    lines.extend([f"", f"### 남은 작업", f""])
    lines.extend(to_do_backend if to_do_backend else ["- 없음"])
    lines.extend([
        f"",
        f"---",
        f"",
        f"## 프론트엔드",
        f"",
        f"### 완료된 작업",
        f"",
    ])
    lines.extend(done_frontend if done_frontend else ["- 없음"])
    lines.extend([f"", f"### 진행 중인 작업", f""])
    lines.extend(in_progress_frontend if in_progress_frontend else ["- 없음"])
    lines.extend([f"", f"### 남은 작업", f""])
    lines.extend(to_do_frontend if to_do_frontend else ["- 없음"])
    lines.extend([
        f"",
        f"---",
        f"",
        f"**작업물 링크**: [Go Almond]({report_web_url})",
        f"",
    ])
    return "\n".join(lines)


def load_jira_env_from_file(path: str) -> None:
    """docs/jira/jira.env 등 KEY=value 형식 파일을 읽어 os.environ에 설정."""
    if not path or not os.path.exists(path):
        return
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                key, _, value = line.partition('=')
                key, value = key.strip(), value.strip()
                if value.startswith('"') and value.endswith('"'):
                    value = value[1:-1].replace('\\"', '"')
                elif value.startswith("'") and value.endswith("'"):
                    value = value[1:-1].replace("\\'", "'")
                if key:
                    os.environ[key] = value


def main():
    # 환경 변수가 없으면 docs/jira/jira.env 참조 (docs는 push 제외)
    if not os.getenv('JIRA_URL'):
        for p in ('docs/jira/jira.env', 'jira.env'):
            load_jira_env_from_file(p)
            if os.getenv('JIRA_URL'):
                break
    parser = argparse.ArgumentParser(description='JIRA 진행 상황 보고서 생성')
    parser.add_argument('--jira-url', default=os.getenv('JIRA_URL'), help='JIRA URL')
    parser.add_argument('--jira-email', default=os.getenv('JIRA_EMAIL'), help='JIRA 이메일')
    parser.add_argument('--jira-api-token', default=os.getenv('JIRA_API_TOKEN'), help='JIRA API 토큰')
    parser.add_argument('--project-key', default='GAM', help='프로젝트 키')
    parser.add_argument('--report-web-url', default='https://go-almond.ddnsfree.com/', help='작업물 확인 URL')
    parser.add_argument('--output', '-o', default='', help='저장할 마크다운 파일 경로 (없으면 stdout)')
    parser.add_argument('--date', default='', help='보고일 (YYYY-MM-DD, 기본: 오늘)')
    parser.add_argument('--canonical-only', action='store_true',
                        help='매핑/백로그에 있는 정규 이슈만 포함 (중복·고아 이슈 제외)')
    parser.add_argument('--mapping-file', default='.github/jira-mapping.json', help='정규 이슈 목록용 매핑 파일')
    parser.add_argument('--backend-backlog', default='docs/jira/JIRA_BACKLOG.md', help='백엔드 백로그 (정규 키 추출용)')
    parser.add_argument('--frontend-backlog', default='docs/jira/FRONT_JIRA_BACKLOG.md', help='프론트엔드 백로그 (표시 제목 추출용)')
    parser.add_argument('--config', default='.github/jira-config.json', help='공통 설정 파일 (로컬/커밋/CI 보고서 규칙 통일)')
    args = parser.parse_args()

    # 공통 규칙: config 파일이 있으면 보고서 옵션 통일 (로컬/커밋/CI 동일 규칙)
    config_path = Path(args.config)
    if config_path.is_file():
        try:
            cfg = json.loads(config_path.read_text(encoding='utf-8'))
            args.project_key = cfg.get('projectKey') or args.project_key
            args.report_web_url = cfg.get('reportWebUrl') or args.report_web_url
            args.mapping_file = cfg.get('mappingFile') or args.mapping_file
            args.backend_backlog = cfg.get('backlogDocument') or args.backend_backlog
            args.frontend_backlog = cfg.get('frontendBacklog') or args.frontend_backlog
        except Exception:
            pass

    jira_url = (args.jira_url or '').rstrip('/')
    if not jira_url or not args.jira_email or not args.jira_api_token:
        print("JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN이 필요합니다.", file=sys.stderr)
        sys.exit(1)

    report_date = args.date or datetime.now().strftime('%Y-%m-%d')
    auth = base64.b64encode(f"{args.jira_email}:{args.jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json"
    }

    print("JIRA 이슈 조회 중...", file=sys.stderr)
    issues = fetch_all_issues(jira_url, headers, args.project_key)
    print(f"조회 완료: {len(issues)}개 이슈", file=sys.stderr)

    if args.canonical_only:
        canonical_keys = load_canonical_keys(args.mapping_file, args.backend_backlog)
        before = len(issues)
        issues = [i for i in issues if (i.get('key') or '') in canonical_keys]
        print(f"정규 이슈만 포함: {before}개 → {len(issues)}개 (중복·고아 제외)", file=sys.stderr)

    display_titles = load_display_titles_from_backlogs(
        args.backend_backlog,
        args.frontend_backlog,
        args.mapping_file,
    )
    if display_titles:
        print(f"백로그 표시 제목 로드: {len(display_titles)}개", file=sys.stderr)

    frontend_keys = load_frontend_jira_keys(args.mapping_file)
    if frontend_keys:
        print(f"백엔드/프론트 구분: 프론트엔드 {len(frontend_keys)}개", file=sys.stderr)

    markdown = build_report_markdown(
        issues,
        report_date=report_date,
        report_web_url=args.report_web_url,
        display_titles=display_titles,
        frontend_keys=frontend_keys,
    )

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(markdown, encoding='utf-8')
        print(f"보고서 저장: {args.output}", file=sys.stderr)
    else:
        print(markdown)


if __name__ == '__main__':
    main()
