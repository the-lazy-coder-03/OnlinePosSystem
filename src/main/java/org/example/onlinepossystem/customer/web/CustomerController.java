package org.example.onlinepossystem.customer.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.onlinepossystem.customer.api.AddressSelection;
import org.example.onlinepossystem.customer.service.CustomerRegistrationException;
import org.example.onlinepossystem.customer.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.core.AuthenticationException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

@Controller
@Validated
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public CustomerController(
            CustomerService customerService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.customerService = customerService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam @NotBlank @Size(max = 100) String firstName,
            @RequestParam @NotBlank @Size(max = 100) String lastName,
            @RequestParam(required = false) @Size(max = 100) String houseNumber,
            @RequestParam(required = false) @Size(max = 255) String street,
            @RequestParam(required = false) @Size(max = 120) String area,
            @RequestParam(required = false) @Size(max = 120) String city,
            @RequestParam(required = false) @Size(max = 20) String postalCode,
            @RequestParam(required = false) @Size(max = 255) String googlePlaceId,
            @RequestParam(required = false) @Size(max = 512) String formattedAddress,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) @Size(max = 120) String province,
            @RequestParam(required = false) @Size(max = 120) String country,
            @RequestParam(required = false) @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$") String phone1,
            @RequestParam(required = false) @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$") String phone2,
            @RequestParam(required = false) @Size(max = 100) String preferredStore,
            @RequestParam(required = false) @Size(max = 120) String complexName,
            @RequestParam(required = false, name = "currentPassword") @Size(max = 512) String currentPassword,
            @RequestParam(required = false, name = "password") @Size(max = 512) String newPassword,
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        if (newPassword != null && !newPassword.isBlank()) {
            try {
                authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                        authentication.getName(), currentPassword == null ? "" : currentPassword));
            } catch (AuthenticationException failure) {
                return "redirect:/profile/edit?error=current-password";
            }
        }

        AddressSelection address = new AddressSelection(googlePlaceId, formattedAddress, latitude, longitude,
                houseNumber, street, area, city, postalCode, complexName, province, country);
        try {
            customerService.updateProfile(
                    authentication.getName(),
                    firstName,
                    lastName,
                    houseNumber,
                    street,
                    area,
                    postalCode,
                    phone1,
                    phone2,
                    preferredStore,
                    complexName,
                    newPassword,
                    address
            );
        } catch (IllegalArgumentException ex) {
            if (requiresVerifiedAddress(ex)) {
                return "redirect:/profile/edit?error=address";
            }
            return "redirect:/profile/edit?error=password";
        }

        if (newPassword != null && !newPassword.isBlank()) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            return "redirect:/login?passwordChanged";
        }
        return "redirect:/profile/edit?success";
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam @NotBlank @Size(max = 100) String firstName,
            @RequestParam @NotBlank @Size(max = 100) String lastName,
            @RequestParam @NotBlank @Email @Size(max = 254) String email,
            @RequestParam @NotBlank @Size(max = 512) String password,
            @RequestParam(required = false, name = "house_number") @Size(max = 100) String houseNumber,
            @RequestParam @NotBlank @Size(max = 255) String street,
            @RequestParam @NotBlank @Size(max = 120) String area,
            @RequestParam(required = false) @Size(max = 120) String city,
            @RequestParam @NotBlank @Size(max = 20) String postalCode,
            @RequestParam(required = false) @Size(max = 255) String googlePlaceId,
            @RequestParam(required = false) @Size(max = 512) String formattedAddress,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) @Size(max = 120) String province,
            @RequestParam(required = false) @Size(max = 120) String country,
            @RequestParam @NotBlank @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$") String phone,
            @RequestParam(required = false) @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$") String phone2,
            @RequestParam(required = false, name = "preferred_store") @Size(max = 100) String preferredStore,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        AddressSelection address = new AddressSelection(googlePlaceId, formattedAddress, latitude, longitude,
                houseNumber, street, area, city, postalCode, null, province, country);
        if (!address.isVerifiedGoogleAddress()) {
            return "redirect:/register?error=address";
        }
        try {
            customerService.registerCustomer(
                    firstName,
                    lastName,
                    normalizedEmail,
                    password,
                    phone,
                    phone2,
                    houseNumber,
                    street,
                    area,
                    null,
                    preferredStore,
                    postalCode,
                    address
            );
        } catch (CustomerRegistrationException ex) {
            return switch (ex.getReason()) {
                case PASSWORD -> "redirect:/register?error=password";
                case EMAIL, PHONE -> "redirect:/register?error=account";
            };
        } catch (DataIntegrityViolationException ex) {
            return "redirect:/register?error=account";
        }
        logger.info("New customer account registered");

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, password)
        );
        // Registration signs in directly, so it must also rotate the pre-login session and CSRF token.
        if (request.getSession(false) != null) request.changeSessionId();
        new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository().saveToken(null, request, response);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);

        logger.info("New customer signed in after registration");
        return "redirect:/";
    }

    private boolean requiresVerifiedAddress(IllegalArgumentException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("verified Google address");
    }

}
