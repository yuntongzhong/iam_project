package com.example.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AppClientRequest(
        @NotBlank String clientId,
        String clientSecret,
        @NotBlank String clientName,
        List<String> redirectUris,
        List<String> postLogoutRedirectUris,
        List<String> scopes,
        List<String> grantTypes,
        List<String> authenticationMethods,
        Boolean requireProofKey,
        Boolean requireAuthorizationConsent,
        Integer authorizationCodeTtlMinutes,
        Integer accessTokenTtlMinutes,
        Integer refreshTokenTtlMinutes,
        Boolean reuseRefreshTokens,
        Boolean active
) {
}
