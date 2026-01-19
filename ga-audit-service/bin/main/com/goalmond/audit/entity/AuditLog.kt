package com.goalmond.audit.entity

import com.goalmond.common.entity.AuditEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "audit_logs")
class AuditLog : AuditEntity()
