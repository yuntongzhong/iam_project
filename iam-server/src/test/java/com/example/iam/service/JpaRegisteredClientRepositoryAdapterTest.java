package com.example.iam.service;

import com.example.iam.domain.AppClient;
import com.example.iam.repository.AppClientRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaRegisteredClientRepositoryAdapterTest {

    private final AppClientRepository appClientRepository = mock(AppClientRepository.class);
    private final JpaRegisteredClientRepositoryAdapter adapter = new JpaRegisteredClientRepositoryAdapter(appClientRepository);

    @Test
    void toRegisteredClientShouldExposeTokenSettings() {
        AppClient appClient = AppClient.builder()
                .clientId("app-a")
                .clientSecret("{noop}secret")
                .clientName("Demo App A")
                .redirectUris(List.of("http://127.0.0.1:8081/login/oauth2/code/iam"))
                .postLogoutRedirectUris(List.of("http://127.0.0.1:8081/"))
                .scopes(List.of("openid", "profile"))
                .grantTypes(List.of("authorization_code", "refresh_token"))
                .authenticationMethods(List.of("client_secret_basic"))
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .authorizationCodeTtlMinutes(5)
                .accessTokenTtlMinutes(30)
                .refreshTokenTtlMinutes(480)
                .reuseRefreshTokens(false)
                .active(true)
                .build();
        appClient.setId(1L);

        RegisteredClient registeredClient = adapter.toRegisteredClient(appClient);

        assertEquals(Duration.ofMinutes(5), registeredClient.getTokenSettings().getAuthorizationCodeTimeToLive());
        assertEquals(Duration.ofMinutes(30), registeredClient.getTokenSettings().getAccessTokenTimeToLive());
        assertEquals(Duration.ofMinutes(480), registeredClient.getTokenSettings().getRefreshTokenTimeToLive());
        assertFalse(registeredClient.getTokenSettings().isReuseRefreshTokens());
    }

    @Test
    void saveShouldPersistTokenSettingsBackIntoEntity() {
        AppClient existing = AppClient.builder().clientId("app-a").clientName("Old").clientSecret("secret").build();
        existing.setId(1L);
        when(appClientRepository.findByClientId("app-a")).thenReturn(Optional.of(existing));

        RegisteredClient registeredClient = RegisteredClient.withId("1")
                .clientId("app-a")
                .clientSecret("encoded")
                .clientName("Demo App A")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8081/login/oauth2/code/iam")
                .postLogoutRedirectUri("http://127.0.0.1:8081/")
                .scope("openid")
                .scope("profile")
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(false).build())
                .tokenSettings(TokenSettings.builder()
                        .authorizationCodeTimeToLive(Duration.ofMinutes(7))
                        .accessTokenTimeToLive(Duration.ofMinutes(45))
                        .refreshTokenTimeToLive(Duration.ofMinutes(720))
                        .reuseRefreshTokens(true)
                        .build())
                .build();

        adapter.save(registeredClient);

        ArgumentCaptor<AppClient> captor = ArgumentCaptor.forClass(AppClient.class);
        verify(appClientRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getAuthorizationCodeTtlMinutes());
        assertEquals(45, captor.getValue().getAccessTokenTtlMinutes());
        assertEquals(720, captor.getValue().getRefreshTokenTtlMinutes());
        assertTrue(captor.getValue().getReuseRefreshTokens());
    }
}
