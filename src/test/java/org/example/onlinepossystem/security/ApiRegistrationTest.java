package org.example.onlinepossystem.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.onlinepossystem.security.api.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiRegistrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RateLimiter limiter;

    private String uniqueEmail() { return "android-signup-" + UUID.randomUUID() + "@example.com"; }
    private String uniquePhone() { return "07" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode()) % 100_000_000); }

    private Map<String, Object> request(String email, String phone) {
        return Map.of("firstName", "Android", "lastName", "Signup", "email", email,
                "password", "Password1!", "phone", phone, "preferredStore", "Kenridge Branch",
                "address", Map.of("googlePlaceId", "ChIJtestPlaceId", "formattedAddress", "1 Main Street, Cape Town, South Africa",
                        "latitude", -33.9, "longitude", 18.4, "houseNumber", "1", "street", "Main Street",
                        "area", "Cape Town", "city", "Cape Town", "postalCode", "8001", "country", "South Africa"));
    }

    private org.springframework.test.web.servlet.ResultActions register(Map<String, Object> body, String ip) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setRemoteAddr(ip); return req; }).content(json.writeValueAsBytes(body)));
    }

    @Test void signupReturnsTokenForImmediateCustomerAccessWithoutCsrfOrSession() throws Exception {
        String email = uniqueEmail();
        var result = register(request(email, uniquePhone()), "203.0.113.101")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value(email)).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        JsonNode response = json.readTree(result.getResponse().getContentAsString());
        mvc.perform(get("/api/orders/mine").header("Authorization", "Bearer " + response.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test void invalidFieldsWeakPasswordAndUnverifiedAddressReturnJsonErrors() throws Exception {
        var invalid = new java.util.HashMap<>(request(uniqueEmail(), uniquePhone()));
        invalid.put("firstName", "");
        register(invalid, "203.0.113.102").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
        invalid = new java.util.HashMap<>(request(uniqueEmail(), uniquePhone()));
        invalid.put("password", "weak");
        register(invalid, "203.0.113.103").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        invalid = new java.util.HashMap<>(request(uniqueEmail(), uniquePhone()));
        invalid.put("address", Map.of("street", "Main Street", "area", "Cape Town", "postalCode", "8001"));
        register(invalid, "203.0.113.104").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A verified Google address selection is required."));
    }

    @Test void duplicateEmailAndPhoneReturnConflict() throws Exception {
        String email = uniqueEmail(), phone = uniquePhone();
        register(request(email, phone), "203.0.113.105").andExpect(status().isCreated());
        register(request(email, uniquePhone()), "203.0.113.106").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
        register(request(uniqueEmail(), phone), "203.0.113.107").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test void websiteAndApiShareLimitWhileFormCsrfRemainsRequired() throws Exception {
        String ip = "203.0.113.108";
        limiter.reset("http:register:" + ip);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(req -> { req.setRemoteAddr("203.0.113.109"); return req; }))
                .andExpect(status().isForbidden());
        mvc.perform(post("/register").with(req -> { req.setRemoteAddr(ip); return req; }))
                .andExpect(status().isForbidden());
        for (int i = 1; i < 10; i++) {
            register(request(uniqueEmail(), uniquePhone()), ip).andExpect(status().isCreated());
        }
        register(request(uniqueEmail(), uniquePhone()), ip).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(post("/register").with(csrf()).with(req -> { req.setRemoteAddr(ip); return req; }))
                .andExpect(status().isTooManyRequests());
    }
}
