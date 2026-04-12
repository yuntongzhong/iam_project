package com.example.iam.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank
        @Size(min = 8, max = 64)
        String newPassword
) {
}
