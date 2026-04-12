package com.example.iam.web.dto;

import java.util.List;
import java.util.Set;

public record DepartmentResponse(
        Long id,
        String name,
        Long parentId,
        Set<String> roleCodes,
        List<DepartmentResponse> children
) {
}
