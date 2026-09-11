package org.example.onlinepossystem.security.api;

import org.example.onlinepossystem.security.dto.AuthRequest;
import org.example.onlinepossystem.security.dto.AuthResponse;

public interface ApiAuthentication {
    AuthResponse authenticate(AuthRequest request, String clientIp);
}
