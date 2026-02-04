package com.goalmond.api.service.research

import com.goalmond.api.domain.research.PromptTemplate
import com.goalmond.api.domain.research.ResearchStage

/**
 * Vocational College 조사용 프롬프트 10개 (4단계).
 */
object VocationalPrompts {
    fun register(repository: PromptRepository) {
        // STAGE 1: 기본 구조 파악

        repository.add(PromptTemplate(
            id = "P1_COLLEGE_TYPES",
            category = "vocational",
            stage = ResearchStage.STAGE_1_OVERVIEW,
            description = "CC와 Trade School 차이점 비교",
            template = """
                미국의 Community College(CC)와 Private Vocational School(Trade School)의 차이점을 유학생 관점에서 설명해주세요.
                
                분석 항목:
                1. 학비 비교 (연간 기준, USD)
                2. 이수 기간 (평균)
                3. 비자 유형 차이 (F-1 vs M-1)
                4. OPT 가능 여부 및 기간
                5. 4년제 편입 가능성
                6. 유학생에게 추천하는 선택 기준
                7. 각 유형의 장단점
                
                응답은 표 형식으로 비교하고, 한국어로 작성해주세요.
            """.trimIndent(),
            variables = emptyList()
        ))

        repository.add(PromptTemplate(
            id = "P2_TOP_PROGRAMS",
            category = "vocational",
            stage = ResearchStage.STAGE_1_OVERVIEW,
            description = "인기 Vocational 프로그램 TOP 5",
            template = """
                {field} 분야의 미국 Vocational College 인기 프로그램 TOP 5를 소개해주세요.
                
                각 프로그램마다 다음을 포함하세요:
                1. 프로그램명
                2. 주요 학습 내용
                3. 이수 기간
                4. 평균 학비
                5. 취업률 (또는 예상 취업률)
                6. 평균 연봉 (시작 연봉)
                7. 유학생에게 유리한 점
                
                한국어로 작성하고, 가능하면 구체적인 수치를 제시해주세요.
            """.trimIndent(),
            variables = listOf("field")
        ))

        // STAGE 2: 학교 선정

        repository.add(PromptTemplate(
            id = "P3_SCHOOL_LIST",
            category = "vocational",
            stage = ResearchStage.STAGE_2_SELECTION,
            description = "지역별 추천 Vocational 학교",
            template = """
                {state}에 위치한 {field} 분야 Vocational College 추천 학교 TOP 5를 알려주세요.
                
                각 학교마다 다음을 포함하세요:
                1. 학교명
                2. 위치 (도시)
                3. 프로그램 이름
                4. 학비 (연간, USD)
                5. 이수 기간
                6. SEVIS 승인 여부 (F-1 비자 가능 여부)
                7. 특징 또는 강점
                8. 졸업 후 취업 지원 서비스
                
                한국어로 작성하고, 유학생에게 유리한 학교를 우선 추천해주세요.
            """.trimIndent(),
            variables = listOf("state", "field")
        ))

        repository.add(PromptTemplate(
            id = "P4_STEM_PROGRAMS",
            category = "vocational",
            stage = ResearchStage.STAGE_2_SELECTION,
            description = "STEM OPT 가능한 프로그램",
            template = """
                {state}에서 STEM OPT가 가능한 {field} 분야 Vocational 프로그램을 알려주세요.
                
                다음 정보를 포함하세요:
                1. 프로그램명 및 학교명
                2. STEM Designation 코드 (CIP Code)
                3. OPT 기간 (일반 12개월 + STEM 24개월 = 36개월)
                4. 학비 및 이수 기간
                5. STEM OPT 신청 요건
                6. 졸업 후 취업 가능 분야
                
                한국어로 작성하고, 유학생 관점에서 실용적인 조언을 해주세요.
            """.trimIndent(),
            variables = listOf("state", "field")
        ))

        repository.add(PromptTemplate(
            id = "P5_CAREER_SERVICES",
            category = "vocational",
            stage = ResearchStage.STAGE_2_SELECTION,
            description = "취업 지원 서비스 우수 학교",
            template = """
                {state}의 {field} 분야 Vocational College 중 취업 지원 서비스가 우수한 학교를 추천해주세요.
                
                각 학교의 다음 정보를 포함하세요:
                1. 학교명 및 위치
                2. 취업 지원 서비스 내용 (Job Placement, Career Counseling 등)
                3. 졸업생 취업률
                4. 주요 파트너 기업 (채용 파트너)
                5. 인턴십 또는 CPT 기회
                6. 유학생 대상 취업 지원 특화 프로그램
                
                한국어로 작성하고, 실제 취업 성공 사례가 있다면 포함해주세요.
            """.trimIndent(),
            variables = listOf("state", "field")
        ))

        // STAGE 3: 재정/법적 요건

        repository.add(PromptTemplate(
            id = "P6_SCHOLARSHIPS",
            category = "vocational",
            stage = ResearchStage.STAGE_3_FINANCIALS,
            description = "유학생 장학금 정보",
            template = """
                {state}의 Vocational College에서 유학생이 받을 수 있는 장학금 정보를 알려주세요.
                
                다음 내용을 포함하세요:
                1. 주요 장학금 종류 (Merit-based, Need-based 등)
                2. 지원 자격 및 요건
                3. 지원 금액 (연간 또는 총액)
                4. 신청 시기 및 방법
                5. 유학생 특화 장학금 프로그램
                6. 장학금 외 학비 절감 방법 (Work-study, Payment Plan 등)
                
                한국어로 작성하고, 실용적인 조언을 해주세요.
            """.trimIndent(),
            variables = listOf("state")
        ))

        repository.add(PromptTemplate(
            id = "P7_CPT_WORK",
            category = "vocational",
            stage = ResearchStage.STAGE_3_FINANCIALS,
            description = "CPT 근무 가이드",
            template = """
                Vocational College 재학 중 CPT(Curricular Practical Training)를 통한 근무에 대해 설명해주세요.
                
                다음 내용을 포함하세요:
                1. CPT란 무엇인가? (정의 및 목적)
                2. CPT 신청 요건 (학업 요건, 타이밍)
                3. CPT 종류 (Full-time vs Part-time)
                4. CPT 근무 가능 시간 및 기간
                5. CPT가 OPT에 미치는 영향
                6. {field} 분야에서의 CPT 활용 방법
                7. CPT 신청 절차 및 주의사항
                
                한국어로 작성하고, 유학생 관점에서 실용적인 팁을 제공해주세요.
            """.trimIndent(),
            variables = listOf("field")
        ))

        repository.add(PromptTemplate(
            id = "P8_H1B_PATH",
            category = "vocational",
            stage = ResearchStage.STAGE_3_FINANCIALS,
            description = "H-1B 비자 전환 경로",
            template = """
                Vocational College 졸업 후 H-1B 비자로 전환하는 경로를 설명해주세요.
                
                다음 내용을 포함하세요:
                1. H-1B 비자란? (정의 및 요건)
                2. OPT → H-1B 전환 프로세스
                3. {field} 분야에서의 H-1B 스폰서십 가능성
                4. H-1B 신청 타이밍 및 절차
                5. H-1B 추첨 확률 및 최근 트렌드
                6. H-1B 대안 (O-1, L-1 등)
                7. 유학생이 H-1B를 받기 위한 전략
                
                한국어로 작성하고, 2026년 최신 정보를 기준으로 해주세요.
            """.trimIndent(),
            variables = listOf("field")
        ))

        // STAGE 4: 졸업 후 로드맵

        repository.add(PromptTemplate(
            id = "P9_GREEN_CARD",
            category = "vocational",
            stage = ResearchStage.STAGE_4_ROADMAP,
            description = "EB-3 영주권 가이드",
            template = """
                Vocational College 졸업 후 EB-3 카테고리를 통한 영주권 취득 경로를 설명해주세요.
                
                다음 내용을 포함하세요:
                1. EB-3란? (정의 및 3가지 카테고리)
                2. Vocational 졸업생이 해당하는 EB-3 카테고리
                3. EB-3 신청 요건 및 절차
                4. {field} 분야에서의 EB-3 가능성
                5. 예상 처리 기간 (Priority Date, 대기 시간)
                6. H-1B 없이 EB-3 가능 여부
                7. 영주권 취득 전략 및 주의사항
                
                한국어로 작성하고, 한국인 유학생 관점에서 실용적인 조언을 해주세요.
            """.trimIndent(),
            variables = listOf("field")
        ))

        repository.add(PromptTemplate(
            id = "P10_CITY_ANALYSIS",
            category = "vocational",
            stage = ResearchStage.STAGE_4_ROADMAP,
            description = "도시별 생활 비교",
            template = """
                {cities}에서 Vocational College를 다니며 {field} 분야에서 일할 때의 생활을 비교해주세요.
                
                각 도시별로 다음을 분석하세요:
                1. 생활비 (렌트, 식비, 교통비)
                2. {field} 분야 취업 시장 및 수요
                3. 평균 시작 연봉
                4. 주거 환경 (안전도, 편의시설)
                5. 대중교통 및 교통편
                6. 날씨 및 생활 환경
                7. 한인 커뮤니티 유무
                8. 유학생 관점에서의 총평
                
                한국어로 작성하고, 표 형식으로 비교해주세요.
            """.trimIndent(),
            variables = listOf("cities", "field")
        ))
    }
}
