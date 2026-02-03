#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 완료 상태 vs 실제 코드 검증
완료 처리된 Task에 해당하는 파일/코드가 실제로 존재하는지 검증
"""
import json
import os
import re
from pathlib import Path
from typing import Optional

PROJECT_ROOT = Path(__file__).resolve().parents[2]
JIRA_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
OUTPUT_JSON = PROJECT_ROOT / ".github" / "code-completion-verification.json"
OUTPUT_MD = PROJECT_ROOT / "reports" / "jira-code-verification.md"

# Task key -> 예상 파일/패턴 (여러 개 가능)
# summary에서 추출하기 어려운 것은 명시적 매핑
TASK_TO_FILES = {
    "GAM-27": ["settings.gradle.kts", "build.gradle.kts"],  # 단일 모듈 구조
    "GAM-28": ["MockMatchingController.kt"],
    "GAM-29": ["MatchingResponse.kt"],
    "GAM-30": ["SchoolResponse.kt"],
    "GAM-31": ["ProgramResponse.kt"],
    "GAM-32": [],  # Mock 데이터 - MockMatchingController 내부 또는 별도
    "GAM-33": ["docs/api/API.md"],
    "GAM-34": ["docs/api/matching.md", "docs/api/programs.md", "docs/api/schools.md"],
    "GAM-35": ["docs/FRONTEND_HANDOFF.md"],
    "GAM-36": ["docs/04_FRONTEND_COOPERATION.md"],
    "GAM-37": ["README.md"],
    "GAM-38": ["build.gradle.kts"],
    "GAM-39": ["application.yml"],
    "GAM-40": ["application-lightsail.yml", "application-local.yml"],
    "GAM-52": ["UserRepository.kt", "AcademicProfileRepository.kt", "UserPreferenceRepository.kt"],
    "GAM-53": ["UserProfileService.kt"],
    "GAM-54": ["UserProfileController.kt"],
    "GAM-55": ["UserProfileRequest.kt"],
    "GAM-72": ["ScoringService", "학업", "적합도"],
    "GAM-73": ["영어", "적합도", "ScoringService"],
    "GAM-74": ["예산", "적합도"],
    "GAM-75": ["지역", "선호"],
    "GAM-76": ["기간", "적합도"],
    "GAM-77": ["진로", "연계"],
    "GAM-78": ["WeightConfig.kt"],
    "GAM-79": ["GPA", "정규화"],
    "GAM-80": ["PathOptimizationService.kt"],
    "GAM-81": ["경로", "최적화"],
    "GAM-82": ["리스크", "패널티"],
    "GAM-83": ["추천", "유형", "분류"],
    "GAM-84": ["최종", "점수", "검증"],
    "GAM-85": ["School.kt"],
    "GAM-86": ["Program.kt"],
    "GAM-87": ["SchoolRepository.kt"],
    "GAM-88": ["ProgramRepository.kt"],
    "GAM-89": ["seed_schools", "seed.*school"],
    "GAM-90": ["seed_programs", "seed.*program"],
    "GAM-91": ["SchoolService.kt"],
    "GAM-92": ["ProgramController.kt"],
    "GAM-93": ["MatchingResult.kt"],
    "GAM-94": ["MatchingResultRepository.kt"],
    "GAM-95": ["MatchingController.kt"],
    "GAM-96": ["매칭", "파이프라인", "MatchingService"],
    "GAM-97": ["결과", "저장", "MatchingResult"],
    "GAM-98": ["결과", "조회"],
    "GAM-99": ["캐싱", "인덱스", "Cache"],
    "GAM-100": ["ExplanationService.kt"],
    "GAM-101": ["설명", "템플릿"],
    "GAM-102": ["점수", "기여도"],
    "GAM-103": ["긍정", "부정", "요소"],
    "GAM-104": ["설명", "문구", "생성"],
    "GAM-105": ["체크리스트", "스키마"],
    "GAM-106": [],
    "GAM-107": [],
    "GAM-108": ["Application.kt"],
    "GAM-109": ["ApplicationRepository.kt"],
    "GAM-110": ["ApplicationService.kt"],
    "GAM-111": ["ApplicationController.kt"],
    "GAM-112": ["ApplicationStatus"],
    "GAM-113": ["Progress", "계산"],
    "GAM-114": ["Document.kt"],
    "GAM-115": ["DocumentRepository.kt"],
    "GAM-116": ["DocumentService.kt"],
    "GAM-117": ["FileStorageService", "S3", "Local"],
    "GAM-118": ["DocumentController.kt"],
    "GAM-119": ["파일", "검증", "validate"],
    "GAM-120": ["파일", "삭제", "delete"],
    "GAM-121": ["DashboardController.kt"],
    "GAM-122": ["DashboardService.kt"],
    "GAM-123": ["집계", "쿼리", "dashboard"],
    "GAM-124": ["DashboardResponse"],
    "GAM-125": ["E2E", "integration", "test"],
    "GAM-126": ["성능", "테스트", "performance"],
    "GAM-127": ["테스트", "데이터"],
    "GAM-128": [],
    "GAM-129": ["JwtAuthenticationFilter.kt"],
    "GAM-130": ["RateLimitingFilter", "RateLimit"],
    "GAM-131": ["InputValidator", "Validator"],
    "GAM-132": ["CORS", "WebConfig", "cors"],
    "GAM-133": ["보안", "체크리스트"],
    "GAM-134": ["Actuator", "actuator"],
    "GAM-135": ["Prometheus", "prometheus", "micrometer"],
    "GAM-136": ["log", "json", "logging"],
    "GAM-137": ["메트릭", "metric", "custom"],
    "GAM-138": ["README.md"],
    "GAM-139": ["04_FRONTEND_COOPERATION"],
    "GAM-140": ["API", "사용", "예시"],
    "GAM-141": ["트러블슈팅", "troubleshoot"],
}


def find_file(relative_path: str) -> bool:
    """프로젝트 루트 기준 파일 존재 여부"""
    p = PROJECT_ROOT / relative_path
    return p.exists()


def find_any_file(patterns: list) -> bool:
    """패턴 중 하나라도 파일명으로 존재하면 True. .kt/.md/.yml 등은 파일명 완전 일치만 인정."""
    if not patterns:
        return False
    for pattern in patterns:
        exact_match = pattern.endswith(".kt") or pattern.endswith(".md") or pattern.endswith(".yml") or pattern.endswith(".sql")
        for root, dirs, files in os.walk(PROJECT_ROOT):
            if "node_modules" in root or ".git" in root or "build" in root:
                continue
            for f in files:
                if exact_match:
                    if f == pattern:
                        return True
                else:
                    if pattern in f or f == pattern:
                        return True
    return False


def search_in_code(patterns: list) -> bool:
    """소스/설정 파일 내용에 패턴이 하나라도 있으면 True"""
    if not patterns:
        return False
    ext = (".kt", ".yml", ".yaml", ".md", ".sql", ".kts")
    for root, dirs, files in os.walk(PROJECT_ROOT):
        if "node_modules" in root or ".git" in root or "build" in root:
            continue
        for f in files:
            if not any(f.endswith(e) for e in ext):
                continue
            path = Path(root) / f
            try:
                text = path.read_text(encoding="utf-8", errors="ignore")
                for pat in patterns:
                    if pat in text:
                        return True
            except Exception:
                pass
    return False


def verify_task(task_key: str, summary: str, expected: list) -> tuple[bool, str]:
    """
    Task에 대한 코드 구현 여부 검증.
    Returns: (구현됨 여부, 사유)
    """
    if not expected:
        # 매핑 없음: summary에서 .kt 파일명 추출 시도
        m = re.search(r"(\w+)\.kt", summary)
        if m:
            name = m.group(1) + ".kt"
            if find_any_file([name]):
                return True, f"파일 발견: {name}"
        return False, "검증 매핑 없음"
    
    # 첫 요소가 확실한 파일명이면 파일 존재 여부로 판단
    file_like = [x for x in expected if x.endswith(".kt") or x.endswith(".md") or x.endswith(".yml") or x.endswith(".sql") or "/" in x]
    if file_like:
        for rel in file_like:
            if "/" in rel:
                if find_file(rel):
                    return True, f"파일 발견: {rel}"
            else:
                if find_any_file([rel]):
                    return True, f"파일 발견: {rel}"
        return False, f"다음 파일 없음: {file_like}"
    
    # 키워드 검색
    if search_in_code(expected):
        return True, f"코드 내 키워드 발견: {expected[:3]}"
    return False, f"코드 내 키워드 없음: {expected[:3]}"


def main():
    with open(JIRA_FILE, "r", encoding="utf-8") as f:
        issues = json.load(f)
    
    done_tasks = [i for i in issues if i["type"] == "작업" and i["status"] == "완료"]
    
    results = []
    by_epic = {}
    
    for task in done_tasks:
        key = task["key"]
        summary = task["summary"]
        parent = task.get("parent") or "UNKNOWN"
        
        expected = TASK_TO_FILES.get(key, [])
        implemented, reason = verify_task(key, summary, expected)
        
        results.append({
            "key": key,
            "summary": summary,
            "parent": parent,
            "implemented": implemented,
            "reason": reason,
        })
        
        if parent not in by_epic:
            by_epic[parent] = {"total": 0, "implemented": 0, "tasks": []}
        by_epic[parent]["total"] += 1
        if implemented:
            by_epic[parent]["implemented"] += 1
        by_epic[parent]["tasks"].append({
            "key": key,
            "summary": summary,
            "implemented": implemented,
            "reason": reason,
        })
    
    # JSON 저장
    out = {
        "total_done": len(done_tasks),
        "total_implemented": sum(1 for r in results if r["implemented"]),
        "results": results,
        "by_epic": {k: {"total": v["total"], "implemented": v["implemented"], "tasks": v["tasks"]} for k, v in by_epic.items()},
    }
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
    
    # Markdown 리포트
    total_impl = out["total_implemented"]
    total_done = out["total_done"]
    
    md_lines = [
        "# JIRA 완료 상태 vs 실제 코드 검증 보고서",
        "",
        "**검증일**: 자동 생성",
        "",
        "## 요약",
        "",
        f"- JIRA 완료 처리된 Task: **{total_done}개**",
        f"- 실제 코드로 구현됨: **{total_impl}개** ({100 * total_impl // max(total_done, 1)}%)",
        f"- 미구현(불일치): **{total_done - total_impl}개**",
        "",
        "## Epic별 검증 결과",
        "",
        "| Epic | 완료 Task | 실제 구현 | 불일치 |",
        "|------|----------|----------|--------|",
    ]
    
    for epic in sorted(by_epic.keys()):
        if epic == "UNKNOWN":
            continue
        e = by_epic[epic]
        impl = e["implemented"]
        tot = e["total"]
        diff = tot - impl
        md_lines.append(f"| {epic} | {tot}개 | {impl}개 | {diff}개 |")
    
    md_lines.extend([
        "",
        "## 미구현 Task 상세 (JIRA 완료 ↔ 코드 없음)",
        "",
    ])
    
    for r in results:
        if not r["implemented"]:
            md_lines.append(f"- **{r['key']}** ({r['parent']}): {r['summary']}")
            md_lines.append(f"  - 사유: {r['reason']}")
            md_lines.append("")
    
    md_lines.extend([
        "## 구현됨 Task (참고)",
        "",
    ])
    for r in results:
        if r["implemented"]:
            md_lines.append(f"- **{r['key']}**: {r['summary']} — {r['reason']}")
    
    # JIRA 상태 수정 방안
    not_impl_keys = [r["key"] for r in results if not r["implemented"]]
    md_lines.extend([
        "",
        "---",
        "",
        "## JIRA 상태 수정 방안",
        "",
        "### 요약",
        "",
        f"JIRA에 **완료**로 되어 있으나 실제 코드가 없는 Task **{len(not_impl_keys)}개**에 대해 아래 중 하나를 적용하는 것을 권장합니다.",
        "",
        "### 옵션 A: 미구현 Task를 \"해야 할 일\"로 되돌리기 (권장)",
        "",
        "- **대상**: 위 \"미구현 Task 상세\" 목록",
        "- **방법**: JIRA에서 해당 이슈 상태를 \"완료\" → \"해야 할 일\"로 전환",
        "- **장점**: 백로그가 실제 작업량과 일치함",
        "",
        "### 옵션 B: \"code-incomplete\" 라벨 추가",
        "",
        "- **방법**: JIRA 이슈에 라벨 `code-incomplete` 추가 (상태는 완료 유지)",
        "- **활용**: 필터 `labels = code-incomplete` 로 재작업 대상 조회",
        "",
        "### 옵션 C: 기술 부채 Epic 생성",
        "",
        "- **방법**: 새 Epic 생성 후 미구현 Task를 해당 Epic 하위로 이동 또는 링크",
        "",
        "### 검증 재실행",
        "",
        "```bash",
        "python3 .github/scripts/jira-verify-code-completion.py",
        "```",
    ])
    
    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write("\n".join(md_lines))
    
    print(f"완료 Task: {total_done}개")
    print(f"실제 구현: {total_impl}개 ({100 * total_impl // max(total_done, 1)}%)")
    print(f"미구현: {total_done - total_impl}개")
    print(f"\nJSON: {OUTPUT_JSON}")
    print(f"리포트: {OUTPUT_MD}")


if __name__ == "__main__":
    main()
