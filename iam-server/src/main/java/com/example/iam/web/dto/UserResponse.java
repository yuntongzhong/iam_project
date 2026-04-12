package com.example.iam.web.dto;

import com.example.iam.domain.enums.UserStatus;

import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        UserStatus status,
        Integer failedLoginAttempts,
        Boolean totpEnabled,
        Long departmentId,
        String departmentName,
        Set<String> roleCodes,
        Set<String> permissionCodes
) {
}
