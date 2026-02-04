package com.goalmond.api.domain.research

/**
 * 범용 조사 에이전트의 4단계.
 * Vocational 등 카테고리별 프롬프트 그룹핑에 사용.
 */
enum class ResearchStage(val displayName: String, val order: Int) {
    STAGE_1_OVERVIEW("기본 구조 파악", 1),
    STAGE_2_SELECTION("학교 선정", 2),
    STAGE_3_FINANCIALS("재정/법적 요건", 3),
    STAGE_4_ROADMAP("졸업 후 로드맵", 4);

    companion object {
        fun fromOrder(order: Int): ResearchStage? = values().find { it.order == order }
    }
}
