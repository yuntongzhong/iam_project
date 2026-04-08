package com.example.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record DepartmentRequest(
        @NotBlank String name,
        Long parentId,
        Set<Long> roleIds
) {
}
