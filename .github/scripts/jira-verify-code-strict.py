#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JIRA 완료 상태 vs 실제 코드 검증 (엄격 모드)
- Entity/Service/Controller/Repository/DTO: src/ 폴더에 정확한 클래스 파일 존재 필수
- 키워드 검색: src/ 폴더만 (docs/, reports/, .github/ 제외)
"""
import json
import os
import re
from pathlib import Path
from typing import Optional

PROJECT_ROOT = Path(__file__).resolve().parents[2]
SRC_ROOT = PROJECT_ROOT / "src"
JIRA_FILE = PROJECT_ROOT / ".github" / "jira-backend-issues.json"
OUTPUT_JSON = PROJECT_ROOT / ".github" / "strict-code-completion-verification.json"
OUTPUT_JSON_ALL = PROJECT_ROOT / ".github" / "strict-all-tasks-verification.json"
OUTPUT_MD = PROJECT_ROOT / "reports" / "jira-strict-code-verification.md"

# Task key -> 예상 파일/패턴 (동일 구조, 검증 로직만 엄격화)
TASK_TO_FILES = {
    "GAM-27": ["settings.gradle.kts", "build.gradle.kts"],
    "GAM-28": ["MockMatchingController.kt"],
    "GAM-29": ["MatchingResponse.kt"],
    "GAM-30": ["SchoolResponse.kt"],
    "GAM-31": ["ProgramResponse.kt"],
    "GAM-32": ["MockScenario", "SAFE", "CHALLENGE", "STRATEGY"],
    "GAM-33": ["docs/api/API.md"],
    "GAM-34": ["docs/api/matching.md", "docs/api/programs.md", "docs/api/schools.md"],
    "GAM-35": ["docs/FRONTEND_HANDOFF.md"],
    "GAM-36": ["docs/04_FRONTEND_COOPERATION.md"],
    "GAM-37": ["README.md"],
    "GAM-38": ["build.gradle.kts"],
    "GAM-39": ["application.yml"],
    "GAM-40": ["application-lightsail.yml", "application-local.yml"],
    "GAM-41": ["src/main/resources/db/migration/V1__create_schema_from_doc.sql"],
    "GAM-46": ["flyway", "enabled", "classpath"],
    "GAM-49": ["User.kt"],
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
    "GAM-89": ["seed_schools", "seed"],
    "GAM-90": ["seed_programs", "seed"],
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


def verify_class_file(class_name: str, file_extension: str = ".kt") -> bool:
    """src/ 폴더에서 정확한 파일명 검색 후 클래스/인터페이스 정의 확인."""
    if not SRC_ROOT.exists():
        return False
    filename = class_name + file_extension
    for root, _dirs, files in os.walk(SRC_ROOT):
        if filename in files:
            path = Path(root) / filename
            try:
                content = path.read_text(encoding="utf-8", errors="ignore")
                # data class X / class X / interface X
                if re.search(
                    rf"\b(data\s+)?class\s+{re.escape(class_name)}\b|\binterface\s+{re.escape(class_name)}\b",
                    content,
                ):
                    return True
            except Exception:
                pass
    return False


def verify_dto(dto_name: str) -> bool:
    """DTO: src/ 내 해당 이름 또는 XxxResponse/XxxRequest 클래스 존재."""
    if verify_class_file(dto_name):
        return True
    if verify_class_file(dto_name + "Response"):
        return True
    if verify_class_file(dto_name + "Request"):
        return True
    return False


def search_in_src_only(patterns: list) -> bool:
    """src/ 폴더만 검색 (docs/, reports/, .github/ 제외). .kt, .yml, .kts (resources)."""
    if not patterns:
        return False
    exts = (".kt", ".yml", ".yaml", ".kts")
    for root, _dirs, files in os.walk(SRC_ROOT):
        for f in files:
            if not any(f.endswith(e) for e in exts):
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


def find_file_root(relative_path: str) -> bool:
    """프로젝트 루트 또는 src/main/resources 기준 파일 존재."""
    p = PROJECT_ROOT / relative_path
    if p.exists():
        return True
    # application*.yml 등은 src/main/resources에 있을 수 있음
    if "application" in relative_path and relative_path.endswith(".yml"):
        r = SRC_ROOT / "main" / "resources" / relative_path
        return r.exists()
    return False


def find_any_file_root(patterns: list) -> bool:
    """패턴 중 하나라도 루트/리소스에 존재하면 True."""
    if not patterns:
        return False
    for rel in patterns:
        if "/" in rel or rel.endswith(".md") or rel.endswith(".yml") or rel.endswith(".kts") or rel.endswith(".sql"):
            if find_file_root(rel):
                return True
    return False


def verify_task(task_key: str, summary: str, expected: list) -> tuple[bool, str]:
    """
    엄격 검증: .kt는 src/ 내 파일+클래스 정의, 문서/설정은 루트, 키워드는 src/만.
    """
    if not expected:
        m = re.search(r"(\w+)\.kt", summary)
        if m:
            name = m.group(1)
            if verify_class_file(name):
                return True, f"src/ 클래스 발견: {name}"
        return False, "검증 매핑 없음"

    # 1) .kt 파일: src/ 내 정확한 클래스 파일 존재 필수
    kt_files = [x.replace(".kt", "") for x in expected if x.endswith(".kt")]
    if kt_files:
        for class_name in kt_files:
            if verify_class_file(class_name):
                return True, f"src/ 클래스 발견: {class_name}.kt"
        return False, f"src/에 다음 클래스 없음: {[c + '.kt' for c in kt_files]}"

    # 2) 문서/설정 경로 (docs/, README, build.gradle.kts, application*.yml)
    root_like = [x for x in expected if "/" in x or x.endswith(".md") or x.endswith(".yml") or x.endswith(".kts") or x.endswith(".sql")]
    if root_like:
        for rel in root_like:
            if find_file_root(rel):
                return True, f"파일 발견: {rel}"
        return False, f"다음 파일 없음: {root_like}"

    # 3) DTO 이름만 (예: DashboardResponse) -> src/에 해당 클래스 필수
    dto_like = [x for x in expected if "Response" in x or "Request" in x]
    if dto_like:
        for name in dto_like:
            if verify_class_file(name) or verify_dto(name):
                return True, f"src/ DTO 발견: {name}"
        return False, f"src/에 DTO 없음: {dto_like}"

    # 4) 키워드: src/ 폴더만 검색
    if search_in_src_only(expected):
        return True, f"src/ 내 키워드 발견: {expected[:3]}"
    return False, f"src/ 내 키워드 없음: {expected[:3]}"


def main():
    import argparse
    parser = argparse.ArgumentParser(description="JIRA 완료 vs 실제 코드 엄격 검증")
    parser.add_argument("--all-tasks", action="store_true", help="JIRA 상태 무관, 전체 작업(type=작업) 검증 후 JSON 저장")
    args = parser.parse_args()

    with open(JIRA_FILE, "r", encoding="utf-8") as f:
        issues = json.load(f)

    if args.all_tasks:
        tasks_to_verify = [i for i in issues if i.get("type") == "작업"]
        out_path = OUTPUT_JSON_ALL
    else:
        tasks_to_verify = [i for i in issues if i["type"] == "작업" and i["status"] == "완료"]
        out_path = OUTPUT_JSON

    results = []
    by_epic = {}

    for task in tasks_to_verify:
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

    total_impl = sum(1 for r in results if r["implemented"])
    total_tasks = len(tasks_to_verify)

    out = {
        "total_tasks": total_tasks,
        "total_implemented": total_impl,
        "results": results,
        "by_epic": {k: {"total": v["total"], "implemented": v["implemented"], "tasks": v["tasks"]} for k, v in by_epic.items()},
    }
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=2)

    if not args.all_tasks:
        md_lines = [
            "# JIRA 완료 상태 vs 실제 코드 검증 (엄격 모드) 보고서",
            "",
            "**검증 원칙**: Entity/Service/Controller/Repository/DTO는 src/ 내 파일+클래스 정의 필수, 키워드는 src/만 검색.",
            "",
            "## 요약",
            "",
            f"- JIRA 완료 Task: **{total_tasks}개**",
            f"- 실제 구현됨 (엄격): **{total_impl}개** ({100 * total_impl // max(total_tasks, 1)}%)",
            f"- 미구현(불일치): **{total_tasks - total_impl}개**",
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
            md_lines.append(f"| {epic} | {e['total']}개 | {e['implemented']}개 | {e['total'] - e['implemented']}개 |")
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
        md_lines.extend(["## 구현됨 Task (참고)", ""])
        for r in results:
            if r["implemented"]:
                md_lines.append(f"- **{r['key']}**: {r['summary']} — {r['reason']}")
        not_impl_keys = [r["key"] for r in results if not r["implemented"]]
        md_lines.extend([
            "",
            "---",
            "",
            "## JIRA 상태 수정 방안",
            "",
            f"미구현 Task **{len(not_impl_keys)}개**를 \"해야 할 일\"로 되돌리려면:",
            "",
            "```bash",
            "python3 .github/scripts/jira-revert-code-incomplete-to-todo.py",
            "```",
            "",
            "### 검증 재실행",
            "",
            "```bash",
            "python3 .github/scripts/jira-verify-code-strict.py",
            "```",
        ])
        OUTPUT_MD.parent.mkdir(parents=True, exist_ok=True)
        with open(OUTPUT_MD, "w", encoding="utf-8") as f:
            f.write("\n".join(md_lines))
        print(f"리포트: {OUTPUT_MD}")

    print(f"검증 Task: {total_tasks}개")
    print(f"실제 구현 (엄격): {total_impl}개 ({100 * total_impl // max(total_tasks, 1)}%)")
    print(f"미구현: {total_tasks - total_impl}개")
    print(f"\nJSON: {out_path}")


if __name__ == "__main__":
    main()
