package org.example.onlinepossystem.security.api;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestClientIp {
    private RequestClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        // The servlet container resolves forwarded addresses only from trusted proxies.
        // Reading the raw header here would let clients choose their rate-limit key.
        return request.getRemoteAddr();
    }
}
