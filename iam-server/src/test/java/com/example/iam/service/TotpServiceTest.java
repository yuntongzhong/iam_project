package com.example.iam.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void shouldGenerateSecretAndQrCode() {
        String secret = totpService.generateSecret();
        String qrCode = totpService.generateQrCodeBase64("alice", secret);

        assertNotNull(secret);
        assertTrue(!secret.isBlank());
        assertNotNull(qrCode);
        assertTrue(!qrCode.isBlank());
    }

    @Test
    void shouldRejectInvalidCode() {
        String secret = totpService.generateSecret();
        assertFalse(totpService.verifyCode(secret, "000000"));
    }
}
