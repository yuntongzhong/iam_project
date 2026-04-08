package com.example.iam.web.dto;

import com.example.iam.domain.enums.AuditStatus;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String username,
        String action,
        String ipAddress,
        LocalDateTime timestamp,
        AuditStatus status,
        String details
) {
}
