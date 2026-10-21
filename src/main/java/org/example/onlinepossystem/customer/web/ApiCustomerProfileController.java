package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.customer.api.AddressSelection;
import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.customer.dto.CustomerProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
public class ApiCustomerProfileController {
    private final CustomerAccountReader customers;

    public ApiCustomerProfileController(CustomerAccountReader customers) {
        this.customers = customers;
    }

    @GetMapping("/api/me")
    public CustomerProfileResponse me(Authentication authentication) {
        CustomerAccount customer = customers.findByEmail(authentication.getName())
                .orElseThrow(() -> new NoSuchElementException("Customer account not found"));
        AddressSelection address = new AddressSelection(customer.googlePlaceId(), customer.formattedAddress(),
                customer.latitude(), customer.longitude(), customer.houseNumber(), customer.street(),
                customer.area(), customer.city(), customer.postalCode(), customer.complexName(),
                customer.province(), customer.country());
        return new CustomerProfileResponse(customer.firstName(), customer.lastName(), customer.email(),
                customer.phone1(), customer.phone2(), customer.preferredStore(), customer.complexName(), address);
    }
}
