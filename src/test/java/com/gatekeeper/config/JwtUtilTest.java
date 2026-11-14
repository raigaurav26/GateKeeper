package com.gatekeeper.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("test-secret-test-secret-test-secret-123", 60_000);

    @Test
    void generatedTokenIsValidAndCarriesUsername() {
        String token = jwtUtil.generateToken("admin", "ADMIN");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken("admin", "ADMIN");
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void sameSecretValidatesTokensAcrossInstances() {
        String secret = "another-secret-another-secret-12345678";
        String token = new JwtUtil(secret, 60_000).generateToken("admin", "ADMIN");

        assertThat(new JwtUtil(secret, 60_000).validateToken(token)).isTrue();
        assertThat(new JwtUtil("different-secret-different-secret-1234", 60_000).validateToken(token)).isFalse();
    }

    @Test
    void blankSecretFallsBackToRandomKeyPerInstance() {
        String token = new JwtUtil("", 60_000).generateToken("admin", "ADMIN");

        assertThat(new JwtUtil("", 60_000).validateToken(token)).isFalse();
    }

    @Test
    void shortSecretIsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new JwtUtil("too-short", 60_000));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = new JwtUtil("test-secret-test-secret-test-secret-123", -1000).generateToken("admin", "ADMIN");

        assertThat(new JwtUtil("test-secret-test-secret-test-secret-123", 60_000).validateToken(token)).isFalse();
    }
}
