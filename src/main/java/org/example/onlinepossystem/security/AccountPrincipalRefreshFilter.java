package org.example.onlinepossystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.onlinepossystem.security.api.AccountPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/** Refresh session authorities as well as RLS context after access assignments change. */
public final class AccountPrincipalRefreshFilter extends OncePerRequestFilter {
    private final UserDetailsService accounts;

    public AccountPrincipalRefreshFilter(UserDetailsService accounts) { this.accounts = accounts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && existing.getPrincipal() instanceof AccountPrincipal principal
                && !principal.environmentAdmin() && request.getHeader("Authorization") == null) {
            try {
                var refreshed = accounts.loadUserByUsername(principal.getUsername());
                if (!(refreshed instanceof AccountPrincipal account)
                        || !Objects.equals(account.customerId(), principal.customerId())) {
                    throw new UsernameNotFoundException("Account no longer exists");
                }
                var authentication = UsernamePasswordAuthenticationToken.authenticated(account, null, account.getAuthorities());
                authentication.setDetails(existing.getDetails());
                authentication.eraseCredentials();
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UsernameNotFoundException failure) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                response.sendError(401, "Authentication is required");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
