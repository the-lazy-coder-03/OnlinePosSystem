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

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private final RateLimiter rateLimiter;

    public LoginRateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isLoginRequest(request)) {
            String key = "login:" + RequestClientIp.resolve(request);
            if (!rateLimiter.isAllowed(key, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW)) {
                response.sendError(TOO_MANY_REQUESTS, "Too many login attempts. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && ("/login".equals(request.getRequestURI()) || "/api/auth/login".equals(request.getRequestURI()));
    }

}
