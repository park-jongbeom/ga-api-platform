#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
백로그 키 → JIRA 실제 키 매핑 생성.
기존 jira-mapping.json의 _jiraToBacklog 역매핑 + GAM-22-3→GAM-51.
JIRA API로 추가 매핑 시도 (실패 시 파일 기반만 사용).
"""
import os
import sys
import json
import base64
import re
import requests
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
MAPPING_FILE = PROJECT_ROOT / ".github" / "jira-mapping.json"
OUTPUT_FILE = PROJECT_ROOT / ".github" / "backlog-to-jira-mapping.json"
BACKLOG_FILE = PROJECT_ROOT / "docs" / "jira" / "JIRA_BACKLOG.md"


def load_jira_env(paths):
    for p in paths:
        path = PROJECT_ROOT / p if not os.path.isabs(p) else Path(p)
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and "=" in line and not line.startswith("#"):
                        k, _, v = line.partition("=")
                        os.environ[k.strip()] = v.strip().strip('"\'')
    # Also try jira.env in repo root
    jira_env = PROJECT_ROOT / "jira.env"
    if jira_env.exists():
        with open(jira_env, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and "=" in line and not line.startswith("#"):
                    k, _, v = line.partition("=")
                    os.environ[k.strip()] = v.strip().strip('"\'')
    return None


def build_from_file():
    """jira-mapping.json 기반 역매핑 + 고정 추가."""
    backlog_to_jira = {}
    if not MAPPING_FILE.exists():
        return backlog_to_jira
    with open(MAPPING_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)
    jira_to_backlog = data.get("_jiraToBacklog") or {}
    for jira_key, backlog_key in jira_to_backlog.items():
        if isinstance(jira_key, str) and isinstance(backlog_key, str):
            backlog_to_jira[backlog_key] = jira_key
    # GAM-22-3 (UserPreference Entity) → GAM-51 (JIRA 실제 키)
    backlog_to_jira["GAM-22-3"] = "GAM-51"
    return backlog_to_jira


def fetch_jira_issues():
    """JIRA API로 GAM 이슈 목록 조회. 실패 시 None."""
    jira_url = (os.getenv("JIRA_URL") or "").rstrip("/")
    jira_email = os.getenv("JIRA_EMAIL") or ""
    jira_token = os.getenv("JIRA_API_TOKEN") or ""
    if not jira_url or not jira_email or not jira_token:
        return None
    auth = base64.b64encode(f"{jira_email}:{jira_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }
    try:
        r = requests.get(
            f"{jira_url}/rest/api/3/search",
            headers=headers,
            params={
                "jql": "project = GAM ORDER BY created ASC",
                "fields": "summary,issuetype",
                "maxResults": 500,
            },
            timeout=30,
        )
        if r.status_code != 200:
            return None
        return r.json().get("issues") or []
    except Exception:
        return None


def normalize_summary(s):
    """요약 정규화 (비교용)."""
    if not s:
        return ""
    s = re.sub(r"\s+", " ", str(s).strip().lower())
    return s


def main():
    load_jira_env(["docs/jira/jira.env", "jira.env"])
    backlog_to_jira = build_from_file()

    issues = fetch_jira_issues()
    if issues and BACKLOG_FILE.exists():
        with open(BACKLOG_FILE, "r", encoding="utf-8") as f:
            backlog_text = f.read()
        # 백로그에서 Story/Task 헤더로 키와 제목 추출
        for m in re.finditer(
            r"###\s+(?:Story|Task)\s+(GAM-\d+(?:-\d+)?)\s*:\s*([^\n]+)",
            backlog_text,
        ):
            backlog_key, title = m.group(1), m.group(2).strip()
            if backlog_key in backlog_to_jira:
                continue
            norm_title = normalize_summary(title)
            for issue in issues:
                jira_key = issue.get("key")
                summary = (issue.get("fields") or {}).get("summary") or ""
                if not jira_key or not summary:
                    continue
                if normalize_summary(summary) == norm_title:
                    backlog_to_jira[backlog_key] = jira_key
                    break
                # 부분 일치: 백로그 제목이 JIRA 요약에 포함
                if norm_title and norm_title in normalize_summary(summary):
                    backlog_to_jira[backlog_key] = jira_key
                    break
    else:
        if not issues:
            print("JIRA API 조회 생략 또는 실패. 파일 기반 매핑만 사용합니다.", file=sys.stderr)

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(backlog_to_jira, f, indent=2, ensure_ascii=False)

    print(f"매핑 생성 완료: {len(backlog_to_jira)}개")
    for bk in sorted(backlog_to_jira.keys()):
        print(f"  {bk} → {backlog_to_jira[bk]}")
    return 0


if __name__ == "__main__":
    sys.exit(main() or 0)
