package com.example.iam.web.dto;

import java.util.List;

public record UserImportRowResponse(
        int rowNumber,
        String username,
        String operation,
        String outcome,
        List<String> warnings,
        List<String> errors
) {
}
