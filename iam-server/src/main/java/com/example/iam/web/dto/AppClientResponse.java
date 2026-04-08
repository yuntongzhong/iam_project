package com.example.iam.web.dto;

import java.util.List;

public record AppClientResponse(
        Long id,
        String clientId,
        String clientName,
        List<String> redirectUris,
        List<String> postLogoutRedirectUris,
        List<String> scopes,
        List<String> grantTypes,
        List<String> authenticationMethods,
        Boolean requireProofKey,
        Boolean requireAuthorizationConsent,
        Boolean active
) {
}
