package org.example.onlinepossystem.customer.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.onlinepossystem.customer.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Validated
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(
            @RequestParam @NotBlank @Email String email,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("message", passwordResetService.requestReset(email, clientIp(request)));
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam @NotBlank String token,
            @RequestParam @NotBlank String password,
            @RequestParam @NotBlank String confirmPassword,
            Model model
    ) {
        try {
            passwordResetService.resetPassword(token, password, confirmPassword);
            return "redirect:/login?reset";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("token", token);
            model.addAttribute("error", ex.getMessage());
            return "reset-password";
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
