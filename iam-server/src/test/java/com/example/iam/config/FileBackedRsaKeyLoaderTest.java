package com.example.iam.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBackedRsaKeyLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReuseExistingJwkFile() {
        AppSecurityProperties properties = new AppSecurityProperties();
        Path jwkPath = tempDir.resolve("keys").resolve("iam-signing.jwk.json");
        properties.setJwkPath(jwkPath.toString());

        FileBackedRsaKeyLoader loader = new FileBackedRsaKeyLoader(properties);

        RSAKey first = loader.loadOrCreate();
        RSAKey second = loader.loadOrCreate();

        assertTrue(Files.exists(jwkPath));
        assertEquals(first.getKeyID(), second.getKeyID());
        assertEquals(first.toPublicJWK().toJSONString(), second.toPublicJWK().toJSONString());
    }
}
