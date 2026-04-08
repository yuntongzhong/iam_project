package com.example.iam.service;

import com.example.iam.domain.AppClient;
import com.example.iam.repository.AppClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JpaRegisteredClientRepositoryAdapter implements RegisteredClientRepository {

    private final AppClientRepository appClientRepository;

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        AppClient entity = appClientRepository.findByClientId(registeredClient.getClientId())
                .orElseGet(AppClient::new);
        entity.setClientId(registeredClient.getClientId());
        entity.setClientSecret(registeredClient.getClientSecret());
        entity.setClientName(registeredClient.getClientName());
        entity.setRedirectUris(List.copyOf(registeredClient.getRedirectUris()));
        entity.setPostLogoutRedirectUris(List.copyOf(registeredClient.getPostLogoutRedirectUris()));
        entity.setScopes(List.copyOf(registeredClient.getScopes()));
        entity.setGrantTypes(registeredClient.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).toList());
        entity.setAuthenticationMethods(registeredClient.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::getValue).toList());
        entity.setRequireProofKey(registeredClient.getClientSettings().isRequireProofKey());
        entity.setRequireAuthorizationConsent(registeredClient.getClientSettings().isRequireAuthorizationConsent());
        entity.setActive(true);
        appClientRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        if (id == null) {
            return null;
        }
        try {
            Long parsedId = Long.parseLong(id);
            return appClientRepository.findById(parsedId).map(this::toRegisteredClient).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        return appClientRepository.findByClientId(clientId)
                .filter(appClient -> Boolean.TRUE.equals(appClient.getActive()))
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    public RegisteredClient toRegisteredClient(AppClient appClient) {
        RegisteredClient.Builder builder = RegisteredClient.withId(String.valueOf(appClient.getId()))
                .clientId(appClient.getClientId())
                .clientSecret(appClient.getClientSecret())
                .clientName(appClient.getClientName());

        appClient.getAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::new)
                .forEach(builder::clientAuthenticationMethod);
        appClient.getGrantTypes().stream()
                .map(AuthorizationGrantType::new)
                .forEach(builder::authorizationGrantType);
        appClient.getRedirectUris().forEach(builder::redirectUri);
        appClient.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        appClient.getScopes().forEach(builder::scope);
        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(Boolean.TRUE.equals(appClient.getRequireProofKey()))
                .requireAuthorizationConsent(Boolean.TRUE.equals(appClient.getRequireAuthorizationConsent()))
                .build());
        return builder.build();
    }
}
