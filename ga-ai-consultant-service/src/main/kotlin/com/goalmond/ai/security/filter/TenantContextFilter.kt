package com.goalmond.ai.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 테넌트 컨텍스트 필터
 * 
 * JWT에서 추출한 테넌트 ID를 ThreadLocal에 저장하여 요청 전체에서 사용 가능하게 합니다.
 * 멀티테넌트 환경에서 데이터 격리를 보장합니다.
 * 
 * 보안 준수 항목:
 * - 테넌트 격리: 각 요청마다 테넌트 ID 분리
 * - ABAC: 속성 기반 접근 제어 (테넌트 속성)
 */
@Component
class TenantContextFilter : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // 1. SecurityContext에서 테넌트 ID 추출
            val authentication = SecurityContextHolder.getContext().authentication
            
            if (authentication != null && authentication.isAuthenticated) {
                val details = authentication.details as? Map<*, *>
                val tenantId = details?.get("tenantId") as? String
                
                if (tenantId != null) {
                    // 2. ThreadLocal에 테넌트 ID 저장
                    TenantContext.setTenantId(tenantId)
                    logger.debug("테넌트 컨텍스트 설정: $tenantId")
                } else {
                    logger.warn("JWT에 테넌트 ID가 없습니다.")
                }
            }
            
            filterChain.doFilter(request, response)
        } finally {
            // 3. 요청 완료 후 ThreadLocal 정리 (메모리 누수 방지)
            TenantContext.clear()
        }
    }
}

/**
 * 테넌트 컨텍스트 ThreadLocal 관리
 * 
 * 스레드별로 독립적인 테넌트 ID를 저장합니다.
 */
object TenantContext {
    private val tenantIdHolder = ThreadLocal<String>()
    
    /**
     * 현재 스레드의 테넌트 ID 설정
     */
    fun setTenantId(tenantId: String) {
        tenantIdHolder.set(tenantId)
    }
    
    /**
     * 현재 스레드의 테넌트 ID 조회
     */
    fun getTenantId(): String? {
        return tenantIdHolder.get()
    }
    
    /**
     * 현재 스레드의 테넌트 ID 필수 조회
     * 
     * @throws IllegalStateException 테넌트 ID가 없는 경우
     */
    fun requireTenantId(): String {
        return getTenantId() ?: throw IllegalStateException("테넌트 ID가 설정되지 않았습니다.")
    }
    
    /**
     * ThreadLocal 정리
     */
    fun clear() {
        tenantIdHolder.remove()
    }
}
