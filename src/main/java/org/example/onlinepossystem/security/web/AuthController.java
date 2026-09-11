package org.example.onlinepossystem.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.onlinepossystem.security.InvalidCredentialsException;
import org.example.onlinepossystem.security.api.ApiAuthentication;
import org.example.onlinepossystem.security.api.RequestClientIp;
import org.example.onlinepossystem.security.dto.AuthRequest;
import org.example.onlinepossystem.security.dto.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ApiAuthentication authenticationService;

    public AuthController(ApiAuthentication authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(authenticationService.authenticate(request, RequestClientIp.resolve(httpRequest)));
        } catch (InvalidCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

}
