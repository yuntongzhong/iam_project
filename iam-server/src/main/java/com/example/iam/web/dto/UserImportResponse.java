package com.example.iam.web.dto;

import java.util.List;

public record UserImportResponse(
        UserImportSummaryResponse summary,
        List<UserImportRowResponse> rows
) {
}
