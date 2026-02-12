package com.goalmond.api.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
@org.springframework.context.annotation.Profile("local", "lightsail")
class JwtUtil(
    @Value("\${app.jwt.secret:defaultSecretKeyForLocalDevelopmentOnlyChangeInProduction}")
    private val secret: String,
    @Value("\${app.jwt.expiration-ms:86400000}") // 24시간
    private val expirationMs: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.padEnd(32, '0').take(32).toByteArray(Charsets.UTF_8))
    }

    fun generateToken(userId: String, email: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun extractUserId(token: String): String? = extractClaims(token)?.subject

    fun isValid(token: String): Boolean {
        return try {
            val claims = extractClaims(token) ?: return false
            claims.expiration.after(Date())
        } catch (_: Exception) {
            false
        }
    }

    private fun extractClaims(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: Exception) {
            null
        }
    }
}
