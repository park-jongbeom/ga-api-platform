package com.goalmond.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@SpringBootApplication(scanBasePackages = ["com.goalmond"])
class AuthServiceApplication {
    
    @Bean
    fun applicationReadyListener(env: Environment): ApplicationListener<ApplicationReadyEvent> {
        return ApplicationListener { event ->
            // #region agent log
            val logPath = "/app/logs/debug.log"
            val port = env.getProperty("server.port", "8081")
            val payload = mapOf(
                "sessionId" to "debug-session",
                "runId" to "run1",
                "hypothesisId" to "H3",
                "location" to "AuthServiceApplication.kt:applicationReadyListener",
                "message" to "service started successfully",
                "data" to mapOf(
                    "port" to port,
                    "applicationName" to env.getProperty("spring.application.name", "ga-auth-service"),
                    "timestamp" to System.currentTimeMillis()
                ),
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
            // #endregion
        }
    }
}

fun main(args: Array<String>) {
    // #region agent log
    val logPath = "/app/logs/debug.log"
    val payload = mapOf(
        "sessionId" to "debug-session",
        "runId" to "run1",
        "hypothesisId" to "H4",
        "location" to "AuthServiceApplication.kt:main",
        "message" to "application starting",
        "data" to mapOf(
            "args" to args.joinToString(","),
            "timestamp" to System.currentTimeMillis()
        ),
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
    try {
        Files.write(
            Paths.get(logPath),
            (json + System.lineSeparator()).toByteArray(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    } catch (e: Exception) {
        // 로그 파일 쓰기 실패는 무시 (서비스 시작 전일 수 있음)
    }
    // #endregion
    
    runApplication<AuthServiceApplication>(*args)
}
