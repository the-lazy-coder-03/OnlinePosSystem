package org.example.onlinepossystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    public void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    public void testOrderPageUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/order"))
                .andExpect(status().is3xxRedirection());
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
