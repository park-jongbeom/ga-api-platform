package com.goalmond.auth.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Component
class SwaggerDebugFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI ?: ""
        val isSwaggerRequest = path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path == "/swagger-ui.html"

        if (isSwaggerRequest) {
            // #region agent log
            appendDebugLog(
                hypothesisId = "H1",
                location = "SwaggerDebugFilter.kt:doFilterInternal.entry",
                message = "swagger request received",
                data = mapOf(
                    "method" to request.method,
                    "uri" to request.requestURI,
                    "origin" to request.getHeader("Origin"),
                    "host" to request.getHeader("Host")
                )
            )
            // #endregion
        }

        filterChain.doFilter(request, response)

        if (isSwaggerRequest) {
            // #region agent log
            appendDebugLog(
                hypothesisId = "H2",
                location = "SwaggerDebugFilter.kt:doFilterInternal.exit",
                message = "swagger response prepared",
                data = mapOf(
                    "status" to response.status,
                    "allowOrigin" to response.getHeader("Access-Control-Allow-Origin")
                )
            )
            // #endregion
        }
    }

    private fun appendDebugLog(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?>
    ) {
        val logPath = "c:\\Users\\qk54r\\ga-api-platform\\.cursor\\debug.log"
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
