package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.customer.api.PasswordResetOperations;
import org.example.onlinepossystem.customer.service.PasswordResetService;
import org.example.onlinepossystem.shared.exception.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetApiControllerTest {

    private FakePasswordResetOperations passwordResetOperations;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        passwordResetOperations = new FakePasswordResetOperations();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PasswordResetApiController(passwordResetOperations))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void forgotPasswordReturnsGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_RESET_MESSAGE));

        assertThat(passwordResetOperations.requestedEmail).isEqualTo("customer@example.com");
        assertThat(passwordResetOperations.requestedClientIp).isNotBlank();
    }

    @Test
    void resetPasswordReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token\",\"newPassword\":\"ValidPass1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been reset. Please log in."));

        assertThat(passwordResetOperations.resetToken).isEqualTo("token");
        assertThat(passwordResetOperations.newPassword).isEqualTo("ValidPass1!");
    }

    @Test
    void resetPasswordReturnsCleanValidationError() throws Exception {
        passwordResetOperations.resetException = new IllegalArgumentException("Reset link is invalid or has expired.");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad-token\",\"newPassword\":\"ValidPass1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reset link is invalid or has expired."));
    }

    private static class FakePasswordResetOperations implements PasswordResetOperations {
        private String requestedEmail;
        private String requestedClientIp;
        private String resetToken;
        private String newPassword;
        private RuntimeException resetException;

        @Override
        public String requestReset(String email, String clientIp) {
            requestedEmail = email;
            requestedClientIp = clientIp;
            return PasswordResetService.GENERIC_RESET_MESSAGE;
        }

        @Override
        public void resetPassword(String token, String newPassword) {
            resetToken = token;
            this.newPassword = newPassword;
            if (resetException != null) {
                throw resetException;
            }
        }

        @Override
        public void resetPassword(String token, String newPassword, String confirmPassword) {
            resetPassword(token, newPassword);
        }
    }
}
