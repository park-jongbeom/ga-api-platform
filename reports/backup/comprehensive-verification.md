# 종합 불일치 검증 보고서

Report vs 코드 검증 결과, JIRA 상태를 교차 검증한 결과입니다.

---

## 1. Report 완료인데 코드 미검증 (Report 과대 평가)

| 키 | 제목 | JIRA 상태 |
|----|------|------------|
| GAM-1 | Mock API 및 API 문서(Markdown) 구축 | 완료 |
| GAM-2 | 사용자 프로필 및 유학 목표 관리 | 완료 |
| GAM-7 | Mock API 명세 구현 | 완료 |
| GAM-8 | API 문서(Markdown) & 프론트엔드 협업 가이드 | 완료 |
| GAM-9 | DB 인프라 구축 (단일 모듈 프로젝트) | 완료 |
| GAM-10 | DB 스키마 설계 & 마이그레이션 | 완료 |
| GAM-11 | Mock API 명세 구현 | 완료 |
| GAM-12 | API 문서(Markdown) & 프론트엔드 협업 가이드 | 완료 |
| GAM-13 | DB 인프라 구축 (단일 모듈 프로젝트) | 완료 |
| GAM-21 | DB 스키마 설계 & 마이그레이션 | 완료 |
| GAM-23 | 단위 테스트 & 통합 테스트 | 완료 |

**권장**: JIRA에서 해당 이슈를 To Do로 되돌리거나, 코드 구현 후 재검증.

## 2. 코드 검증 완료인데 Report 남은 작업 (Report 갱신 필요)

| 키 | 제목 |
|----|------|
| GAM-51 | Application API 구현 |

**권장**: `./reports/jira-report-local.sh` 실행 후 Report 갱신.

## 3. JIRA Done인데 코드 없음 (JIRA 오표시)

| 키 | 제목 | JIRA 상태 |
|----|------|------------|
| GAM-1 | Mock API 및 API 문서(Markdown) 구축 | 완료 |
| GAM-2 | 사용자 프로필 및 유학 목표 관리 | 완료 |
| GAM-7 | Mock API 명세 구현 | 완료 |
| GAM-8 | API 문서(Markdown) & 프론트엔드 협업 가이드 | 완료 |
| GAM-9 | DB 인프라 구축 (단일 모듈 프로젝트) | 완료 |
| GAM-10 | DB 스키마 설계 & 마이그레이션 | 완료 |
| GAM-11 | Mock API 명세 구현 | 완료 |
| GAM-12 | API 문서(Markdown) & 프론트엔드 협업 가이드 | 완료 |
| GAM-13 | DB 인프라 구축 (단일 모듈 프로젝트) | 완료 |
| GAM-21 | DB 스키마 설계 & 마이그레이션 | 완료 |
| GAM-23 | 단위 테스트 & 통합 테스트 | 완료 |

**권장**: JIRA에서 해당 이슈를 To Do로 되돌리기.

## 4. 백로그 vs Report 요약

- 백로그 Story 수: 22
- Report 완료 수: 13
- Report 남은 작업 수: 21
