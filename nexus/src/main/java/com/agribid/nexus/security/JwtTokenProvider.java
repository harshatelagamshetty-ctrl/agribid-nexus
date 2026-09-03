package com.agribid.nexus.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues and validates JWTs. The kycVerified claim is embedded here,
 * at issuance time, specifically so authorization can be enforced
 * purely from the token's claims at the filter-chain layer — no
 * database lookup needed to reject an unverified actor before the
 * request reaches a controller.
 *
 * Uses the jjwt 0.12+ fluent API (Jwts.parser(), verifyWith(),
 * parseSignedClaims(), getPayload()) — the older setSigningKey() /
 * parserBuilder() / parseClaimsJws() / getBody() methods used in most
 * tutorials were removed as of jjwt 0.12.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtTokenProvider(
            @Value("${agribid.jwt.secret}") String secret,
            @Value("${agribid.jwt.expiration-ms}") long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim("email", principal.getEmail())
                .claim("role", principal.getRole().name())
                .claim("kycVerified", principal.isKycVerified())
                .issuedAt(now)
                .expiration(expiry)
                // algorithm is inferred from the key type (HMAC-SHA family for a SecretKey
                // produced by Keys.hmacShaKeyFor) — no need to pass SignatureAlgorithm.HS512
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public boolean isKycVerified(String token) {
        return Boolean.TRUE.equals(parseClaims(token).get("kycVerified", Boolean.class));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}