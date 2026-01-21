package com.goalmond.ai.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * 감사 로그 서비스
 * 
 * gRPC를 통해 ga-audit-service에 감사 로그를 전송합니다.
 * 
 * 보안 준수 항목:
 * - Audit Log: 모든 AI 요청/응답 기록
 * - 민감정보 제외: 마스킹된 데이터만 로깅
 */
@Service
class AuditLogService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * AI 상담 요청 감사 로그 기록
     * 
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @param conversationId 대화 세션 ID
     * @param maskedQuery 마스킹된 쿼리
     * @param responseSummary 응답 요약
     */
    fun logAiConsultation(
        userId: String,
        tenantId: String,
        conversationId: UUID,
        maskedQuery: String,
        responseSummary: String
    ) {
        try {
            // TODO: gRPC 클라이언트로 ga-audit-service에 전송
            // val auditLog = AuditLog(...)
            // auditServiceClient.sendAuditLog(auditLog)
            
            // 현재는 로컬 로깅
            logger.info(
                "Audit Log - AI Consultation: " +
                "userId=$userId, " +
                "tenantId=$tenantId, " +
                "conversationId=$conversationId, " +
                "maskedQuery=${maskedQuery.take(50)}..., " +
                "responseSummary=${responseSummary.take(50)}..., " +
                "timestamp=${LocalDateTime.now()}"
            )
        } catch (e: Exception) {
            logger.error("감사 로그 전송 실패: ${e.message}", e)
            // 감사 로그 전송 실패는 비즈니스 로직을 중단시키지 않음
        }
    }
    
    /**
     * 인증 실패 감사 로그 기록
     * 
     * @param ipAddress IP 주소
     * @param reason 실패 사유
     */
    fun logAuthenticationFailure(
        ipAddress: String,
        reason: String
    ) {
        try {
            logger.warn(
                "Audit Log - Authentication Failure: " +
                "ipAddress=$ipAddress, " +
                "reason=$reason, " +
                "timestamp=${LocalDateTime.now()}"
            )
        } catch (e: Exception) {
            logger.error("감사 로그 전송 실패: ${e.message}", e)
        }
    }
    
    /**
     * Rate Limit 초과 감사 로그 기록
     * 
     * @param userId 사용자 ID
     * @param ipAddress IP 주소
     */
    fun logRateLimitExceeded(
        userId: String?,
        ipAddress: String
    ) {
        try {
            logger.warn(
                "Audit Log - Rate Limit Exceeded: " +
                "userId=$userId, " +
                "ipAddress=$ipAddress, " +
                "timestamp=${LocalDateTime.now()}"
            )
        } catch (e: Exception) {
            logger.error("감사 로그 전송 실패: ${e.message}", e)
        }
    }
}
