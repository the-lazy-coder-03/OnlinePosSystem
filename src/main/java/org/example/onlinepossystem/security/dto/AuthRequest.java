package org.example.onlinepossystem.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Size(max = 254) String username,
        @NotBlank @Size(max = 512) String password
) {
}
