package com.goalmond.ai.domain.prompt

import com.goalmond.ai.security.validator.InputSanitizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 프롬프트 템플릿 관리자
 * 
 * LangChain 프롬프트 템플릿 패턴을 적용하여 시스템/사용자 메시지를 분리하고
 * 재사용 가능한 프롬프트 구성을 제공합니다.
 * 
 * 참고: docs/reference/spring-ai-samples/03.LangChain 프롬프트 템플릿.md
 * - PartialPromptTemplate 패턴 적용
 * - 변수 플레이스홀더 방식으로 프롬프트 버전 관리
 * 
 * 보안:
 * - InputSanitizer 통합으로 프롬프트 인젝션 방어
 * - RAG 컨텍스트 출처 검증
 * 
 * @author AI Consultant Team
 * @since 2026-01-21
 */
@Component
class PromptTemplateManager(
    private val inputSanitizer: InputSanitizer
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 시스템 프롬프트 템플릿
     * 
     * 변수:
     * - {expertRole}: 전문가 역할 (예: "유학 상담", "진로 상담")
     * - {securityNote}: 보안 지침
     */
    private val systemTemplate = """
        당신은 Goal-Almond의 {expertRole} 전문 AI 어시스턴트입니다.
        
        역할:
        - 사용자의 질문에 정확하고 친절하게 답변합니다.
        - 제공된 지식 베이스 정보를 우선적으로 참고하여 답변합니다.
        - {securityNote}
        
        답변 원칙:
        1. 친절하고 공감적인 톤을 유지합니다.
        2. 구체적이고 실용적인 조언을 제공합니다.
        3. 불확실한 정보는 명확히 언급합니다.
        4. 한국어로 답변합니다.
    """.trimIndent()
    
    /**
     * RAG 컨텍스트 템플릿
     * 
     * 변수:
     * - {ragContext}: 검색된 관련 문서 내용
     */
    private val ragContextTemplate = """
        === 관련 지식 베이스 정보 ===
        {ragContext}
        === 위 정보를 참고하여 답변해주세요 ===
    """.trimIndent()
    
    /**
     * 사용자 쿼리 템플릿
     * 
     * 변수:
     * - {userQuery}: 사용자의 질문
     */
    private val userQueryTemplate = """
        사용자 질문: {userQuery}
        
        답변:
    """.trimIndent()
    
    /**
     * 전체 프롬프트 생성
     * 
     * 보안 강화: 사용자 쿼리에 대한 프롬프트 인젝션 검증 추가
     * 
     * @param config 프롬프트 설정
     * @return 완성된 프롬프트 문자열
     * @throws SecurityException 프롬프트 인젝션 시도 감지 시
     */
    fun createPrompt(config: PromptConfig): String {
        // 보안 검증: 프롬프트 인젝션 방어
        validatePromptSecurity(config.userQuery)
        
        val systemPrompt = applyPartialTemplate(
            systemTemplate,
            mapOf(
                "expertRole" to config.expertRole,
                "securityNote" to config.securityNote
            )
        )
        
        val ragContext = if (config.ragContext.isNotBlank()) {
            // RAG 컨텍스트도 검증 (추가 보안)
            validatePromptSecurity(config.ragContext)
            applyPartialTemplate(ragContextTemplate, mapOf("ragContext" to config.ragContext))
        } else {
            "관련 문서를 찾을 수 없습니다."
        }
        
        val userQuery = applyPartialTemplate(userQueryTemplate, mapOf("userQuery" to config.userQuery))
        
        logger.debug("프롬프트 생성 완료 (역할: ${config.expertRole}, 쿼리 길이: ${config.userQuery.length})")
        
        return buildString {
            appendLine(systemPrompt)
            appendLine()
            appendLine(ragContext)
            appendLine()
            appendLine(userQuery)
        }
    }
    
    /**
     * 프롬프트 인젝션 공격 검증
     * 
     * 검증 항목:
     * 1. "Ignore previous instructions" 패턴
     * 2. "System:" 등의 역할 위장 시도
     * 3. 특수 문자 조합 패턴
     * 4. 유니코드 우회 방지
     * 5. 공백/구두점 변형 우회 방지
     * 
     * @param text 검증할 텍스트
     * @throws SecurityException 위험 패턴 감지 시
     */
    private fun validatePromptSecurity(text: String) {
        // 정규화: 공백, 구두점, 특수문자 제거
        val normalizedText = normalizeForSecurityCheck(text)
        
        // 위험 패턴 목록 (정규화된 버전 - 구두점 없음!)
        val dangerousPatterns = listOf(
            "ignorepreviousinstructions",
            "ignoreallprevious",
            "disregardprevious",
            "forgeteverything",
            "system",        // 콜론 제거 (정규화 과정에서 제거됨)
            "assistant",     // 콜론 제거 (정규화 과정에서 제거됨)
            "youarenow",
            "newinstructions",
            "override",
            "jailbreak",
            // 추가 패턴
            "ignoreyourprevious",
            "disregardall",
            "actasif",
            "pretendyouare",
            "newrole",
            "roleplay",
            "developermode"
        )
        
        val detectedPattern = dangerousPatterns.firstOrNull { pattern ->
            normalizedText.contains(pattern)
        }
        
        if (detectedPattern != null) {
            logger.warn("프롬프트 인젝션 시도 감지: $detectedPattern (원본 길이: ${text.length})")
            throw SecurityException("프롬프트에 허용되지 않는 패턴이 포함되어 있습니다.")
        }
        
        // InputSanitizer 추가 검증
        val validation = inputSanitizer.validate(text)
        if (!validation.isValid) {
            logger.warn("입력 검증 실패: ${validation.reason}")
            throw SecurityException(validation.reason ?: "입력값 검증에 실패했습니다.")
        }
    }
    
    /**
     * 보안 검증을 위한 텍스트 정규화
     * 
     * 우회 공격 방지:
     * - 유니코드 normalization
     * - 공백 문자 제거
     * - 구두점 제거
     * - 소문자 변환
     * 
     * @param text 원본 텍스트
     * @return 정규화된 텍스트
     */
    private fun normalizeForSecurityCheck(text: String): String {
        // CRITICAL: Unicode normalization BEFORE lowercase to handle fancy Unicode properly
        return text
            // 1. 유니코드 정규화 (NFKD: 호환성 분해) - ℐ → I, ﬁ → fi 등
            .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFKD) }
            // 2. 소문자 변환 (AFTER normalization!)
            .lowercase()
            // 3. 공백 문자 모두 제거 (스페이스, 탭, Zero-width space 등)
            .replace(Regex("\\s+"), "")
            // 4. 구두점 및 특수문자 제거 (마크다운 문자 포함: * _ # [ ] 등)
            .replace(Regex("[.,!?;:'\"`\\-*_#\\[\\](){}|\\\\/<>@]"), "")
            // 5. 제어 문자 제거
            .replace(Regex("\\p{C}"), "")
    }
    
    /**
     * 유학 상담 전용 프롬프트 생성 (PartialPromptTemplate 패턴)
     * 
     * @param ragContext RAG 검색 결과
     * @param userQuery 사용자 질문
     * @return 완성된 프롬프트
     */
    fun createStudyAbroadPrompt(ragContext: String, userQuery: String): String {
        return createPrompt(
            PromptConfig(
                expertRole = "유학 상담",
                securityNote = "민감정보(여권번호, 성적 등)는 절대 요구하지 않습니다.",
                ragContext = ragContext,
                userQuery = userQuery
            )
        )
    }
    
    /**
     * 진로 상담 전용 프롬프트 생성 (PartialPromptTemplate 패턴)
     * 
     * @param ragContext RAG 검색 결과
     * @param userQuery 사용자 질문
     * @return 완성된 프롬프트
     */
    fun createCareerPrompt(ragContext: String, userQuery: String): String {
        return createPrompt(
            PromptConfig(
                expertRole = "진로 상담",
                securityNote = "개인 정보 보호를 최우선으로 합니다.",
                ragContext = ragContext,
                userQuery = userQuery
            )
        )
    }
    
    /**
     * PartialPromptTemplate 패턴 구현
     * 
     * 템플릿 문자열의 {변수명} 플레이스홀더를 실제 값으로 치환합니다.
     * 
     * @param template 템플릿 문자열
     * @param variables 변수 맵
     * @return 치환된 문자열
     */
    private fun applyPartialTemplate(template: String, variables: Map<String, String>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
    
    /**
     * 프롬프트 버전 정보
     */
    fun getVersion(): String = "v1.0.0"
    
    /**
     * 사용 가능한 템플릿 유형
     */
    enum class TemplateType {
        STUDY_ABROAD,  // 유학 상담
        CAREER,        // 진로 상담
        GENERAL        // 일반 상담
    }
}

/**
 * 프롬프트 설정 데이터 클래스
 * 
 * @property expertRole 전문가 역할
 * @property securityNote 보안 지침
 * @property ragContext RAG 검색 컨텍스트
 * @property userQuery 사용자 질문
 */
data class PromptConfig(
    val expertRole: String,
    val securityNote: String,
    val ragContext: String,
    val userQuery: String
)
