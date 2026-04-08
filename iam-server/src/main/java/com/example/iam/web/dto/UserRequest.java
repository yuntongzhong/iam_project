package com.example.iam.web.dto;

import com.example.iam.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UserRequest(
        @NotBlank String username,
        String password,
        @Email @NotBlank String email,
        @NotBlank String phone,
        UserStatus status,
        Long departmentId,
        Set<Long> roleIds
) {
}
