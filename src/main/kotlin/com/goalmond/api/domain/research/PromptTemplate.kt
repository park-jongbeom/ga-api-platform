package com.goalmond.api.domain.research

/**
 * 조사 에이전트용 프롬프트 템플릿.
 * 변수 치환 {field}, {state}, {cities} 등 지원.
 */
data class PromptTemplate(
    val id: String,
    val category: String,
    val stage: ResearchStage,
    val template: String,
    val variables: List<String> = emptyList(),
    val description: String = ""
) {
    /**
     * 변수 맵으로 템플릿 내 {key} 치환.
     */
    fun render(variablesMap: Map<String, Any>): String {
        var rendered = template
        variablesMap.forEach { (key, value) ->
            rendered = rendered.replace("{$key}", value.toString())
        }
        return rendered
    }
}
