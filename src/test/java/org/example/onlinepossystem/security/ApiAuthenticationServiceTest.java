package org.example.onlinepossystem.security;

import org.example.onlinepossystem.security.api.TokenService;
import org.example.onlinepossystem.security.dto.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiAuthenticationServiceTest {

    @Test
    void wrongPasswordRemainsAnInvalidCredentialsFailure() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        TokenService tokenService = mock(TokenService.class);
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BadCredentialsException("invalid"));
        ApiAuthenticationService service = new ApiAuthenticationService(authenticationManager, tokenService, "test-revision");

        assertThatThrownBy(() -> service.authenticate(new AuthRequest("person@example.com", "wrong"), "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void accountStoreFailureIsNotReportedAsIncorrectPassword() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        TokenService tokenService = mock(TokenService.class);
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InternalAuthenticationServiceException("account store unavailable"));
        ApiAuthenticationService service = new ApiAuthenticationService(authenticationManager, tokenService, "test-revision");

        assertThatThrownBy(() -> service.authenticate(new AuthRequest("person@example.com", "secret"), "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
