package org.example.onlinepossystem.staff.service;

import org.example.onlinepossystem.staff.entity.Staff;
import org.example.onlinepossystem.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StaffLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffRepository staffRepository;

    @Test
    public void testLoginWith16CharCode() throws Exception {
        // Given
        String code = "TEST_CODE_123456";
        staffService.createStaff("Test Staff", "Kenridge", "1234", code);

        // When & Then
        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"" + code + "\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("/admin/login")));
    }

    @Test
    public void testLoginWithInvalidCode() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"INVALID_CODE_123\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testPinLoginIsRetired() throws Exception {
        // Given
        String pin = "5678";
        staffService.createStaff("Uitzicht Staff", "Uitzicht", pin, "UITZICHT_1234567");

        // When & Then
        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\": \"" + pin + "\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("/admin/login")));
    }
}
