package com.example.iam.service;

import org.springframework.stereotype.Component;

@Component
public class CredentialPolicy {

    public void validatePassword(String label, String password) {
        validateSecret(label, password, 12);
    }

    public void validateClientSecret(String label, String clientSecret) {
        validateSecret(label, clientSecret, 16);
    }

    private void validateSecret(String label, String value, int minLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        if (value.length() < minLength) {
            throw new IllegalArgumentException(label + " must be at least " + minLength + " characters long");
        }
        if (!value.matches(".*[A-Z].*")
                || !value.matches(".*[a-z].*")
                || !value.matches(".*\\d.*")
                || value.matches("[A-Za-z0-9]*")) {
            throw new IllegalArgumentException(label + " must contain upper, lower, digit and special characters");
        }
    }
}
