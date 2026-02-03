#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
고아 Task를 summary 기반으로 Epic에 자동 분류
"""
import json
import re
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
ISSUES_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
OUTPUT_FILE = PROJECT_ROOT / ".github" / "jira-task-epic-auto-mapping.json"

# Epic별 키워드 패턴
EPIC_KEYWORDS = {
    "GAM-1": [
        # Epic 1: API 인프라 & Mock 데이터
        r"flyway", r"설정", r"entity.*생성", r"user.*entity",
        r"migration", r"db", r"schema", r"테이블", r"매핑"
    ],
    "GAM-2": [
        # Epic 2: User Profile & Preference (이미 배치됨, 추가 없음)
        r"profile", r"preference", r"academic", r"repository",
        r"userprofile", r"dto", r"validation"
    ],
    "GAM-3": [
        # Epic 3: 매칭 엔진
        r"적합도", r"점수.*계산", r"scoring", r"filter", r"weight",
        r"hardfilter", r"학업", r"영어", r"예산", r"지역", r"기간", r"진로",
        r"pathoptimization", r"경로.*최적화", r"리스크.*패널티",
        r"추천.*유형", r"최종.*점수", r"정규화"
    ],
    "GAM-4": [
        # Epic 4: 매칭 API
        r"school.*entity", r"program.*entity", r"school.*repository",
        r"program.*repository", r"seed.*school", r"seed.*program",
        r"matchingresult", r"매칭.*파이프라인", r"매칭.*controller",
        r"결과.*저장", r"결과.*조회", r"성능.*최적화",
        r"explanation", r"설명.*템플릿", r"점수.*기여도", r"긍정.*부정",
        r"e2e.*호출", r"mock.*실제", r"체크리스트"
    ],
    "GAM-5": [
        # Epic 5: Application, Document, Dashboard
        r"application.*entity", r"application.*repository",
        r"application.*service", r"application.*controller",
        r"applicationstatus", r"progress.*계산",
        r"document.*entity", r"document.*repository", r"document.*service",
        r"filestorage", r"document.*controller", r"파일.*검증", r"파일.*삭제",
        r"dashboard", r"집계.*쿼리", r"dashboard.*response",
        r"e2e.*테스트", r"성능.*테스트", r"테스트.*데이터", r"테스트.*실행"
    ],
    "GAM-6": [
        # Epic 6: 보안, 모니터링, 문서
        r"jwt", r"authentication", r"ratelimit", r"validator",
        r"cors", r"보안.*체크리스트",
        r"actuator", r"prometheus", r"메트릭",
        r"로그.*포맷", r"커스텀.*메트릭",
        r"readme", r"frontend.*cooperation", r"api.*사용.*예시",
        r"트러블슈팅", r"가이드", r"문서"
    ]
}


def classify_task(summary: str) -> str:
    """Task summary를 분석하여 가장 적합한 Epic 반환"""
    summary_lower = summary.lower()
    
    # Epic별 점수 계산
    scores = {}
    for epic_key, patterns in EPIC_KEYWORDS.items():
        score = 0
        for pattern in patterns:
            if re.search(pattern, summary_lower):
                score += 1
        scores[epic_key] = score
    
    # 가장 높은 점수의 Epic 선택
    if max(scores.values()) > 0:
        return max(scores, key=scores.get)
    
    # 매칭 안되면 수동 분류 필요
    return "UNKNOWN"


def main():
    with open(ISSUES_FILE, "r", encoding="utf-8") as f:
        issues = json.load(f)
    
    # 고아 Task 찾기
    orphan_tasks = [i for i in issues if i["type"] == "작업" and not i.get("parent")]
    
    print(f"총 {len(orphan_tasks)}개 고아 Task 분류 시작\n")
    
    # 분류
    classified = {f"GAM-{i}": [] for i in range(1, 7)}
    classified["UNKNOWN"] = []
    
    for task in orphan_tasks:
        key = task["key"]
        summary = task["summary"]
        epic = classify_task(summary)
        
        classified[epic].append({
            "key": key,
            "summary": summary
        })
    
    # 결과 출력
    print("=== 분류 결과 ===")
    for epic_key in sorted(classified.keys()):
        tasks = classified[epic_key]
        if tasks:
            print(f"\n{epic_key}: {len(tasks)}개")
            for task in tasks[:5]:
                print(f"  {task['key']}: {task['summary'][:60]}")
            if len(tasks) > 5:
                print(f"  ... 외 {len(tasks) - 5}개")
    
    # task_to_epic 매핑 생성
    task_to_epic = {}
    for epic_key, tasks in classified.items():
        if epic_key != "UNKNOWN":
            for task in tasks:
                task_to_epic[task["key"]] = epic_key
    
    # JSON 저장
    output = {
        "task_to_epic": task_to_epic,
        "classified": classified,
        "total": len(orphan_tasks),
        "classified_count": len(task_to_epic),
        "unknown_count": len(classified["UNKNOWN"])
    }
    
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ 매핑 저장: {OUTPUT_FILE}")
    print(f"  분류 완료: {len(task_to_epic)}개")
    print(f"  미분류: {len(classified['UNKNOWN'])}개")


if __name__ == "__main__":
    main()
