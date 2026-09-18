package org.example.onlinepossystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class RouteTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    public void testAdminLoginPage() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login"));
    }

    @Test
    public void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    public void testMenuPageUsesReadOnlyOrderingLayout() throws Exception {
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("PlaceOrder"))
                .andExpect(model().attribute("menuOnly", true));
    }

    @Test
    public void testKenridgeMenuPageUsesReadOnlyOrderingLayout() throws Exception {
        mockMvc.perform(get("/menu/kenridge"))
                .andExpect(status().isOk())
                .andExpect(view().name("PlaceOrder"))
                .andExpect(model().attribute("menuOnly", true))
                .andExpect(model().attribute("branchId", 1))
                .andExpect(model().attribute("branchName", "Kenridge"));
    }

    @Test
    public void testUitzichtMenuPageUsesReadOnlyOrderingLayout() throws Exception {
        mockMvc.perform(get("/menu/uitzicht"))
                .andExpect(status().isOk())
                .andExpect(view().name("PlaceOrder"))
                .andExpect(model().attribute("menuOnly", true))
                .andExpect(model().attribute("branchId", 2))
                .andExpect(model().attribute("branchName", "Uitzicht"));
    }

    @Test
    public void testForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"));
    }

    @Test
    public void testResetPasswordPage() throws Exception {
        mockMvc.perform(get("/reset-password").param("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"));
    }

    @Test
    public void testResetPasswordPageWithoutTokenShowsCleanError() throws Exception {
        mockMvc.perform(get("/reset-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("error", "Reset link is invalid or has expired."));
    }

    @Test
    public void testOrderPageUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/order"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testOrderCreationRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testInvalidOrderStatusKeepsExistingErrorContract() throws Exception {
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Cancelled\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Invalid status. Must be Pending, Preparing, Completed, or Rejected."
                ));
    }

    @Test
    public void testInputOrdersPage() throws Exception {
        mockMvc.perform(get("/input-orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("InputOrders"));
    }

    @Test
    public void testOrdersPage() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("InputOrders"));
    }

    @Test
    public void testInputOrdersHtmlIsOk() throws Exception {
        mockMvc.perform(get("/InputOrders.html"))
                .andExpect(status().isOk())
                .andExpect(view().name("InputOrders"));
    }

    @Test
    public void testInputOrdersCaseSensitiveIsOk() throws Exception {
        mockMvc.perform(get("/InputOrders"))
                .andExpect(status().isOk())
                .andExpect(view().name("InputOrders"));
    }
}
