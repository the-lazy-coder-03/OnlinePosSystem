package org.example.onlinepossystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.onlinepossystem.security.api.RateLimiter;
import org.example.onlinepossystem.security.api.RequestClientIp;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/** Coarse per-client limits for expensive or state-changing HTTP endpoints. */
@Component
public final class AbuseProtectionFilter extends OncePerRequestFilter {
    private final RateLimiter rateLimiter;

    public AbuseProtectionFilter(RateLimiter rateLimiter) { this.rateLimiter = rateLimiter; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = limitFor(request);
        if (limit != null) {
            String key = "http:" + limit.bucket + ":" + RequestClientIp.resolve(request);
            if (!rateLimiter.isAllowed(key, limit.attempts, limit.window)) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(limit.window.toSeconds()));
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Limit limitFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("POST".equals(method) && "/register".equals(path)) return new Limit("register", 10, Duration.ofHours(1));
        if ("GET".equals(method) && "/api/full-address".equals(path)) return new Limit("geocode", 30, Duration.ofMinutes(1));
        if ("POST".equals(method) && "/api/orders".equals(path)) return new Limit("orders", 20, Duration.ofMinutes(1));
        if ("POST".equals(method) && (path.equals("/forgot-password") || path.equals("/api/auth/forgot-password")))
            return new Limit("forgot-password", 10, Duration.ofMinutes(15));
        if ("POST".equals(method) && (path.equals("/reset-password") || path.equals("/api/auth/reset-password")))
            return new Limit("reset-password", 10, Duration.ofMinutes(15));
        if ("POST".equals(method) && path.startsWith("/api/branches/") && path.endsWith("/quote"))
            return new Limit("quotes", 60, Duration.ofMinutes(1));
        return null;
    }

    private record Limit(String bucket, int attempts, Duration window) {}
}
