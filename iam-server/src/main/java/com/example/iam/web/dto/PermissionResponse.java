package com.example.iam.web.dto;

import com.example.iam.domain.enums.PermissionType;

public record PermissionResponse(
        Long id,
        String code,
        String name,
        String description,
        PermissionType permissionType,
        String resource,
        Long appClientId
) {
}
