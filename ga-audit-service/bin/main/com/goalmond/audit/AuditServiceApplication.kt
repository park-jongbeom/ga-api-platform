package com.goalmond.audit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.goalmond"])
class AuditServiceApplication

fun main(args: Array<String>) {
    runApplication<AuditServiceApplication>(*args)
}
