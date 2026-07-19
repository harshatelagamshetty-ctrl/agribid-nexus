package com.agribid.nexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds agribid.jwt.* from application.properties. JwtTokenProvider
 * currently injects these two values directly via @Value — this
 * class exists so a future addition (e.g. a refresh-token TTL, or a
 * rotating-secret schedule) has a single typed home rather than
 * scattering more @Value("${agribid.jwt....}") strings across the
 * codebase.
 */
@Configuration
@ConfigurationProperties(prefix = "agribid.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * HMAC signing secret. Must be at least 64 bytes for HS512 (the
     * algorithm Keys.hmacShaKeyFor infers from a sufficiently long
     * key) — a short secret here fails at startup with a clear
     * WeakKeyException rather than silently signing with a weaker
     * algorithm.
     */
    private String secret;

    private long expirationMs;
}
