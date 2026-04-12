package com.example.iam.config;

import com.example.iam.repository.AppClientRepository;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CredentialUpgradeInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AppClientRepository appClientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        ensureSeedUserPassword(DemoCredentialCatalog.ADMIN_USERNAME, DemoCredentialCatalog.ADMIN_PASSWORD);
        ensureSeedUserPassword(DemoCredentialCatalog.ALICE_USERNAME, DemoCredentialCatalog.ALICE_PASSWORD);
        ensureSeedUserPassword(DemoCredentialCatalog.BOB_USERNAME, DemoCredentialCatalog.BOB_PASSWORD);

        ensureClientConfiguration(DemoCredentialCatalog.APP_A_CLIENT_ID, DemoCredentialCatalog.APP_A_CLIENT_SECRET);
        ensureClientConfiguration(DemoCredentialCatalog.APP_B_CLIENT_ID, DemoCredentialCatalog.APP_B_CLIENT_SECRET);
    }

    private void ensureSeedUserPassword(String username, String strongPassword) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (!passwordEncoder.matches(strongPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(strongPassword));
                userRepository.save(user);
                log.info("Ensured secure password for seed user {}", username);
            }
        });
    }

    private void ensureClientConfiguration(String clientId, String strongSecret) {
        appClientRepository.findByClientId(clientId).ifPresent(appClient -> {
            boolean dirty = false;
            if (!passwordEncoder.matches(strongSecret, appClient.getClientSecret())) {
                appClient.setClientSecret(passwordEncoder.encode(strongSecret));
                dirty = true;
            }
            if (appClient.getAuthorizationCodeTtlMinutes() == null || appClient.getAuthorizationCodeTtlMinutes() <= 0) {
                appClient.setAuthorizationCodeTtlMinutes(5);
                dirty = true;
            }
            if (appClient.getAccessTokenTtlMinutes() == null || appClient.getAccessTokenTtlMinutes() <= 0) {
                appClient.setAccessTokenTtlMinutes(30);
                dirty = true;
            }
            if (appClient.getRefreshTokenTtlMinutes() == null || appClient.getRefreshTokenTtlMinutes() <= 0) {
                appClient.setRefreshTokenTtlMinutes(480);
                dirty = true;
            }
            if (appClient.getReuseRefreshTokens() == null) {
                appClient.setReuseRefreshTokens(Boolean.FALSE);
                dirty = true;
            }
            if (dirty) {
                appClientRepository.save(appClient);
                log.info("Ensured secure client configuration for {}", clientId);
            }
        });
    }
}
