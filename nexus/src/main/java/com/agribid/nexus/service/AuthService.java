package com.agribid.nexus.service;

import com.agribid.nexus.dto.request.AuthRequest;
import com.agribid.nexus.dto.request.RegisterRequest;
import com.agribid.nexus.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);
}
