package org.example.onlinepossystem.security;

import org.example.onlinepossystem.security.api.TokenService;
import org.example.onlinepossystem.security.dto.AuthRequest;
import org.example.onlinepossystem.security.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiAuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(ApiAuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public ApiAuthenticationService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

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
        } catch (AuthenticationException ex) {
            logger.warn("Failed API login attempt from IP {}", clientIp);
            throw new InvalidCredentialsException();
        }
    }
}
