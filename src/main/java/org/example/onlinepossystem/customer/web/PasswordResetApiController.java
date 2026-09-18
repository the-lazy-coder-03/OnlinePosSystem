package org.example.onlinepossystem.customer.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.onlinepossystem.customer.api.PasswordResetOperations;
import org.example.onlinepossystem.security.api.RequestClientIp;
import org.example.onlinepossystem.security.dto.ForgotPasswordRequest;
import org.example.onlinepossystem.security.dto.MessageResponse;
import org.example.onlinepossystem.security.dto.ResetPasswordRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetApiController {

    private final PasswordResetOperations passwordResetOperations;

    public PasswordResetApiController(PasswordResetOperations passwordResetOperations) {
        this.passwordResetOperations = passwordResetOperations;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String message = passwordResetOperations.requestReset(request.email(), RequestClientIp.resolve(httpRequest));
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetOperations.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok(new MessageResponse("Your password has been reset. Please log in."));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
