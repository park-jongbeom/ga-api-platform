package com.goalmond.auth.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Component
@Order(1)
class SwaggerRequestFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI ?: ""
        val isSwaggerRequest = path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path == "/swagger-ui.html"

        // #region agent log
        try {
            val logPath = "c:\\Users\\qk54r\\ga-api-platform\\.cursor\\debug.log"
            appendDebugLog(
                logPath = logPath,
                hypothesisId = "H1",
                location = "SwaggerRequestFilter.kt:doFilterInternal.entry",
                message = "http request received",
                data = mapOf(
                    "method" to request.method,
                    "uri" to request.requestURI,
                    "isSwaggerRequest" to isSwaggerRequest,
                    "origin" to (request.getHeader("Origin") ?: "null"),
                    "host" to (request.getHeader("Host") ?: "null"),
                    "remoteAddr" to request.remoteAddr,
                    "remotePort" to request.remotePort.toString()
                )
            )
        } catch (e: Exception) {
            // 로그 실패는 무시하고 요청 처리 계속
        }
        // #endregion

        filterChain.doFilter(request, response)

        // #region agent log
        try {
            val logPath = "c:\\Users\\qk54r\\ga-api-platform\\.cursor\\debug.log"
            appendDebugLog(
                logPath = logPath,
                hypothesisId = "H2",
                location = "SwaggerRequestFilter.kt:doFilterInternal.exit",
                message = "http response prepared",
                data = mapOf(
                    "method" to request.method,
                    "uri" to request.requestURI,
                    "status" to response.status,
                    "isSwaggerRequest" to isSwaggerRequest,
                    "allowOrigin" to (response.getHeader("Access-Control-Allow-Origin") ?: "null")
                )
            )
        } catch (e: Exception) {
            // 로그 실패는 무시
        }
        // #endregion
    }

    private fun appendDebugLog(
        logPath: String,
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?>
    ) {
        val payload = mapOf(
            "sessionId" to "debug-session",
            "runId" to "run1",
            "hypothesisId" to hypothesisId,
            "location" to location,
            "message" to message,
            "data" to data,
            "timestamp" to System.currentTimeMillis()
        )
        val json = payload.entries.joinToString(
            prefix = "{",
            postfix = "}"
        ) { (k, v) ->
            val value = when (v) {
                null -> "null"
                is String -> "\"${v.replace("\"", "\\\"")}\""
                is Map<*, *> -> v.entries.joinToString(prefix = "{", postfix = "}") { (mk, mv) ->
                    val mvValue = if (mv == null) "null" else "\"${mv.toString().replace("\"", "\\\"")}\""
                    "\"$mk\":$mvValue"
                }
                else -> v.toString()
            }
            "\"$k\":$value"
        }
        Files.write(
            Paths.get(logPath),
            (json + System.lineSeparator()).toByteArray(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }
}
