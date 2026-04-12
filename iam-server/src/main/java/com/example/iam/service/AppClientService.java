package com.example.iam.service;

import com.example.iam.domain.AppClient;
import com.example.iam.repository.AppClientRepository;
import com.example.iam.web.dto.AppClientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppClientService {

    private final AppClientRepository appClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialPolicy credentialPolicy;

    public List<AppClient> findAll() {
        return appClientRepository.findAll();
    }

    public AppClient findById(Long id) {
        return appClientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("App client not found: " + id));
    }

    public AppClient create(AppClientRequest request) {
        AppClient appClient = new AppClient();
        apply(appClient, request, true);
        return appClientRepository.save(appClient);
    }

    public AppClient update(Long id, AppClientRequest request) {
        AppClient appClient = findById(id);
        apply(appClient, request, false);
        return appClientRepository.save(appClient);
    }

    public void delete(Long id) {
        appClientRepository.deleteById(id);
    }

    private void apply(AppClient appClient, AppClientRequest request, boolean creating) {
        appClient.setClientId(request.clientId());
        appClient.setClientName(request.clientName());
        if (creating && (request.clientSecret() == null || request.clientSecret().isBlank())) {
            throw new IllegalArgumentException("clientSecret is required when creating an app client");
        }
        if (creating || (request.clientSecret() != null && !request.clientSecret().isBlank())) {
            credentialPolicy.validateClientSecret("clientSecret", request.clientSecret());
            appClient.setClientSecret(passwordEncoder.encode(request.clientSecret()));
        }
        appClient.setRedirectUris(normalize(request.redirectUris()));
        appClient.setPostLogoutRedirectUris(normalize(request.postLogoutRedirectUris()));
        appClient.setScopes(normalize(request.scopes()));
        appClient.setGrantTypes(normalize(request.grantTypes()));
        appClient.setAuthenticationMethods(normalize(request.authenticationMethods()));
        appClient.setRequireProofKey(request.requireProofKey() != null ? request.requireProofKey() : Boolean.TRUE.equals(appClient.getRequireProofKey()));
        appClient.setRequireAuthorizationConsent(request.requireAuthorizationConsent() != null
                ? request.requireAuthorizationConsent()
                : Boolean.TRUE.equals(appClient.getRequireAuthorizationConsent()));
        appClient.setAuthorizationCodeTtlMinutes(normalizeDuration(request.authorizationCodeTtlMinutes(), appClient.getAuthorizationCodeTtlMinutes(), 5));
        appClient.setAccessTokenTtlMinutes(normalizeDuration(request.accessTokenTtlMinutes(), appClient.getAccessTokenTtlMinutes(), 30));
        appClient.setRefreshTokenTtlMinutes(normalizeDuration(request.refreshTokenTtlMinutes(), appClient.getRefreshTokenTtlMinutes(), 480));
        appClient.setReuseRefreshTokens(request.reuseRefreshTokens() != null
                ? request.reuseRefreshTokens()
                : Boolean.TRUE.equals(appClient.getReuseRefreshTokens()));
        appClient.setActive(request.active() != null ? request.active() : appClient.getActive() == null || appClient.getActive());
    }

    private List<String> normalize(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private Integer normalizeDuration(Integer requested, Integer existing, int fallback) {
        if (requested != null && requested > 0) {
            return requested;
        }
        if (existing != null && existing > 0) {
            return existing;
        }
        return fallback;
    }
}
