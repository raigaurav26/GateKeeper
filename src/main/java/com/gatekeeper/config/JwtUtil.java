package com.gatekeeper.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;
    private final long expiration;

    /**
     * The signing secret comes from {@code JWT_SECRET} (at least 32 bytes). If it is not set, a random
     * key is generated for this run: tokens stop working after a restart, but no secret lives in the code.
     */
    public JwtUtil(@Value("${gatekeeper.jwt.secret:}") String secret,
                   @Value("${gatekeeper.jwt.expiration-ms:86400000}") long expiration) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET is not set; using a random key, so tokens will not survive a restart");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long");
            }
            this.key = Keys.hmacShaKeyFor(bytes);
        }
        this.expiration = expiration;
    }

    private SecretKey getKey() {
        return key;
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
