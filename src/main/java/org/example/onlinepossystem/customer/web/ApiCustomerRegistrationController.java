package org.example.onlinepossystem.customer.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.onlinepossystem.customer.dto.RegisterRequest;
import org.example.onlinepossystem.customer.service.CustomerRegistrationException;
import org.example.onlinepossystem.customer.service.CustomerService;
import org.example.onlinepossystem.security.api.ApiAuthentication;
import org.example.onlinepossystem.security.api.RequestClientIp;
import org.example.onlinepossystem.security.dto.AuthRequest;
import org.example.onlinepossystem.security.dto.AuthResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
public class ApiCustomerRegistrationController {
    private final CustomerService customers;
    private final ApiAuthentication authentication;

    public ApiCustomerRegistrationController(CustomerService customers, ApiAuthentication authentication) {
        this.customers = customers;
        this.authentication = authentication;
    }

    @PostMapping(value = "/api/auth/register", consumes = "application/json")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletRequest httpRequest) {
        if (!request.address().isVerifiedGoogleAddress() || request.address().postalCode() == null
                || request.address().postalCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A verified Google address selection is required.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        var address = request.address();
        try {
            customers.registerCustomer(request.firstName(), request.lastName(), email, request.password(),
                    request.phone(), request.phone2(), address.houseNumber(), address.street(), address.area(),
                    address.complexName(), request.preferredStore(), address.postalCode(), address);
        } catch (CustomerRegistrationException ex) {
            HttpStatus status = ex.getReason() == CustomerRegistrationException.Reason.PASSWORD
                    ? HttpStatus.BAD_REQUEST : HttpStatus.CONFLICT;
            throw new ResponseStatusException(status, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or phone number is already registered.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authentication.authenticate(
                new AuthRequest(email, request.password()), RequestClientIp.resolve(httpRequest)));
    }
}
