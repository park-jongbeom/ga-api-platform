#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA_BACKLOG.md(또는 JIRA_BACKEND.md) 문서의 완료 항목 [x]을 기준으로
실제 JIRA에서 해당 작업(Task) 및 에픽(Epic) 상태를 완료(Done)로 전환한 뒤,
보고서 생성 스크립트(jira-report-local.sh)에서 이 스크립트를 먼저 실행하고
이어서 jira-generate-report.py로 보고서를 작성한다.

문서 경로: 기본값 docs/jira/JIRA_BACKLOG.md (JIRA_BACKEND.md 없음 → JIRA_BACKLOG.md 사용)
"""
import os
import re
import sys
import subprocess
from pathlib import Path


def load_jira_env(paths):
    for p in paths:
        if not p or not os.path.exists(p):
            continue
        with open(p, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, _, v = line.partition("=")
                k, v = k.strip(), v.strip()
                if k:
                    os.environ[k] = v


def parse_done_keys_from_backlog(backlog_path: str) -> list:
    """백로그 마크다운에서 '- [x] GAM-XXX: ...' 패턴으로 완료된 이슈 키 목록 추출."""
    path = Path(backlog_path)
    if not path.exists():
        return []
    text = path.read_text(encoding="utf-8")
    keys = []
    for m in re.finditer(r"- \[x\] (GAM-\d+):", text):
        keys.append(m.group(1))
    return list(dict.fromkeys(keys))  # 순서 유지, 중복 제거


def main():
    import argparse
    parser = argparse.ArgumentParser(
        description="JIRA_BACKLOG.md 완료 항목 [x]을 JIRA에 반영(작업·에픽 Done) 후 보고서 생성 시 선행 실행용"
    )
    parser.add_argument(
        "--backlog",
        default="docs/jira/JIRA_BACKLOG.md",
        help="백로그 문서 경로 (기본: docs/jira/JIRA_BACKLOG.md, JIRA_BACKEND.md 없음)",
    )
    parser.add_argument("--dry-run", action="store_true", help="JIRA API 호출 없이 전환 대상만 출력")
    parser.add_argument("--skip-epics", action="store_true", help="에픽 완료 전환(jira-transition-epics-done-if-children-done.py) 스킵")
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parent.parent
    os.chdir(project_root)

    load_jira_env(["docs/jira/jira.env", "jira.env"])
    jira_url = os.getenv("JIRA_URL", "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL", "")
    jira_api_token = os.getenv("JIRA_API_TOKEN", "")
    if not jira_url or not jira_email or not jira_api_token:
        print("경고: JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 미설정. 동기화 스킵. 보고서만 JIRA API로 생성됩니다.", file=sys.stderr)
        return 0

    backlog_path = args.backlog
    if not os.path.isabs(backlog_path):
        backlog_path = str(project_root / backlog_path)
    done_keys = parse_done_keys_from_backlog(backlog_path)
    if not done_keys:
        print("백로그에서 완료 [x] 항목을 찾지 못했습니다. 동기화 스킵.", file=sys.stderr)
        if not args.skip_epics:
            scripts_dir = project_root / ".github" / "scripts"
            epics_script = scripts_dir / "jira-transition-epics-done-if-children-done.py"
            if epics_script.exists():
                rc = subprocess.call(
                    [sys.executable, str(epics_script)] + (["--dry-run"] if args.dry_run else []),
                    cwd=str(project_root),
                    env=os.environ,
                )
                if rc != 0:
                    print("에픽 완료 전환 스크립트 종료 코드:", rc, file=sys.stderr)
        return 0

    print(f"백로그 완료 [x] 항목 {len(done_keys)}개 → JIRA 완료 전환 대상")
    if args.dry_run:
        for k in done_keys:
            print(f"  [DRY RUN] {k}")
        if not args.skip_epics:
            scripts_dir = project_root / ".github" / "scripts"
            subprocess.call(
                [sys.executable, str(scripts_dir / "jira-transition-epics-done-if-children-done.py"), "--dry-run"],
                cwd=str(project_root),
                env=os.environ,
            )
        return 0

    scripts_dir = project_root / ".github" / "scripts"
    transition_script = scripts_dir / "jira-transition-issues.py"
    if not transition_script.exists():
        print("오류: jira-transition-issues.py 없음.", file=sys.stderr)
        return 1
    issues_arg = ",".join(done_keys)
    rc = subprocess.call(
        [sys.executable, str(transition_script), "--issues", issues_arg],
        cwd=str(project_root),
        env=os.environ,
    )
    if rc != 0:
        print("경고: 작업 완료 전환 스크립트 종료 코드:", rc, file=sys.stderr)

    if not args.skip_epics:
        epics_script = scripts_dir / "jira-transition-epics-done-if-children-done.py"
        if epics_script.exists():
            rc2 = subprocess.call(
                [sys.executable, str(epics_script)],
                cwd=str(project_root),
                env=os.environ,
            )
            if rc2 != 0:
                print("경고: 에픽 완료 전환 스크립트 종료 코드:", rc2, file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
