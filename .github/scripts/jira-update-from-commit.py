#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
커밋 메시지에서 JIRA 이슈 키를 추출하여 해당 이슈를 Done(완료) 상태로 업데이트하는 스크립트.
GitHub Actions CI/CD에서 push 시 자동 실행용.
"""
import os
import re
import sys
import base64
import requests
import time
from typing import List, Set


def extract_issue_keys_from_message(message: str, pattern: str = r'GAM-\d+') -> Set[str]:
    """커밋 메시지에서 JIRA 이슈 키 추출 (중복 제거)."""
    if not message:
        return set()
    return set(re.findall(pattern, message, re.IGNORECASE))


def get_available_transitions(jira_url: str, headers: dict, issue_key: str) -> List[dict]:
    """이슈의 가능한 상태 전환 목록 조회"""
    try:
        url = f"{jira_url}/rest/api/3/issue/{issue_key}/transitions"
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            data = response.json()
            return data.get('transitions', [])
        return []
    except Exception:
        return []


def transition_to_done(jira_url: str, headers: dict, issue_key: str) -> bool:
    """이슈를 Done(완료) 상태로 전환"""
    try:
        transitions = get_available_transitions(jira_url, headers, issue_key)
        done_names = ['done', '완료', 'complete', 'closed', '종료', 'resolved', '해결됨']
        transition_id = None
        for trans in transitions:
            name = (trans.get('name') or '').lower()
            if any(d in name for d in done_names):
                transition_id = trans.get('id')
                break
        if not transition_id and transitions:
            transition_id = transitions[0].get('id')
        if not transition_id:
            return False
        url = f"{jira_url}/rest/api/3/issue/{issue_key}/transitions"
        response = requests.post(url, headers=headers, json={"transition": {"id": transition_id}})
        return response.status_code == 204
    except Exception:
        return False


def main():
    jira_url = os.getenv('JIRA_URL', '').rstrip('/')
    jira_email = os.getenv('JIRA_EMAIL', '')
    jira_api_token = os.getenv('JIRA_API_TOKEN', '')
    commit_message = os.getenv('COMMIT_MESSAGE', '')
    commit_messages = os.getenv('COMMIT_MESSAGES', '')  # newline-separated multiple
    issue_pattern = os.getenv('JIRA_ISSUE_PATTERN', r'GAM-\d+')

    if not jira_url or not jira_email or not jira_api_token:
        print("JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 환경 변수가 필요합니다.")
        sys.exit(0)  # CI에서는 실패하지 않고 스킵

    messages = [commit_message] if commit_message else []
    if commit_messages:
        messages.extend(commit_messages.strip().split('\n'))
    if not messages:
        print("COMMIT_MESSAGE 또는 COMMIT_MESSAGES가 없어 JIRA 업데이트를 건너뜁니다.")
        sys.exit(0)

    keys = set()
    for msg in messages:
        keys.update(extract_issue_keys_from_message(msg, issue_pattern))
    if not keys:
        print("커밋 메시지에서 JIRA 이슈 키를 찾지 못했습니다.")
        sys.exit(0)

    auth_string = f"{jira_email}:{jira_api_token}"
    auth_header = base64.b64encode(auth_string.encode()).decode()
    headers = {
        "Authorization": f"Basic {auth_header}",
        "Content-Type": "application/json",
        "Accept": "application/json"
    }

    ok = 0
    for issue_key in sorted(keys):
        if transition_to_done(jira_url, headers, issue_key):
            print(f"JIRA {issue_key} -> 완료 처리됨")
            ok += 1
        else:
            print(f"JIRA {issue_key} -> 완료 처리 실패 또는 이미 완료")
        time.sleep(0.3)
    print(f"처리: {ok}/{len(keys)} 이슈 완료 상태로 업데이트됨.")


if __name__ == '__main__':
    main()
