package com.goalmond.common.exception

/**
 * 비즈니스 로직 예외
 */
open class BusinessException(
    message: String,
    val code: String? = null
) : RuntimeException(message)

class NotFoundException(
    message: String = "요청한 리소스를 찾을 수 없습니다.",
    code: String = "NOT_FOUND"
) : BusinessException(message, code)

class UnauthorizedException(
    message: String = "인증이 필요합니다.",
    code: String = "UNAUTHORIZED"
) : BusinessException(message, code)

class ForbiddenException(
    message: String = "접근 권한이 없습니다.",
    code: String = "FORBIDDEN"
) : BusinessException(message, code)
