package org.example.onlinepossystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.net.URI;

/** Adds a per-response CSP nonce and browser security policy. */
@Component
public final class SecurityHeadersFilter extends OncePerRequestFilter {
    private final SecureRandom random = new SecureRandom();
    private final String websocketOrigin;

    public SecurityHeadersFilter(@Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl) {
        URI origin = URI.create(applicationBaseUrl);
        String websocketScheme = "https".equalsIgnoreCase(origin.getScheme()) ? "wss" : "ws";
        this.websocketOrigin = websocketScheme + "://" + origin.getAuthority();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        request.setAttribute("cspNonce", nonce);
        response.setHeader("Content-Security-Policy", "default-src 'self'; base-uri 'self'; object-src 'none'; "
                + "frame-ancestors 'self'; form-action 'self'; script-src 'self' 'nonce-" + nonce + "'; "
                + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; "
                + "connect-src 'self' " + websocketOrigin + " https://nominatim.openstreetmap.org; "
                + "frame-src https://www.google.com");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(self), camera=(), microphone=(), payment=(), usb=()");
        chain.doFilter(request, response);
    }
}
