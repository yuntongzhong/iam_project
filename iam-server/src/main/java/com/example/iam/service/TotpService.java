package com.example.iam.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TotpService {

    private static final String ISSUER = "IAM-Platform";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final TimeProvider timeProvider = new SystemTimeProvider();

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String buildOtpAuthUrl(String username, String secret) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                urlEncode(ISSUER),
                urlEncode(username),
                secret,
                urlEncode(ISSUER)
        );
    }

    public String generateQrCodeBase64(String username, String secret) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(buildOtpAuthUrl(username, secret), BarcodeFormat.QR_CODE, 280, 280);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate QR code", ex);
        }
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long bucket = timeProvider.getTime() / 30;
        return matches(secret, code, bucket) || matches(secret, code, bucket - 1) || matches(secret, code, bucket + 1);
    }

    private boolean matches(String secret, String code, long bucket) {
        try {
            return code.equals(codeGenerator.generate(secret, bucket));
        } catch (Exception ex) {
            return false;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
