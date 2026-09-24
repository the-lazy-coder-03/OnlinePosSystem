package org.example.onlinepossystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.onlinepossystem.security.api.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void resetLoginLimit() {
        rateLimiter.reset("login:127.0.0.1");
    }

    @Test
    public void testAdminPanelRedirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost/admin/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminPanelAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminOrdersPageAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin#orders"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testAdminPanelForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testAdminOrdersApiForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testAdminOrderStatusUpdateForbiddenForUserRole() throws Exception {
        mockMvc.perform(put("/api/admin/orders/1/status").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Preparing\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "driver", roles = {"DRIVER"})
    public void testDriverCannotAccessCustomerOrAdminPages() throws Exception {
        mockMvc.perform(get("/order"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "branch-admin", roles = {"ADMIN"})
    public void testBranchAdminCannotManageAccountLevels() throws Exception {
        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "branch-admin", roles = {"ADMIN"})
    public void testBranchAdminCannotCreateStaffAccounts() throws Exception {
        mockMvc.perform(post("/api/staff/create").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Branch Staff",
                                  "branch": "Kenridge",
                                  "pin": "1234"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAdminLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("adminLogin", "true")
                .param("username", "admin")
                .param("password", "admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    public void testAdminSavedRequestLoginReturnsToAdminPanel() throws Exception {
        MvcResult redirectToLogin = mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost/admin/login"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) redirectToLogin.getRequest().getSession(false);

        mockMvc.perform(post("/login")
                .session(session)
                .with(csrf())
                .param("adminLogin", "true")
                .param("username", "admin")
                .param("password", "admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/admin")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testMissingAuthenticatedRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/radmin"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404));
    }

    @Test
    public void testAdminLoginFailureReturnsToAdminLoginPage() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("adminLogin", "true")
                .param("username", "admin")
                .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    public void testJwtLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "admin",
                          "password": "admin"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    public void testAdminPanelAccessibleWithJwt() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "admin",
                          "password": "admin"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        String token = (String) response.get("accessToken");

        mockMvc.perform(get("/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
    }
}
