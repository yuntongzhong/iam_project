package com.example.iam.web.dto;

public record UserImportSummaryResponse(
        int totalRows,
        int successRows,
        int warningRows,
        int errorRows,
        boolean committed
) {
}
