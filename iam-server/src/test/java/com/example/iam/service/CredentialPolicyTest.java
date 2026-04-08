package com.example.iam.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialPolicyTest {

    private final CredentialPolicy credentialPolicy = new CredentialPolicy();

    @Test
    void shouldAcceptStrongPassword() {
        assertDoesNotThrow(() -> credentialPolicy.validatePassword("password", "Admin#2026!Secure"));
    }

    @Test
    void shouldRejectWeakPassword() {
        assertThrows(IllegalArgumentException.class, () -> credentialPolicy.validatePassword("password", "Admin123"));
    }

    @Test
    void shouldRejectWeakClientSecret() {
        assertThrows(IllegalArgumentException.class, () -> credentialPolicy.validateClientSecret("clientSecret", "short-secret"));
    }
}
