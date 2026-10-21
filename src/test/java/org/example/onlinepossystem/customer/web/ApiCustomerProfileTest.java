package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.customer.api.AddressSelection;
import org.example.onlinepossystem.customer.service.CustomerService;
import org.example.onlinepossystem.security.api.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiCustomerProfileTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerService customers;
    @Autowired TokenService tokens;
    @Autowired UserDetailsService accounts;

    @Test void returnsOnlyTheAuthenticatedCustomersSavedDetails() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String firstEmail = "checkout-first-" + suffix + "@example.com";
        String secondEmail = "checkout-second-" + suffix + "@example.com";
        String firstPhone = "071" + String.format("%07d", Math.floorMod(UUID.randomUUID().hashCode(), 10_000_000));
        String secondPhone = "072" + String.format("%07d", Math.floorMod(UUID.randomUUID().hashCode(), 10_000_000));
        AddressSelection address = new AddressSelection("ChIJprofile", "20 Kenridge Ave, Cape Town",
                new BigDecimal("-33.9"), new BigDecimal("18.4"), "20", "Kenridge Ave",
                "Kenridge", "Cape Town", "7550", "Unit 4", "Western Cape", "South Africa");
        customers.registerCustomer("First", "Customer", firstEmail, "Password1!", firstPhone,
                null, "20", "Kenridge Ave", "Kenridge", "Unit 4", "Kenridge Branch", "7550", address);
        customers.registerCustomer("Second", "Customer", secondEmail, "Password1!", secondPhone,
                null, "2", "Other Road", "Uitzicht", null, "Uitzicht Branch", "7550", AddressSelection.empty());

        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/me").header("Authorization", bearer(firstEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("First"))
                .andExpect(jsonPath("$.lastName").value("Customer"))
                .andExpect(jsonPath("$.email").value(firstEmail))
                .andExpect(jsonPath("$.phone").value(firstPhone))
                .andExpect(jsonPath("$.complexName").value("Unit 4"))
                .andExpect(jsonPath("$.address.googlePlaceId").value("ChIJprofile"))
                .andExpect(jsonPath("$.address.formattedAddress").value("20 Kenridge Ave, Cape Town"))
                .andExpect(jsonPath("$.address.country").value("South Africa"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/me").header("Authorization", bearer(secondEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(secondEmail))
                .andExpect(jsonPath("$.firstName").value("Second"));
    }

    private String bearer(String username) {
        return "Bearer " + tokens.generateToken(accounts.loadUserByUsername(username));
    }
}
