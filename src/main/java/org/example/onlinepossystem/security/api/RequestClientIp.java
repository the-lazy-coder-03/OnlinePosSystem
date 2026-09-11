package org.example.onlinepossystem.security.api;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestClientIp {
    private RequestClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
