package org.example.onlinepossystem.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 256) String token,
        @NotBlank @Size(max = 512) String newPassword
) {
}
