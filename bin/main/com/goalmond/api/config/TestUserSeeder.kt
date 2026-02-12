package com.goalmond.api.config

import com.goalmond.api.domain.entity.User
import com.goalmond.api.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * 로컬 프로파일 전용: 테스트용 계정이 없으면 한 건 생성.
 * application-local.yml 의 app.seed-test-user.* 로 설정 가능.
 */
@Component
@Profile("local")
@Order(Int.MAX_VALUE) // Flyway·다른 초기화 이후 실행
class TestUserSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val seedProps: TestUserSeedProperties
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (!seedProps.enabled) return
        if (userRepository.existsByEmail(seedProps.email)) return

        val user = User(
            email = seedProps.email,
            fullName = seedProps.fullName,
            passwordHash = passwordEncoder.encode(seedProps.password)
        )
        userRepository.save(user)
        // 로그는 필요 시 추가 (보안상 비밀번호는 절대 로그에 남기지 않음)
    }
}
