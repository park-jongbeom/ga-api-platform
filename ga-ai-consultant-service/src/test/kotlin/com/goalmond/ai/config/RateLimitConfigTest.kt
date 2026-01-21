package com.goalmond.ai.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * RateLimitConfig 테스트
 * 
 * Rate Limiting 기능을 검증합니다.
 */
class RateLimitConfigTest {

    private lateinit var rateLimitConfig: RateLimitConfig

    @BeforeEach
    fun setUp() {
        rateLimitConfig = RateLimitConfig()
    }

    @Test
    fun `사용자별 Bucket 생성 테스트`() {
        // Given
        val userId = "user-123"
        
        // When
        val bucket1 = rateLimitConfig.resolveBucket(userId)
        val bucket2 = rateLimitConfig.resolveBucket(userId)
        
        // Then
        assertNotNull(bucket1)
        assertSame(bucket1, bucket2) // 동일한 사용자는 같은 Bucket 사용
    }

    @Test
    fun `다른 사용자는 다른 Bucket 사용 테스트`() {
        // Given
        val userId1 = "user-123"
        val userId2 = "user-456"
        
        // When
        val bucket1 = rateLimitConfig.resolveBucket(userId1)
        val bucket2 = rateLimitConfig.resolveBucket(userId2)
        
        // Then
        assertNotSame(bucket1, bucket2)
    }

    @Test
    fun `IP별 Bucket 생성 테스트`() {
        // Given
        val ipAddress = "192.168.1.1"
        
        // When
        val bucket1 = rateLimitConfig.resolveBucketByIp(ipAddress)
        val bucket2 = rateLimitConfig.resolveBucketByIp(ipAddress)
        
        // Then
        assertNotNull(bucket1)
        assertSame(bucket1, bucket2)
    }

    @Test
    fun `Rate Limit 초과 테스트`() {
        // Given
        val userId = "user-123"
        val bucket = rateLimitConfig.resolveBucket(userId)
        
        // When - 제한 횟수만큼 토큰 소비
        var successCount = 0
        repeat(15) { // 분당 10회 제한을 초과하도록 시도
            if (bucket.tryConsume(1)) {
                successCount++
            }
        }
        
        // Then - 제한 횟수 이상은 거부됨
        assertTrue(successCount <= 10) // 분당 10회 제한
    }

    @Test
    fun `캐시 정리 테스트`() {
        // Given
        val userId1 = "user-123"
        val userId2 = "user-456"
        rateLimitConfig.resolveBucket(userId1)
        rateLimitConfig.resolveBucket(userId2)
        
        // When
        rateLimitConfig.clearCache()
        
        // Then
        val newBucket1 = rateLimitConfig.resolveBucket(userId1)
        val newBucket2 = rateLimitConfig.resolveBucket(userId2)
        
        assertNotNull(newBucket1)
        assertNotNull(newBucket2)
    }

    @Test
    fun `RateLimitExceededException 생성 테스트`() {
        // When
        val exception = RateLimitExceededException("요청 한도 초과")
        
        // Then
        assertEquals("요청 한도 초과", exception.message)
        assertTrue(exception is RuntimeException)
    }
}
