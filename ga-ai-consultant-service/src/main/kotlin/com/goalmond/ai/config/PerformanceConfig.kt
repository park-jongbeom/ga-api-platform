package com.goalmond.ai.config

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * 성능 모니터링 설정
 * 
 * AI 상담 파이프라인의 각 단계별 실행 시간을 측정합니다.
 * 
 * 측정 지표:
 * - 입력 검증 시간
 * - 마스킹 처리 시간
 * - RAG 검색 시간
 * - LLM 호출 시간
 * - 전체 파이프라인 시간
 * 
 * @author AI Consultant Team
 * @since 2026-01-21
 */
@Configuration
@EnableAspectJAutoProxy
class PerformanceConfig {
    
    /**
     * @Timed 어노테이션 지원
     */
    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect {
        return TimedAspect(registry)
    }
}

/**
 * 성능 측정 Aspect
 * 
 * AI 상담 서비스의 주요 메서드 실행 시간을 자동으로 측정합니다.
 */
@Aspect
@Configuration
class ConsultantPerformanceAspect(
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * AI 상담 처리 성능 측정
     */
    @Around("execution(* com.goalmond.ai.service.ConsultantService.processChat(..))")
    fun measureChatProcessing(joinPoint: ProceedingJoinPoint): Any? {
        val timer = Timer.builder("consultant.chat.processing")
            .description("AI 상담 전체 파이프라인 실행 시간")
            .tag("method", "processChat")
            .register(meterRegistry)
        
        return timer.record<Any?> {
            val startTime = System.currentTimeMillis()
            try {
                joinPoint.proceed().also {
                    val duration = System.currentTimeMillis() - startTime
                    logger.info("AI 상담 처리 완료: ${duration}ms")
                    
                    // 목표 시간 초과 시 경고
                    if (duration > 3000) {
                        logger.warn("⚠️ AI 상담 처리 시간 초과: ${duration}ms (목표: < 3000ms)")
                    }
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                logger.error("AI 상담 처리 실패: ${duration}ms", e)
                
                // 오류 카운터
                meterRegistry.counter(
                    "consultant.chat.errors",
                    "error_type", e.javaClass.simpleName
                ).increment()
                
                throw e
            }
        }
    }
    
    /**
     * RAG 검색 성능 측정
     */
    @Around("execution(* com.goalmond.ai.service.RagService.searchSimilarDocuments(..))")
    fun measureRagSearch(joinPoint: ProceedingJoinPoint): Any? {
        val timer = Timer.builder("rag.search.duration")
            .description("RAG 문서 검색 실행 시간")
            .tag("method", "searchSimilarDocuments")
            .register(meterRegistry)
        
        return timer.record<Any?> {
            val startTime = System.currentTimeMillis()
            try {
                (joinPoint.proceed() as List<*>).also { results ->
                    val duration = System.currentTimeMillis() - startTime
                    logger.debug("RAG 검색 완료: ${duration}ms, ${results.size}개 문서")
                    
                    // 검색된 문서 수 게이지
                    meterRegistry.gauge(
                        "rag.search.documents.found",
                        results.size
                    )
                    
                    // 목표 시간 초과 시 경고
                    if (duration > 150) {
                        logger.warn("⚠️ RAG 검색 시간 초과: ${duration}ms (목표: < 150ms)")
                    }
                }
            } catch (e: Exception) {
                logger.error("RAG 검색 실패", e)
                meterRegistry.counter("rag.search.errors").increment()
                throw e
            }
        }
    }
    
    /**
     * 마스킹 처리 성능 측정
     */
    @Around("execution(* com.goalmond.ai.service.MaskingService.maskSensitiveData(..))")
    fun measureMasking(joinPoint: ProceedingJoinPoint): Any? {
        val timer = Timer.builder("masking.duration")
            .description("민감정보 마스킹 처리 시간")
            .register(meterRegistry)
        
        return timer.record<Any?> {
            val startTime = System.currentTimeMillis()
            joinPoint.proceed().also {
                val duration = System.currentTimeMillis() - startTime
                logger.debug("마스킹 완료: ${duration}ms")
                
                if (duration > 50) {
                    logger.warn("⚠️ 마스킹 처리 시간 초과: ${duration}ms (목표: < 50ms)")
                }
            }
        }
    }
    
    /**
     * 프롬프트 템플릿 생성 성능 측정
     */
    @Around("execution(* com.goalmond.ai.domain.prompt.PromptTemplateManager.createPrompt(..))")
    fun measurePromptCreation(joinPoint: ProceedingJoinPoint): Any? {
        val timer = Timer.builder("prompt.template.creation")
            .description("프롬프트 템플릿 생성 시간")
            .register(meterRegistry)
        
        return timer.record<Any?> {
            joinPoint.proceed()
        }
    }
}
