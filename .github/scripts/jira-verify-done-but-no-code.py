#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA에서 완료 처리된 백엔드 이슈를 코드 기준으로 재검증.
코드 증거가 없는 완료 항목을 식별하여 보고서 생성.
"""
import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
JIRA_ISSUES_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
MAPPING_FILE = PROJECT_ROOT / ".github" / "jira-to-backlog-mapping.json"
CODE_VERIFICATION_FILE = PROJECT_ROOT / ".github" / "code-completion-verification.json"
OUTPUT_JSON = PROJECT_ROOT / ".github" / "jira-done-but-no-code.json"
OUTPUT_REPORT = PROJECT_ROOT / "reports" / "jira-done-but-no-code.md"

DONE_STATUSES = frozenset({"완료", "done", "complete", "closed", "resolved", "해결됨", "종료"})


def is_done_status(status: str) -> bool:
    if not status:
        return False
    return status.strip().lower() in {s.lower() for s in DONE_STATUSES}


def main() -> None:
    if not JIRA_ISSUES_FILE.exists():
        print(f"오류: {JIRA_ISSUES_FILE} 없음", file=sys.stderr)
        sys.exit(1)
    if not CODE_VERIFICATION_FILE.exists():
        print(f"오류: {CODE_VERIFICATION_FILE} 없음. 먼저 jira-verify-code-completion.py 실행", file=sys.stderr)
        sys.exit(1)

    with open(JIRA_ISSUES_FILE, "r", encoding="utf-8") as f:
        raw = f.read()
    try:
        jira_issues = json.loads(raw)
    except json.JSONDecodeError:
        print("오류: jira-backend-issues.json JSON 파싱 실패", file=sys.stderr)
        sys.exit(1)
    if not isinstance(jira_issues, list):
        jira_issues = list(jira_issues.values()) if isinstance(jira_issues, dict) else []

    mapping = {}
    if MAPPING_FILE.exists():
        with open(MAPPING_FILE, "r", encoding="utf-8") as f:
            mapping = json.load(f)

    with open(CODE_VERIFICATION_FILE, "r", encoding="utf-8") as f:
        code_verification = json.load(f)
    tasks_completed_keys = {t["key"] for t in code_verification.get("tasks_completed", [])}
    stories_completed_keys = {s["key"] for s in code_verification.get("stories_completed", [])}
    code_verified_keys = tasks_completed_keys | stories_completed_keys

    done_issues = [i for i in jira_issues if is_done_status(i.get("status") or "")]
    done_but_no_code: list = []

    for issue in done_issues:
        jira_key = issue.get("key")
        summary = issue.get("summary") or ""
        jira_type = issue.get("type") or ""
        backlog_key = None
        if isinstance(mapping.get(jira_key), dict):
            backlog_key = mapping[jira_key].get("backlog_key")
        code_verified = (backlog_key and backlog_key in code_verified_keys) or (jira_key in code_verified_keys)
        if not code_verified:
            done_but_no_code.append({
                "jira_key": jira_key,
                "summary": summary,
                "type": jira_type,
                "backlog_key": backlog_key,
                "reason": "no_backlog_mapping" if not backlog_key else "code_not_verified",
            })

    OUTPUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(
            {"done_but_no_code": done_but_no_code, "total_done": len(done_issues), "total_issues": len(jira_issues)},
            f,
            indent=2,
            ensure_ascii=False,
        )

    OUTPUT_REPORT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_REPORT, "w", encoding="utf-8") as f:
        f.write("# JIRA 완료 처리 vs 코드 검증 보고서\n\n")
        f.write("JIRA에서 **완료**로 되어 있으나, 코드 기준으로 검증되지 않은 백엔드 이슈 목록입니다.\n\n")
        f.write("---\n\n")
        f.write(f"**완료 처리된 이슈**: {len(done_issues)}개\n")
        f.write(f"**미작업 완료 의심**: {len(done_but_no_code)}개\n\n")
        f.write("## 미작업 완료 처리 목록 (코드 미검증)\n\n")
        f.write("| JIRA 키 | 유형 | 제목 | 백로그 키 | 사유 |\n")
        f.write("|---------|------|------|----------|------|\n")
        for item in done_but_no_code:
            key = item["jira_key"]
            typ = item["type"]
            title = (item["summary"] or "")[:50]
            bl = item.get("backlog_key") or "-"
            reason = "매핑 없음" if item["reason"] == "no_backlog_mapping" else "코드 미검증"
            f.write(f"| {key} | {typ} | {title} | {bl} | {reason} |\n")
        f.write("\n---\n\n")
        f.write("**기준**: `jira-verify-code-completion.py`의 Task/Story 코드 존재 여부\n")
        f.write("**데이터**: `.github/jira-backend-issues.json`, `.github/jira-to-backlog-mapping.json`\n")

    print("미작업 완료 검증 완료:")
    print(f"  JIRA 완료 이슈: {len(done_issues)}개")
    print(f"  코드 미검증 완료: {len(done_but_no_code)}개")
    print(f"\n결과 저장:")
    print(f"  JSON: {OUTPUT_JSON}")
    print(f"  보고서: {OUTPUT_REPORT}")


if __name__ == "__main__":
    main()
