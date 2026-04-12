package com.example.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record RoleRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        Set<Long> permissionIds
) {
}
