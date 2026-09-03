package com.agribid.nexus.dto.response;

import com.agribid.nexus.domain.user.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        Role role,
        boolean kycVerified
) {
}