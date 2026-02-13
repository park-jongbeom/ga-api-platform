#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
로컬 commit 시, JIRA 일정 작업과 연관된 경우에만
최신 보고서·JIRA_BACKLOG·실제 JIRA 상태를 확인하고, 필요 시에만 보고서를 생성하는 스크립트.
"""
import argparse
import base64
import json
import os
import re
import shutil
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Optional, Set

import requests


# 정규 키·상태 판별용 (jira-generate-report.py와 동일 로직)
DONE_STATUSES = {'done', '완료', 'complete', 'closed', '종료', 'resolved', '해결됨'}


def normalize_status(s: str) -> str:
    t = (s or '').strip().lower()
    if t in DONE_STATUSES or any(d in t for d in ['done', '완료', 'complete', 'closed', '종료']):
        return 'done'
    return 'to_do'  # in_progress도 미완료로 간주


def load_canonical_keys(mapping_file: str, backend_backlog_path: Optional[str] = None) -> Set[str]:
    """정규(참조할) 이슈 키 집합 로드."""
    canonical = set()
    if os.path.exists(mapping_file):
        with open(mapping_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        for k, v in data.items():
            if not k.startswith('_') and isinstance(v, str):
                canonical.add(v)
    if backend_backlog_path and os.path.exists(backend_backlog_path):
        content = Path(backend_backlog_path).read_text(encoding='utf-8')
        for m in re.finditer(r'\b(GAM-\d+(?:-\d+)?)\b', content):
            canonical.add(m.group(1))
    return canonical


def extract_issue_keys_from_message(message: str, pattern: str = r'GAM-\d+(?:-\d+)?') -> Set[str]:
    """커밋 메시지에서 JIRA 이슈 키 추출 (서브태스크 포함)."""
    if not message:
        return set()
    return set(re.findall(pattern, message, re.IGNORECASE))


def parse_done_keys_from_report(report_path: str) -> Set[str]:
    """보고서의 '## 완료된 작업' 섹션에서 GAM-xxx 키 목록 파싱."""
    path = Path(report_path)
    if not path.exists():
        return set()
    text = path.read_text(encoding='utf-8')
    keys = set()
    in_done = False
    for line in text.splitlines():
        if line.strip() == '## 완료된 작업':
            in_done = True
            continue
        if in_done:
            if line.strip().startswith('## '):
                break
            for m in re.finditer(r'\b(GAM-\d+(?:-\d+)?)\b', line):
                keys.add(m.group(1))
    return keys


def get_jira_status(jira_url: str, headers: dict, issue_key: str) -> Optional[str]:
    """이슈 한 건의 status.name 조회. 실패 시 None."""
    try:
        url = f"{jira_url}/rest/api/3/issue/{issue_key}"
        r = requests.get(url, headers=headers, params={"fields": "status"}, timeout=15)
        if r.status_code != 200:
            return None
        data = r.json()
        name = (data.get('fields') or {}).get('status') or {}
        return (name.get('name') or '').strip()
    except Exception:
        return None


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
    parser = argparse.ArgumentParser(
        description='로컬 commit 시, JIRA 일정 관련 작업인 경우에만 조건을 확인하고 필요 시 보고서 생성'
    )
    parser.add_argument('--ref', default='HEAD', help='커밋 참조 (기본: HEAD)')
    parser.add_argument('--commit', action='store_true', help='보고서 생성 후 git commit 까지 수행')
    parser.add_argument('--mapping-file', default='.github/jira-mapping.json')
    parser.add_argument('--backend-backlog', default='docs/jira/JIRA_BACKLOG.md')
    parser.add_argument('--report-latest', default='reports/report-latest.md')
    parser.add_argument('--reports-dir', default='reports')
    args = parser.parse_args()

    # 스크립트가 reports/ 폴더에 있음
    project_root = Path(__file__).resolve().parent.parent
    os.chdir(project_root)

    # 보고서 경로: .github/jira-config.json 의 reportsDir 사용 (로컬/CI와 동일)
    reports_dir = project_root / args.reports_dir
    report_latest_path = project_root / args.report_latest
    config_path = project_root / ".github" / "jira-config.json"
    if config_path.is_file():
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                cfg = json.load(f)
            rd = cfg.get('reportsDir') or args.reports_dir
            reports_dir = project_root / rd
            report_latest_path = reports_dir / "report-latest.md"
        except Exception:
            pass

    # 환경 변수가 없으면 docs/jira/jira.env 참조 (docs는 push 제외)
    if not os.getenv('JIRA_URL'):
        for p in ('docs/jira/jira.env', 'jira.env'):
            load_jira_env_from_file(str(project_root / p))
            if os.getenv('JIRA_URL'):
                break

    jira_url = (os.getenv('JIRA_URL') or '').rstrip('/')
    jira_email = os.getenv('JIRA_EMAIL', '')
    jira_api_token = os.getenv('JIRA_API_TOKEN', '')
    if not jira_url or not jira_email or not jira_api_token:
        print("JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 환경 변수가 필요합니다.", file=sys.stderr)
        print("env.jira.example 을 복사해 docs/jira/jira.env 에 값을 채우거나 export 하세요.", file=sys.stderr)
        sys.exit(1)

    # 1) 커밋 메시지에서 이슈 키 추출
    try:
        out = subprocess.run(
            ['git', 'log', '-1', '--pretty=%B', args.ref],
            capture_output=True, text=True, check=True, timeout=5
        )
        commit_message = (out.stdout or '').strip()
    except Exception:
        commit_message = ''
    keys_in_commit = extract_issue_keys_from_message(commit_message)
    if not keys_in_commit:
        print("커밋 메시지에 JIRA 이슈 키(GAM-xxx)가 없습니다. 보고서 미생성.", file=sys.stderr)
        sys.exit(0)

    # 2) 일정 관련 필터: 정규 목록에 있는 키만
    mapping = project_root / args.mapping_file
    backlog = project_root / args.backend_backlog
    canonical = load_canonical_keys(str(mapping), str(backlog))
    schedule_keys = keys_in_commit & canonical
    if not schedule_keys:
        print("일정과 관련된 작업이 아닙니다(JIRA_BACKLOG/정규 목록에 없음). 보고서 미생성.", file=sys.stderr)
        sys.exit(0)

    # 3) 최신 보고서에 이미 완료로 기록되었는지
    done_in_report = parse_done_keys_from_report(str(report_latest_path))
    if done_in_report and schedule_keys <= done_in_report:
        print("최신 보고서에 이미 완료로 기록된 작업입니다. 보고서 미생성.", file=sys.stderr)
        sys.exit(0)

    # 4) JIRA에서 이미 완료인지
    auth = base64.b64encode(f"{jira_email}:{jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Accept": "application/json",
    }
    all_done_in_jira = True
    for key in schedule_keys:
        status_name = get_jira_status(jira_url, headers, key)
        if status_name is None or normalize_status(status_name) != 'done':
            all_done_in_jira = False
            break
        time.sleep(0.2)
    if all_done_in_jira:
        print("실제 JIRA에 이미 완료로 처리된 작업 일정입니다. 보고서 미생성.", file=sys.stderr)
        sys.exit(0)

    # 5) 보고서 생성 (옵션은 .github/jira-config.json 에서 로드, 로컬/CI와 동일 규칙)
    report_date = datetime.now().strftime('%Y-%m-%d')
    reports_dir.mkdir(parents=True, exist_ok=True)
    report_file = reports_dir / f"report-{report_date}.md"
    latest_file = reports_dir / "report-latest.md"

    script_dir = Path(__file__).resolve().parent
    generate_script = script_dir / "jira-generate-report.py"
    env = os.environ.copy()
    env["JIRA_URL"] = jira_url
    env["JIRA_EMAIL"] = jira_email
    env["JIRA_API_TOKEN"] = jira_api_token
    cmd = [
        sys.executable,
        str(generate_script),
        "--config", str(project_root / ".github" / "jira-config.json"),
        # --canonical-only 제거하여 프론트엔드 포함
        "--output", str(report_file),
        "--date", report_date,
    ]
    try:
        subprocess.run(cmd, env=env, check=True, timeout=120, cwd=str(project_root))
    except subprocess.CalledProcessError as e:
        print(f"보고서 생성 실패: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"보고서 생성 오류: {e}", file=sys.stderr)
        sys.exit(1)

    # report-latest 복사
    shutil.copy(str(report_file), str(latest_file))
    print(f"보고서 생성: {report_file}, {latest_file}", file=sys.stderr)

    # git add
    subprocess.run(
        ['git', 'add', str(report_file), str(latest_file)],
        check=True, cwd=str(project_root), timeout=5
    )
    if args.commit:
        subprocess.run(
            ['git', 'commit', '-m', f"docs: JIRA 진행 보고서 {report_date}"],
            check=True, cwd=str(project_root), timeout=5
        )
        print("커밋 완료. 다음 명령으로 push 하세요: git push", file=sys.stderr)
    else:
        print("", file=sys.stderr)
        print("보고서 생성됨. 다음 명령으로 커밋 후 push 하세요:", file=sys.stderr)
        print(f"  git commit -m \"docs: JIRA 진행 보고서 {report_date}\"", file=sys.stderr)
        print("  git push", file=sys.stderr)


if __name__ == '__main__':
    main()
