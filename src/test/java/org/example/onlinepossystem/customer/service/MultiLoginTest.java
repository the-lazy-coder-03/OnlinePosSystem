package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.api.AccountAccessAdministration;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
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

    @Autowired
    private AccountAccessAdministration accessAdministration;

    @Test
    public void namedSuperAdminCanOpenOrderPageAndPlaceOwnOrder() throws Exception {
        String email = "named-super-order@example.com";
        Customer customer = customerService.registerCustomer(
                "Named", "Admin", email, "Password1!", "0712345699", null,
                "12", "Main Street", "Kenridge", null, "Kenridge Branch", "7550"
        );
        accessAdministration.assignAccessLevel(customer.getId(), 3);
        UserDetails principal = userDetailsService.loadUserByUsername(email);

        mockMvc.perform(MockMvcRequestBuilders.get("/order").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("PlaceOrder"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").with(user(principal))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Named Admin\",\"branchName\":\"Kenridge\",\"items\":[{\"menuItemId\":101,\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    public void namedSuperAdminReturnsToSavedOrderPageAfterLogin() throws Exception {
        String email = "named-super-login@example.com";
        String password = "Password1!";
        Customer customer = customerService.registerCustomer(
                "Named", "Login", email, password, "0712345688", null,
                "12", "Main Street", "Kenridge", null, "Kenridge Branch", "7550"
        );
        accessAdministration.assignAccessLevel(customer.getId(), 3);

        MvcResult loginRedirect = mockMvc.perform(MockMvcRequestBuilders.get("/order"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginRedirect.getRequest().getSession(false);

        MvcResult login = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .session(session)
                        .with(request -> { request.setRemoteAddr("198.51.100.10"); return request; })
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/order")))
                .andExpect(authenticated().withUsername(email))
                .andReturn();

        mockMvc.perform(MockMvcRequestBuilders.get("/order")
                        .session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(view().name("PlaceOrder"));
    }

    @Test
    public void invalidSavedDestinationFallsBackAfterSuccessfulLogin() throws Exception {
        String email = "saved-target@example.com";
        String password = "Password1!";
        customerService.registerCustomer(
                "Saved", "Target", email, password, "0712345687", null,
                "12", "Main Street", "Kenridge", null, "Kenridge Branch", "7550"
        );

        MvcResult loginRedirect = mockMvc.perform(MockMvcRequestBuilders.get("/radmin"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginRedirect.getRequest().getSession(false);

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .session(session)
                        .with(request -> { request.setRemoteAddr("198.51.100.11"); return request; })
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(email));
    }

    @Test
    public void duplicateLoginWithRotatedCsrfIsRejectedButSessionStaysAuthenticated() throws Exception {
        String email = "duplicate-login@example.com";
        String password = "Password1!";
        customerService.registerCustomer(
                "Duplicate", "Login", email, password, "0712345686", null,
                "12", "Main Street", "Kenridge", null, "Kenridge Branch", "7550"
        );

        MvcResult loginPage = mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginPage.getRequest().getSession(false);
        CsrfToken csrfToken = (CsrfToken) loginPage.getRequest().getAttribute(CsrfToken.class.getName());
        assertNotNull(csrfToken);

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .session(session)
                        .with(request -> { request.setRemoteAddr("198.51.100.12"); return request; })
                        .param(csrfToken.getParameterName(), csrfToken.getToken())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(authenticated().withUsername(email));

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .session(session)
                        .with(request -> { request.setRemoteAddr("198.51.100.12"); return request; })
                        .param(csrfToken.getParameterName(), csrfToken.getToken())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.get("/order").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(email));
    }

    @Test
    public void environmentSuperAdminCannotPlaceCustomerOrder() throws Exception {
        UserDetails principal = userDetailsService.loadUserByUsername("admin");

        mockMvc.perform(MockMvcRequestBuilders.get("/order").with(user(principal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").with(user(principal))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Admin\",\"branchName\":\"Kenridge\",\"items\":[{\"menuItemId\":101,\"quantity\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testLoginWithEmailOrPhone() {
        // Given
        String email = "test@example.com";
        String phone1 = "1234567890";
        String phone2 = "0987654321";
        String password = "Password1!";

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
        String password = "Password1!";
        customerService.registerCustomer(
                "John", "Doe", email, password, "1112223333", "4445556666",
                "123", "Main St", "Area", "Complex", "Store", "1234"
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void testRegistrationSignsCustomerIn() throws Exception {
        String email = "new-customer@example.com";

        MvcResult registration = mockMvc.perform(MockMvcRequestBuilders.post("/register")
                        .with(csrf())
                        .param("firstName", "New")
                        .param("lastName", "Customer")
                        .param("email", email)
                        .param("password", "Password1!")
                        .param("house_number", "12")
                        .param("street", "Main Street")
                        .param("area", "Kenridge")
                        .param("postalCode", "7550")
                        .param("phone", "0712345678")
                        .param("preferred_store", "Kenridge Branch"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(email))
                .andReturn();

        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(MockMvcRequestBuilders.get("/order").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(email))
                .andExpect(model().attribute("preferredBranchId", 1))
                .andExpect(model().attribute("preferredBranchName", "Kenridge"))
                .andExpect(content().string(containsString("const PREFERRED_BRANCH_ID = 1;")))
                .andExpect(content().string(containsString("const PREFERRED_BRANCH_NAME = \"Kenridge\";")));
    }

    @Test
    public void testPreferredUitzichtStoreIsResolvedForOrdering() throws Exception {
        String email = "preferred-uitzicht@example.com";
        customerService.registerCustomer(
                "Preferred", "Uitzicht", email, "Password1!", "0712345679", null,
                "12", "Main Street", "Area", null, "Uitzicht Branch", "7550"
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/order")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email)
                                .roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("preferredBranchId", 2))
                .andExpect(model().attribute("preferredBranchName", "Uitzicht"));
    }

    @Test
    public void testMissingOrInvalidPreferredStoreFallsBackToBranchPicker() throws Exception {
        String missingEmail = "missing-preferred@example.com";
        customerService.registerCustomer(
                "Missing", "Preferred", missingEmail, "Password1!", "0712345680", null,
                "12", "Main Street", "Area", null, null, "7550"
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/order")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(missingEmail)
                                .roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("preferredBranchId", "preferredBranchName"));

        String invalidEmail = "invalid-preferred@example.com";
        customerService.registerCustomer(
                "Invalid", "Preferred", invalidEmail, "Password1!", "0712345681", null,
                "12", "Main Street", "Area", null, "Unknown Branch", "7550"
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/order")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(invalidEmail)
                                .roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("preferredBranchId", "preferredBranchName"));
    }

    @Test
    public void testFailedLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .with(csrf())
                        .param("username", "nonexistent@example.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error"));
    }
    @Test
    public void testSuccessfulLoginWithPhone() throws Exception {
        String email = "phone_user@example.com";
        String phone = "9998887777";
        String password = "Password1!";
        customerService.registerCustomer(
                "Phone", "User", email, password, phone, null,
                "123", "Main St", "Area", "Complex", "Store", "1234"
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .with(csrf())
                        .param("username", phone)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }
}
