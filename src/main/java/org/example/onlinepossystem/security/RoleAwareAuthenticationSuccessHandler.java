package org.example.onlinepossystem.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.onlinepossystem.security.api.RateLimiter;
import org.example.onlinepossystem.security.api.RequestClientIp;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

@Component
public class RoleAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(RoleAwareAuthenticationSuccessHandler.class);
    private static final Set<String> CUSTOMER_PATHS = Set.of("/order", "/profile/edit", "/profile/update");
    private static final Set<String> ADMIN_ALIASES = Set.of(
            "/input-orders", "/orders", "/InputOrders", "/InputOrders.html"
    );

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RateLimiter rateLimiter;

    public RoleAwareAuthenticationSuccessHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws ServletException, IOException {
        logger.info("Successful login for role(s) {}", authentication.getAuthorities());
        boolean adminLogin = "true".equals(request.getParameter("adminLogin"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (adminLogin && !isAdmin) {
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            response.sendRedirect("/admin/login?error");
            return;
        }

        rateLimiter.reset("login:" + RequestClientIp.resolve(request));

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String savedTarget = savedRequest == null ? null : safeTarget(savedRequest.getRedirectUrl());
        requestCache.removeRequest(request, response);

        String target = isAllowed(savedTarget, adminLogin, authentication)
                ? savedTarget
                : (adminLogin ? "/admin" : "/");
        redirectStrategy.sendRedirect(request, response, target);
    }

    private String safeTarget(String redirectUrl) {
        try {
            URI uri = URI.create(redirectUrl);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) return null;
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isAllowed(String target, boolean adminLogin, Authentication authentication) {
        if (target == null) return false;
        String path = target.contains("?") ? target.substring(0, target.indexOf('?')) : target;
        if (adminLogin) {
            return path.equals("/admin") || path.startsWith("/admin/") || ADMIN_ALIASES.contains(path);
        }
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority()));
        return isCustomer && CUSTOMER_PATHS.contains(path);
    }
}
