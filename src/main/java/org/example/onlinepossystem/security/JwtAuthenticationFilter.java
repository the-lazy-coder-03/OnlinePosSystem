package org.example.onlinepossystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.onlinepossystem.security.api.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHENTICATED_BEARER = JwtAuthenticationFilter.class.getName() + ".authenticated";

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(TokenService tokenService, UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Never use or mutate a cookie session's identity for a bearer request.
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        try {
            if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                throw new JwtAuthenticationException("Bearer credentials are required");
            }
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            String username = tokenService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            new AccountStatusUserDetailsChecker().check(userDetails);
            if (!tokenService.isTokenValid(token, userDetails)) {
                throw new JwtAuthenticationException("Invalid JWT");
            }
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(AUTHENTICATED_BEARER, true);
        } catch (JwtAuthenticationException | AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
