#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Report(report-latest.md) 완료 목록과 코드 검증 결과(code-completion-verification.json),
JIRA 상태를 교차 검증하여 불일치를 명확히 보고.
"""
import base64
import json
import os
import re
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Set

import requests

PROJECT_ROOT = Path(__file__).resolve().parents[2]
REPORT_FILE = PROJECT_ROOT / "reports" / "report-latest.md"
VERIFICATION_FILE = PROJECT_ROOT / ".github" / "code-completion-verification.json"
OUTPUT_JSON = PROJECT_ROOT / ".github" / "comprehensive-verification.json"
OUTPUT_MD = PROJECT_ROOT / "reports" / "comprehensive-verification.md"

# 백엔드 이슈로 간주할 키 범위 (Report 백엔드 섹션에만 등장하는 번호대)
BACKEND_KEY_PATTERN = re.compile(r"GAM-(?:[1-9]|[1-6]\d|7[0-1])\b")


def load_jira_env(paths: list) -> None:
    for p in paths:
        path = PROJECT_ROOT / p if not os.path.isabs(p) else Path(p)
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    if "=" in line and not line.startswith("#"):
                        k, v = line.strip().split("=", 1)
                        os.environ[k.strip()] = v.strip().strip("'\"")


def parse_report_backend_sections(path: Path) -> tuple[List[Dict], List[Dict]]:
    """
    Report에서 백엔드 '완료된 작업' / '남은 작업' 항목 추출.
    Returns: (done_list, todo_list), 각 항목은 {"key": "GAM-XX", "title": "..."}
    """
    if not path.exists():
        return [], []
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    done_list: List[Dict] = []
    todo_list: List[Dict] = []
    # Report에 "## 백엔드"가 두 번 나옴. 백엔드 섹션 내 "### 완료된 작업" / "### 남은 작업" 블록만 사용
    backend_done = re.search(
        r"## 백엔드\s+\n### 완료된 작업\s*\n+(.*?)(?=\n### 진행 중인 작업|\n### 남은 작업)",
        content,
        re.DOTALL,
    )
    backend_todo = re.search(
        r"## 백엔드\s+.*?\n### 남은 작업\s*\n+(.*?)(?=\n## 프론트엔드|\Z)",
        content,
        re.DOTALL,
    )
    done_text = backend_done.group(1) if backend_done else ""
    todo_text = backend_todo.group(1) if backend_todo else ""

    item_re = re.compile(r"-\s*\*\*(GAM-\d+)\*\*\s*\[[^\]]+\]\s*(.+)", re.MULTILINE)
    for m in item_re.finditer(done_text):
        key = m.group(1)
        title = re.sub(r"\s*\(기한:[^)]+\)\s*$", "", m.group(2)).strip()
        if BACKEND_KEY_PATTERN.search(key):
            done_list.append({"key": key, "title": title})

    for m in item_re.finditer(todo_text):
        key = m.group(1)
        title = re.sub(r"\s*\(기한:[^)]+\)\s*$", "", m.group(2)).strip()
        if BACKEND_KEY_PATTERN.search(key):
            todo_list.append({"key": key, "title": title})

    return done_list, todo_list


def get_issue_status(jira_url: str, headers: dict, issue_key: str) -> str | None:
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}?fields=status",
            headers=headers,
            timeout=10,
        )
        if r.status_code != 200:
            return None
        return r.json().get("fields", {}).get("status", {}).get("name")
    except Exception:
        return None


def is_done_status(status_name: str | None) -> bool:
    if not status_name:
        return False
    done_names = ["done", "완료", "complete", "closed", "종료", "resolved", "해결됨"]
    return (status_name or "").strip().lower() in [d.lower() for d in done_names]


def main() -> None:
    load_jira_env(["docs/jira/jira.env", "jira.env"])

    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""
    use_jira = bool(jira_url and jira_email and jira_token)
    if use_jira:
        auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
        headers = {
            "Authorization": f"Basic {auth}",
            "Content-Type": "application/json",
        }
    else:
        headers = {}

    if not REPORT_FILE.exists():
        print(f"오류: {REPORT_FILE} 없음", file=sys.stderr)
        sys.exit(1)
    if not VERIFICATION_FILE.exists():
        print(
            f"오류: {VERIFICATION_FILE} 없음. 먼저 jira-verify-code-completion.py 실행",
            file=sys.stderr,
        )
        sys.exit(1)

    report_done, report_todo = parse_report_backend_sections(REPORT_FILE)
    report_done_keys: Set[str] = {x["key"] for x in report_done}
    report_todo_keys: Set[str] = {x["key"] for x in report_todo}
    report_done_by_key: Dict[str, Dict] = {x["key"]: x for x in report_done}

    with open(VERIFICATION_FILE, "r", encoding="utf-8") as f:
        verification = json.load(f)

    story_details: Dict[str, Dict] = verification.get("story_task_details", {})
    stories_completed_keys: Set[str] = {
        s["key"] for s in verification.get("stories_completed", [])
    }
    tasks_completed_keys: Set[str] = {
        t["key"] for t in verification.get("tasks_completed", verification.get("completed", []))
    }

    def code_verified(key: str) -> bool:
        if key in stories_completed_keys:
            return True
        if key in story_details and story_details[key].get("code_verified"):
            return True
        if key in tasks_completed_keys:
            return True
        return False

    # 1) Report 완료인데 코드 미검증
    report_done_but_no_code: List[Dict] = []
    for item in report_done:
        key = item["key"]
        if not code_verified(key):
            entry = {"key": key, "title": item["title"], "code_verified": False}
            if use_jira:
                time.sleep(0.2)
                status = get_issue_status(jira_url, headers, key)
                entry["jira_status"] = status or "(조회실패)"
            report_done_but_no_code.append(entry)

    # 2) 코드 검증 완료인데 Report에는 남은 작업
    code_done_but_report_todo: List[Dict] = []
    for key in report_todo_keys:
        if code_verified(key):
            title = next((x["title"] for x in report_todo if x["key"] == key), "")
            code_done_but_report_todo.append({"key": key, "title": title})

    # 3) JIRA Done인데 코드 없음 (Report 완료 목록 기준으로 JIRA 조회)
    jira_done_but_no_code: List[Dict] = []
    if use_jira:
        for item in report_done_but_no_code:
            key = item["key"]
            status = item.get("jira_status")
            if status and is_done_status(status):
                jira_done_but_no_code.append(
                    {"key": key, "title": item["title"], "jira_status": status}
                )

    # 4) 백로그 vs Report 요약
    backlog_stories = list(story_details.keys()) if story_details else []
    report_done_stories = [k for k in report_done_keys if k in backlog_stories or k in story_details]
    missing_in_report = [k for k in backlog_stories if k not in report_done_keys and k not in report_todo_keys]

    result: Dict[str, Any] = {
        "report_vs_code": {
            "report_done_but_no_code": report_done_but_no_code,
            "code_done_but_report_todo": code_done_but_report_todo,
        },
        "jira_vs_code": {
            "jira_done_but_no_code": jira_done_but_no_code,
        },
        "backlog_vs_report": {
            "backlog_stories_count": len(backlog_stories),
            "report_done_count": len(report_done_keys),
            "report_todo_count": len(report_todo_keys),
            "missing_in_report": missing_in_report[:30],
        },
        "story_task_mapping": {
            k: {
                "title": v.get("title", ""),
                "tasks": v.get("tasks", []),
                "completion_rate": v.get("completion_rate", 0),
                "code_verified": v.get("code_verified", False),
            }
            for k, v in list(story_details.items())[:30]
        },
    }

    OUTPUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    # Markdown 보고서 생성
    OUTPUT_MD.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write("# 종합 불일치 검증 보고서\n\n")
        f.write("Report vs 코드 검증 결과, JIRA 상태를 교차 검증한 결과입니다.\n\n")
        f.write("---\n\n")
        f.write("## 1. Report 완료인데 코드 미검증 (Report 과대 평가)\n\n")
        if report_done_but_no_code:
            f.write("| 키 | 제목 | JIRA 상태 |\n|----|------|------------|\n")
            for x in report_done_but_no_code:
                jira = x.get("jira_status", "-")
                f.write(f"| {x['key']} | {x['title'][:50]} | {jira} |\n")
            f.write(f"\n**권장**: JIRA에서 해당 이슈를 To Do로 되돌리거나, 코드 구현 후 재검증.\n\n")
        else:
            f.write("없음.\n\n")
        f.write("## 2. 코드 검증 완료인데 Report 남은 작업 (Report 갱신 필요)\n\n")
        if code_done_but_report_todo:
            f.write("| 키 | 제목 |\n|----|------|\n")
            for x in code_done_but_report_todo:
                f.write(f"| {x['key']} | {x['title'][:50]} |\n")
            f.write("\n**권장**: `./reports/jira-report-local.sh` 실행 후 Report 갱신.\n\n")
        else:
            f.write("없음.\n\n")
        f.write("## 3. JIRA Done인데 코드 없음 (JIRA 오표시)\n\n")
        if jira_done_but_no_code:
            f.write("| 키 | 제목 | JIRA 상태 |\n|----|------|------------|\n")
            for x in jira_done_but_no_code:
                f.write(f"| {x['key']} | {x['title'][:50]} | {x.get('jira_status', '-')} |\n")
            f.write("\n**권장**: JIRA에서 해당 이슈를 To Do로 되돌리기.\n\n")
        else:
            f.write("없음.\n\n")
        f.write("## 4. 백로그 vs Report 요약\n\n")
        f.write(f"- 백로그 Story 수: {result['backlog_vs_report']['backlog_stories_count']}\n")
        f.write(f"- Report 완료 수: {result['backlog_vs_report']['report_done_count']}\n")
        f.write(f"- Report 남은 작업 수: {result['backlog_vs_report']['report_todo_count']}\n")
        missing = result["backlog_vs_report"].get("missing_in_report", [])
        if missing:
            f.write(f"- Report에 없는 백로그 Story: {', '.join(missing[:15])}\n")

    print("종합 검증 완료:")
    print(f"  Report 완료인데 코드 미검증: {len(report_done_but_no_code)}개")
    print(f"  코드 완료인데 Report 남은 작업: {len(code_done_but_report_todo)}개")
    print(f"  JIRA Done인데 코드 없음: {len(jira_done_but_no_code)}개")
    print(f"\n결과 저장: {OUTPUT_JSON}")
    print(f"보고서: {OUTPUT_MD}")
    if report_done_but_no_code:
        print("\n[Report 과대 평가]")
        for x in report_done_but_no_code[:10]:
            print(f"  {x['key']}: {x['title'][:50]}")


if __name__ == "__main__":
    main()
