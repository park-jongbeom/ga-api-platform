package com.goalmond.ai.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate Limiting 설정
 * 
 * Bucket4j를 사용하여 IP/사용자별 요청 제한을 구현합니다.
 * LLM API 비용 절감 및 악용 방지를 목적으로 합니다.
 * 
 * 보안 준수 항목:
 * - Rate Limit: API 호출 빈도 제한
 * - Brute Force 방어: 무차별 대입 공격 방어
 * 
 * 참고:
 * - Bucket4j: https://bucket4j.com/
 */
@Configuration
class RateLimitConfig {
    
    @Value("\${rate-limit.per-minute}")
    private var perMinute: Long = 10
    
    @Value("\${rate-limit.per-hour}")
    private var perHour: Long = 100
    
    @Value("\${rate-limit.per-day}")
    private var perDay: Long = 500
    
    /**
     * 사용자별 Bucket 캐시
     */
    private val bucketCache = ConcurrentHashMap<String, Bucket>()
    
    /**
     * 사용자별 Rate Limiter 조회 또는 생성
     * 
     * @param userId 사용자 ID
     * @return Bucket 인스턴스
     */
    fun resolveBucket(userId: String): Bucket {
        return bucketCache.computeIfAbsent(userId) { createNewBucket() }
    }
    
    /**
     * IP별 Rate Limiter 조회 또는 생성
     * 
     * @param ipAddress IP 주소
     * @return Bucket 인스턴스
     */
    fun resolveBucketByIp(ipAddress: String): Bucket {
        return bucketCache.computeIfAbsent("ip:$ipAddress") { createNewBucket() }
    }
    
    /**
     * 새 Bucket 생성
     */
    private fun createNewBucket(): Bucket {
        // 1분당 제한
        val perMinuteLimit = Bandwidth.classic(
            perMinute,
            Refill.intervally(perMinute, Duration.ofMinutes(1))
        )
        
        // 1시간당 제한
        val perHourLimit = Bandwidth.classic(
            perHour,
            Refill.intervally(perHour, Duration.ofHours(1))
        )
        
        // 1일당 제한
        val perDayLimit = Bandwidth.classic(
            perDay,
            Refill.intervally(perDay, Duration.ofDays(1))
        )
        
        return Bucket.builder()
            .addLimit(perMinuteLimit)
            .addLimit(perHourLimit)
            .addLimit(perDayLimit)
            .build()
    }
    
    /**
     * 캐시 정리 (선택적)
     */
    fun clearCache() {
        bucketCache.clear()
    }
}

/**
 * Rate Limit 예외
 */
class RateLimitExceededException(message: String) : RuntimeException(message)
