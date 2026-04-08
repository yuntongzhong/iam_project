package com.example.iam.web.dto;

import jakarta.validation.constraints.Pattern;

public record MfaCodeRequest(
        @Pattern(regexp = "\\d{6}") String code
) {
    public MfaCodeRequest {
        if (code != null) {
            code = normalizeDigits(code).replaceAll("\\s+", "");
        }
    }

    private static String normalizeDigits(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            if (ch >= '０' && ch <= '９') {
                normalized.append((char) ('0' + (ch - '０')));
            } else {
                normalized.append(ch);
            }
        }
        return normalized.toString();
    }
}
