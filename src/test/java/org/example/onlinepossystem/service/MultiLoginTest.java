package org.example.onlinepossystem.service;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MultiLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerUserDetailsService userDetailsService;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    public void testLoginWithEmailOrPhone() {
        // Given
        String email = "test@example.com";
        String phone1 = "1234567890";
        String phone2 = "0987654321";
        String password = "password123";

        customerService.registerCustomer(
                "John", "Doe", email, password, phone1, phone2,
                "123", "Main St", "Area", "Complex", "Store", "1234"
        );

        // When & Then
        // 1. Login with Email
        UserDetails userByEmail = userDetailsService.loadUserByUsername(email);
        assertNotNull(userByEmail);
        assertEquals(email, userByEmail.getUsername());

        // 2. Login with Phone1
        UserDetails userByPhone1 = userDetailsService.loadUserByUsername(phone1);
        assertNotNull(userByPhone1);
        assertEquals(email, userByPhone1.getUsername());

        // 3. Login with Phone2
        UserDetails userByPhone2 = userDetailsService.loadUserByUsername(phone2);
        assertNotNull(userByPhone2);
        assertEquals(email, userByPhone2.getUsername());
    }
    @Test
    public void testLoginPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    public void testSuccessfulLogin() throws Exception {
        String email = "login@example.com";
        String password = "password123";
        customerService.registerCustomer(
                "John", "Doe", email, password, "1112223333", "4445556666",
                "123", "Main St", "Area", "Complex", "Store", "1234"
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }
    @Test
    public void testFailedLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .param("username", "nonexistent@example.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error"));
    }
    @Test
    public void testSuccessfulLoginWithPhone() throws Exception {
        String email = "phone_user@example.com";
        String phone = "9998887777";
        String password = "password123";
        customerService.registerCustomer(
                "Phone", "User", email, password, phone, null,
                "123", "Main St", "Area", "Complex", "Store", "1234"
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .param("username", phone)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }
}
