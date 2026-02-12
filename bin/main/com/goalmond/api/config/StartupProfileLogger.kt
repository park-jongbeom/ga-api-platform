package com.goalmond.api.config

import com.goalmond.api.controller.AuthController
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 404 Auth API 검증용: 기동 시 active profiles와 AuthController 빈 등록 여부를 로그로 남김.
 * docs/404_AUTH_VERIFICATION_PLAN.md 참고.
 */
@Component
class StartupProfileLogger(
    private val env: Environment,
    private val applicationContext: ApplicationContext
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        // #region agent log
        val activeProfiles = env.activeProfiles.toList()
        val hasAuthController = runCatching {
            applicationContext.getBean(AuthController::class.java)
        }.isSuccess
        log.info(
            "[404-verify] activeProfiles={}, AuthController.registered={}, " +
                "expected_for_auth_api=lightsail_or_local",
            activeProfiles.ifEmpty { listOf("default") },
            hasAuthController
        )
        // #endregion
    }
}
