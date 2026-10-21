package org.example.onlinepossystem.ordering.web;

import org.example.onlinepossystem.customer.service.CustomerService;
import org.example.onlinepossystem.ordering.api.CustomerOrderSummary;
import org.example.onlinepossystem.ordering.service.OrderService;
import org.example.onlinepossystem.security.api.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderHistoryApiTest {
    private static final String CUSTOMER_A = "order-history-a@example.com";
    private static final String CUSTOMER_B = "order-history-b@example.com";

    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @Autowired UserDetailsService accounts;
    @Autowired CustomerService customers;
    @MockBean OrderService history;

    @BeforeEach
    void setUp() {
        reset(history);
        createCustomerIfMissing(CUSTOMER_A, "0710000101");
        createCustomerIfMissing(CUSTOMER_B, "0710000102");
    }

    @Test
    void returnsHistoryForBearerAccountAndUsesTenOrderLimit() throws Exception {
        when(history.getRecentOrdersForCustomer(eq(CUSTOMER_A), eq(10)))
                .thenReturn(List.of(summary(101L, "Kenridge Branch")));

        mvc.perform(get("/api/orders/mine")
                        .header("Authorization", bearer(CUSTOMER_A))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].branchName").value("Kenridge Branch"))
                .andExpect(jsonPath("$[0].itemLines[0]").value("2 x Pizza"))
                .andExpect(jsonPath("$[0].total").value(189.5));

        verify(history).getRecentOrdersForCustomer(CUSTOMER_A, 10);
    }

    @Test
    void historyLookupUsesSignedInCustomerAndIgnoresRequestedCustomerId() throws Exception {
        when(history.getRecentOrdersForCustomer(eq(CUSTOMER_A), eq(10)))
                .thenReturn(List.of(summary(101L, "Customer A branch")));
        when(history.getRecentOrdersForCustomer(eq(CUSTOMER_B), eq(10)))
                .thenReturn(List.of(summary(202L, "Customer B branch")));

        mvc.perform(get("/api/orders/mine").param("customerId", "999")
                        .header("Authorization", bearer(CUSTOMER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101));
        mvc.perform(get("/api/orders/mine")
                        .header("Authorization", bearer(CUSTOMER_B)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(202));

        verify(history).getRecentOrdersForCustomer(CUSTOMER_A, 10);
        verify(history).getRecentOrdersForCustomer(CUSTOMER_B, 10);
    }

    @Test
    void requiresCustomerAuthentication() throws Exception {
        mvc.perform(get("/api/orders/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/mine")
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());
    }

    private CustomerOrderSummary summary(Long id, String branch) {
        return new CustomerOrderSummary(id, LocalDateTime.of(2026, 10, 20, 12, 30), branch,
                "Pending", "pickup", List.of("2 x Pizza"), new BigDecimal("189.50"));
    }

    private String bearer(String username) {
        return "Bearer " + tokens.generateToken(accounts.loadUserByUsername(username));
    }

    private void createCustomerIfMissing(String email, String phone) {
        if (!customers.emailExists(email)) {
            customers.registerCustomer("Order", "History", email, "Password1!", phone,
                    null, "1", "Test Street", "Kenridge", null, "Kenridge Branch", "7550");
        }
    }
}
