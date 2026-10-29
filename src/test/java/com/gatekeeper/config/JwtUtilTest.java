package com.gatekeeper.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

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
}
