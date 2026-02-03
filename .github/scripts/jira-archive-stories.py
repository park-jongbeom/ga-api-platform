#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 백엔드에서 Story 타입 이슈를 보관 처리(안 보이게).

- 기본: 라벨 'legacy-story' 추가 + 상태 '완료'로 전환 → 보드/백로그에서 제외 가능
- 삭제가 불가한 프로젝트에서는 이 보관 방식 사용 권장
- --delete: 삭제 시도 (실패 시 해당 이슈만 보관으로 처리)
"""
import argparse
import base64
import json
import os
import sys
import time
from pathlib import Path

import requests

PROJECT_ROOT = Path(__file__).resolve().parents[2]
JIRA_ISSUES_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
REPORT_FILE = PROJECT_ROOT / "reports" / "jira-archive-stories-report.md"

ARCHIVE_LABEL = "legacy-story"
TYPE_STORY = "스토리"
DONE_NAMES = ("완료", "done", "complete", "closed", "resolved", "해결됨", "종료")


def load_jira_env(paths: list) -> None:
    for p in paths:
        path = PROJECT_ROOT / p if p and not os.path.isabs(p) else Path(p or "")
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith("#") or "=" not in line:
                        continue
                    k, _, v = line.partition("=")
                    if k.strip():
                        os.environ[k.strip()] = v.strip().strip("'\"")


def is_done_status(status: str) -> bool:
    return (status or "").strip().lower() in {s.lower() for s in DONE_NAMES}


def get_transitions(jira_url: str, headers: dict, issue_key: str) -> list:
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
            headers=headers,
            timeout=10,
        )
        return r.json().get("transitions", []) if r.status_code == 200 else []
    except Exception:
        return []


def transition_to_done(jira_url: str, headers: dict, issue_key: str) -> bool:
    transitions = get_transitions(jira_url, headers, issue_key)
    tid = None
    for t in transitions:
        to_name = (t.get("to", {}) or {}).get("name") or ""
        if to_name.strip().lower() in {s.lower() for s in DONE_NAMES}:
            tid = t.get("id")
            break
    if not tid and transitions:
        tid = transitions[0].get("id")
    if not tid:
        return False
    r = requests.post(
        f"{jira_url}/rest/api/3/issue/{issue_key}/transitions",
        headers=headers,
        json={"transition": {"id": tid}},
        timeout=10,
    )
    return r.status_code == 204


def add_label(jira_url: str, headers: dict, issue_key: str, label: str) -> bool:
    try:
        r = requests.put(
            f"{jira_url}/rest/api/3/issue/{issue_key}",
            headers=headers,
            json={"update": {"labels": [{"add": label}]}},
            timeout=10,
        )
        return r.status_code == 204
    except Exception as e:
        print(f"  라벨 추가 오류 {issue_key}: {e}")
        return False


def delete_issue(jira_url: str, headers: dict, issue_key: str) -> tuple[bool, str]:
    """삭제 시도. (성공 여부, 실패 시 메시지)"""
    try:
        r = requests.delete(
            f"{jira_url}/rest/api/3/issue/{issue_key}",
            headers=headers,
            params={"deleteSubtasks": "false"},
            timeout=10,
        )
        if r.status_code == 204:
            return True, ""
        return False, f"{r.status_code} {r.text[:200]}"
    except Exception as e:
        return False, str(e)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Story 타입 이슈 보관(라벨+완료) 또는 삭제 시도"
    )
    parser.add_argument("--dry-run", action="store_true", help="API 호출 없이 대상만 출력")
    parser.add_argument(
        "--delete",
        action="store_true",
        help="삭제 시도 (실패 시 해당 이슈는 보관 처리)",
    )
    args = parser.parse_args()

    load_jira_env(["docs/jira/jira.env", "jira.env"])

    if not JIRA_ISSUES_FILE.exists():
        print(f"오류: {JIRA_ISSUES_FILE} 없음", file=sys.stderr)
        sys.exit(1)

    with open(JIRA_ISSUES_FILE, "r", encoding="utf-8") as f:
        raw = json.load(f)
    issues = raw if isinstance(raw, list) else list(raw.values()) if isinstance(raw, dict) else []
    stories = [i for i in issues if (i.get("type") or "").strip() == TYPE_STORY]

    print(f"Story 이슈: {len(stories)}개")
    for s in stories[:20]:
        print(f"  {s['key']} | {s.get('summary', '')[:45]} | {s.get('status', '')}")
    if len(stories) > 20:
        print(f"  ... 외 {len(stories) - 20}개")

    if args.dry_run:
        print("\n[DRY RUN] API 호출 없이 종료")
        REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
        with open(REPORT_FILE, "w", encoding="utf-8") as f:
            f.write("# Story 보관 대상 (Dry Run)\n\n")
            f.write(f"대상: {len(stories)}개\n\n")
            f.write("| 키 | 요약 | 상태 |\n")
            f.write("|----|------|------|\n")
            for s in stories:
                f.write(f"| {s['key']} | {(s.get('summary') or '')[:50]} | {s.get('status', '')} |\n")
        print(f"보고서: {REPORT_FILE}")
        return

    if not stories:
        print("보관할 Story 없음. 종료.")
        return

    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""
    if not all([jira_url, jira_email, jira_token]):
        print("오류: JIRA 인증 정보 필요 (JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN)", file=sys.stderr)
        sys.exit(1)

    auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    deleted, archived, failed = [], [], []
    for s in stories:
        key = s["key"]
        summary = (s.get("summary") or "")[:50]
        status = s.get("status") or ""

        if args.delete:
            ok, err = delete_issue(jira_url, headers, key)
            if ok:
                print(f"  ✓ 삭제 {key}")
                deleted.append({"key": key, "summary": summary})
                time.sleep(0.35)
                continue
            print(f"  ⊘ 삭제 불가 {key} → 보관 처리 ({err[:60]})")

        # 보관: 라벨 + 완료 전환
        label_ok = add_label(jira_url, headers, key, ARCHIVE_LABEL)
        time.sleep(0.25)
        if not is_done_status(status):
            trans_ok = transition_to_done(jira_url, headers, key)
            time.sleep(0.35)
        else:
            trans_ok = True

        if label_ok:
            print(f"  ✓ 보관 {key} (라벨:{ARCHIVE_LABEL}, 완료 전환:{trans_ok})")
            archived.append({"key": key, "summary": summary, "transitioned": not is_done_status(status)})
        else:
            print(f"  ✗ 보관 실패 {key}")
            failed.append({"key": key, "summary": summary})

    REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write("# Story 보관/삭제 결과\n\n")
        f.write(f"- 삭제: {len(deleted)}개\n")
        f.write(f"- 보관(라벨+완료): {len(archived)}개\n")
        f.write(f"- 실패: {len(failed)}개\n\n")
        if deleted:
            f.write("## 삭제된 이슈\n\n")
            for x in deleted:
                f.write(f"- {x['key']} {x['summary']}\n")
        if archived:
            f.write("## 보관 처리된 이슈\n\n")
            for x in archived:
                f.write(f"- {x['key']} {x['summary']}\n")
        if failed:
            f.write("## 실패\n\n")
            for x in failed:
                f.write(f"- {x['key']} {x['summary']}\n")
    print(f"\n완료: 삭제 {len(deleted)}개, 보관 {len(archived)}개, 실패 {len(failed)}개")
    print(f"보고서: {REPORT_FILE}")


if __name__ == "__main__":
    main()
