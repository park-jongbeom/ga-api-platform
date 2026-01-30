#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GitHub에서 중복된 '프로젝트 진행 상황 보고서' 이슈 정리.
동일 제목의 open 이슈가 여러 개 있으면 가장 최근 1개만 남기고 나머지는 close.
"""
import os
import sys
import json
import argparse
import requests
from collections import defaultdict
from typing import List, Dict


def list_open_report_issues(token: str, repo: str) -> List[dict]:
    """label=report, state=open 인 이슈 목록 조회."""
    url = f"https://api.github.com/repos/{repo}/issues"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json",
    }
    params = {"labels": "report", "state": "open", "per_page": 100}
    out = []
    page = 1
    while True:
        r = requests.get(url, headers=headers, params={**params, "page": page})
        if r.status_code != 200:
            break
        data = r.json()
        if not data:
            break
        out.extend(data)
        if len(data) < 100:
            break
        page += 1
    return out


def close_issue(token: str, repo: str, issue_number: int) -> bool:
    """이슈를 closed 상태로 변경."""
    url = f"https://api.github.com/repos/{repo}/issues/{issue_number}"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json",
    }
    r = requests.patch(url, headers=headers, json={"state": "closed"})
    return r.status_code == 200


def run(token: str, repo: str, dry_run: bool = False) -> None:
    issues = list_open_report_issues(token, repo)
    by_title: Dict[str, List[dict]] = defaultdict(list)
    for i in issues:
        title = (i.get("title") or "").strip()
        if title:
            by_title[title].append(i)

    closed_count = 0
    for title, group in by_title.items():
        if len(group) <= 1:
            continue
        # updated_at 기준 최신 1개 유지, 나머지 close (최신 = 인덱스 0으로 정렬)
        group.sort(key=lambda x: x.get("updated_at") or "", reverse=True)
        keep = group[0]
        duplicates = group[1:]
        print(f"제목: {title}")
        print(f"  유지: #{keep['number']} (updated: {keep.get('updated_at', '')})")
        for dup in duplicates:
            print(f"  중복 close: #{dup['number']} (updated: {dup.get('updated_at', '')})")
            if not dry_run:
                if close_issue(token, repo, dup["number"]):
                    closed_count += 1
                    print(f"    ✓ #{dup['number']} closed")
                else:
                    print(f"    ✗ #{dup['number']} close 실패")
        print()

    if closed_count or dry_run:
        print(f"중복 보고서 이슈: {closed_count}개 close 완료." if not dry_run else "[DRY RUN] 위와 같이 close 예정.")


def main():
    parser = argparse.ArgumentParser(description="GitHub 중복 보고서 이슈 정리 (동일 제목 → 1개만 유지)")
    parser.add_argument("--token", default=os.getenv("GITHUB_TOKEN"), help="GitHub token")
    parser.add_argument("--repo", default=os.getenv("GITHUB_REPOSITORY"), help="owner/repo")
    parser.add_argument("--dry-run", action="store_true", help="실제 close 하지 않고 목록만 출력")
    args = parser.parse_args()

    if not args.token or not args.repo:
        print("오류: GITHUB_TOKEN, GITHUB_REPOSITORY 환경 변수 또는 --token, --repo 필요.", file=sys.stderr)
        sys.exit(1)

    run(args.token, args.repo, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
