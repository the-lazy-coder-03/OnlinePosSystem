package org.example.onlinepossystem.security;

import org.example.onlinepossystem.security.api.ApiAuthentication;
import org.example.onlinepossystem.security.api.TokenService;
import org.example.onlinepossystem.security.dto.AuthRequest;
import org.example.onlinepossystem.security.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ApiAuthenticationService implements ApiAuthentication {
    private static final Logger logger = LoggerFactory.getLogger(ApiAuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final String deploymentRevision;

    public ApiAuthenticationService(AuthenticationManager authenticationManager, TokenService tokenService,
            @Value("${DEPLOYMENT_REVISION:unknown}") String deploymentRevision) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.deploymentRevision = deploymentRevision;
    }

    @Override
    public AuthResponse authenticate(AuthRequest request, String clientIp) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            return new AuthResponse(
                    tokenService.generateToken(userDetails),
                    "Bearer",
                    tokenService.getExpirationSeconds(),
                    userDetails.getUsername(),
                    roles
            );
        } catch (BadCredentialsException | AccountStatusException ex) {
            // Keep the response identical for unknown accounts and wrong passwords.
            // Never include the submitted username, password, or stored hash in logs.
            logger.warn("API login rejected (instance={}, category=invalid_credentials, ip={})",
                    deploymentRevision, clientIp);
            throw new InvalidCredentialsException();
        } catch (AuthenticationException ex) {
            // Database and provider failures are not bad passwords. Returning 401 for
            // these failures misleads the app and hides a deployment/account-store issue.
            logger.error("API login authentication service failure (instance={}, category={}, ip={})",
                    deploymentRevision, ex.getClass().getSimpleName(), clientIp);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication service is temporarily unavailable");
        }
    }
}
