package com.example.iam.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileBackedRsaKeyLoader {

    private final AppSecurityProperties appSecurityProperties;

    public RSAKey loadOrCreate() {
        Path path = resolvePath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(path)) {
                return (RSAKey) JWK.parse(Files.readString(path, StandardCharsets.UTF_8));
            }
            RSAKey key = generateRsaKey();
            Files.writeString(path, key.toJSONString(), StandardCharsets.UTF_8);
            return key;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load signing key from " + path, ex);
        }
    }

    private Path resolvePath() {
        if (StringUtils.hasText(appSecurityProperties.getJwkPath())) {
            return Paths.get(appSecurityProperties.getJwkPath()).toAbsolutePath().normalize();
        }
        return Paths.get(".runtime", "keys", "iam-signing.jwk.json").toAbsolutePath().normalize();
    }

    private RSAKey generateRsaKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
