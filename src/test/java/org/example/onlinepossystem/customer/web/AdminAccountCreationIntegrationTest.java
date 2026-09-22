package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAccountCreationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void superAdminCreatesPhoneFreeAdminWhoCanSignIn() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.test";
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN", "SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"addAdminModal\"")));
        mvc.perform(post("/admin/accounts").with(user("admin").roles("ADMIN", "SUPER_ADMIN"))
                        .with(csrf())
                        .param("firstName", "Branch")
                        .param("lastName", "Manager")
                        .param("email", email)
                        .param("password", "Strong1!")
                        .param("accessLevel", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?accountCreated#accounts"));

        Customer account = customers.findByEmail(email).orElseThrow();
        assertThat(account.getAccessLevel()).isEqualTo(1);
        assertThat(account.getRole()).isEqualTo("ADMIN");
        assertThat(account.getPhone1()).isNull();
        assertThat(account.getHouseNumber()).isNull();
        assertThat(account.getStreet()).isNull();
        assertThat(account.getArea()).isNull();
        assertThat(account.getPostalCode()).isNull();
        assertThat(passwordEncoder.matches("Strong1!", account.getPassword())).isTrue();

        mvc.perform(post("/login").with(csrf())
                        .param("adminLogin", "true")
                        .param("username", email)
                        .param("password", "Strong1!"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void branchAdminsAndCustomersCannotCreateAdminAccountsAndCsrfIsRequired() throws Exception {
        String email = "blocked-" + UUID.randomUUID() + "@example.test";
        for (String role : new String[]{"ADMIN", "USER", "DRIVER"}) {
            mvc.perform(post("/admin/accounts").with(user("someone").roles(role)).with(csrf())
                            .param("firstName", "Blocked").param("lastName", "Account")
                            .param("email", email).param("password", "Strong1!")
                            .param("accessLevel", "3"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/admin/accounts").with(user("admin").roles("ADMIN", "SUPER_ADMIN"))
                        .param("firstName", "Blocked").param("lastName", "Account")
                        .param("email", email).param("password", "Strong1!")
                        .param("accessLevel", "3"))
                .andExpect(status().isForbidden());
        assertThat(customers.findByEmail(email)).isEmpty();
    }

    @Test
    void invalidLevelAndDuplicateEmailDoNotCreateOrAlterAccounts() throws Exception {
        String email = "existing-" + UUID.randomUUID() + "@example.test";
        Customer existing = new Customer();
        existing.setEmail(email);
        existing.setAccessLevel(0);
        existing.setRole("USER");
        customers.saveAndFlush(existing);
        mvc.perform(post("/admin/accounts").with(user("admin").roles("ADMIN", "SUPER_ADMIN"))
                        .with(csrf()).param("firstName", "Other").param("lastName", "Admin")
                        .param("email", email).param("password", "Strong1!").param("accessLevel", "2"))
                .andExpect(redirectedUrl("/admin?accountError=email#accounts"));
        mvc.perform(post("/admin/accounts").with(user("admin").roles("ADMIN", "SUPER_ADMIN"))
                        .with(csrf()).param("firstName", "Other").param("lastName", "Admin")
                        .param("email", "new-" + UUID.randomUUID() + "@example.test")
                        .param("password", "Strong1!").param("accessLevel", "4"))
                .andExpect(redirectedUrl("/admin?accountError=invalid#accounts"));
        assertThat(customers.findByEmail(email).orElseThrow().getAccessLevel()).isEqualTo(0);
    }
}
