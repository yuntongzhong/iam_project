package com.example.iam.security;

public record AuditTarget(
        String targetType,
        String targetId,
        String targetName
) {

    public static AuditTarget none() {
        return new AuditTarget(null, null, null);
    }
}
