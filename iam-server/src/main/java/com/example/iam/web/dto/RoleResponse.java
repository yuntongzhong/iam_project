package com.example.iam.web.dto;

import java.util.Set;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        Set<PermissionResponse> permissions
) {
}
