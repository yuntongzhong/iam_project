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

        ensureClientSecret(DemoCredentialCatalog.APP_A_CLIENT_ID, DemoCredentialCatalog.APP_A_CLIENT_SECRET);
        ensureClientSecret(DemoCredentialCatalog.APP_B_CLIENT_ID, DemoCredentialCatalog.APP_B_CLIENT_SECRET);
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

    private void ensureClientSecret(String clientId, String strongSecret) {
        appClientRepository.findByClientId(clientId).ifPresent(appClient -> {
            if (!passwordEncoder.matches(strongSecret, appClient.getClientSecret())) {
                appClient.setClientSecret(passwordEncoder.encode(strongSecret));
                appClientRepository.save(appClient);
                log.info("Ensured secure client secret for {}", clientId);
            }
        });
    }
}
