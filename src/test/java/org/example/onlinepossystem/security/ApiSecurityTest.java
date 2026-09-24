package org.example.onlinepossystem.security;

import org.example.onlinepossystem.security.api.RateLimiter;
import org.example.onlinepossystem.security.api.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @Autowired UserDetailsService accounts;
    @Autowired RateLimiter limiter;

    @BeforeEach void resetLimit() { limiter.reset("login:127.0.0.1"); }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                        .param("username", "admin").param("password", "admin"))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }

    @Test void sessionWritesRequireValidCsrf() throws Exception {
        var session = login();
        mvc.perform(post("/api/orders").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/orders").session(session).with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        // A valid token reaches request validation rather than being rejected by security.
        mvc.perform(post("/api/orders").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test void untrustedWebsitesCannotReadCredentialedApiResponses() throws Exception {
        mvc.perform(options("/api/admin/orders").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(get("/api/admin/orders").session(login()).header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/api/orders").header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));
    }

    @Test void bearerWritesDoNotRequireCsrfOrCreateSessions() throws Exception {
        String token = tokens.generateToken(accounts.loadUserByUsername("admin"));
        var result = mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test void invalidOrDeletedBearerAccountCannotFallBackToSession() throws Exception {
        var session = login();
        String missingAccountToken = tokens.generateToken(User.withUsername("deleted@example.com")
                .password("unused").roles("ADMIN").build());
        for (String credential : new String[]{"Bearer broken", "Bearer ", "Basic invalid", "Bearer " + missingAccountToken}) {
            mvc.perform(get("/api/admin/orders").session(session).header("Authorization", credential))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/orders").session(session).header("Authorization", credential)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        // Rejecting a bearer request must not overwrite the separately authenticated cookie session.
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
    }

    @Test void publicJsonLoginWorksWithoutCsrfButFormLoginRemainsProtected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        mvc.perform(post("/login").param("username", "admin").param("password", "admin"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isForbidden());
    }
}
