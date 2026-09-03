package com.agribid.nexus.dto.response;

import java.time.Instant;

public record VideoChallengeResponse(
    String instruction,
    Instant expiresAt
) {
}
