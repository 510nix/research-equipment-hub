package com.kuet.hub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPage_returnsOk() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void registerPage_returnsOk() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void registerPost_withBlankFields_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "")
                        .param("email", "not-an-email")
                        .param("password", "123")
                        .param("confirmPassword", "123")
                        .param("role", "BORROWER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registrationDto", "username", "email", "password"));
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void borrowerCannotAccessAdminDashboard_returnsForbiddenOrRedirect() throws Exception {
        // Borrower attempting to access admin dashboard should be denied
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Accept either 403 Forbidden or 302 redirect
                    assertTrue(status == 403 || status == 302, 
                            "Borrower should get 403 Forbidden or 302 redirect, got: " + status);
                });
    }
}