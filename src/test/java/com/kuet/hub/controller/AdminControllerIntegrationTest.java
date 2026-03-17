package com.kuet.hub.controller;

import com.kuet.hub.entity.Category;
import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminController.
 * Tests administrative endpoints with Spring Security context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AdminController Integration Tests")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User borrowerUser;
    private Role adminRole;
    private Role borrowerRole;

    @BeforeEach
    void setUp() {
        // Clean up repositories
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create roles
        adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(Role.RoleName.ROLE_ADMIN);
                    return roleRepository.save(role);
                });

        borrowerRole = roleRepository.findByName(Role.RoleName.ROLE_BORROWER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(Role.RoleName.ROLE_BORROWER);
                    return roleRepository.save(role);
                });

        // Create admin user
        adminUser = new User();
        adminUser.setUsername("admin_user");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setEnabled(true);
        adminUser.getRoles().add(adminRole);
        adminUser = userRepository.save(adminUser);

        // Create borrower user
        borrowerUser = new User();
        borrowerUser.setUsername("borrower_user");
        borrowerUser.setEmail("borrower@example.com");
        borrowerUser.setPassword(passwordEncoder.encode("password123"));
        borrowerUser.setEnabled(true);
        borrowerUser.getRoles().add(borrowerRole);
        borrowerUser = userRepository.save(borrowerUser);
    }

    @Test
    @DisplayName("GET /admin/dashboard - Unauthenticated user receives 401 Unauthorized")
    void testAdminDashboard_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/dashboard - Borrower receives 403 Forbidden")
    @WithUserDetails("borrower_user")
    void testAdminDashboard_BorrowerUser_Returns403() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/dashboard - Admin receives 200 OK and dashboard view")
    @WithUserDetails("admin_user")
    void testAdminDashboard_AdminUser_Returns200() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("items"));
    }

    @Test
    @DisplayName("GET /admin/categories - Admin can list categories")
    @WithUserDetails("admin_user")
    void testListCategories_AdminUser_Returns200() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("GET /admin/categories - Borrower receives 403 Forbidden")
    @WithUserDetails("borrower_user")
    void testListCategories_BorrowerUser_Returns403() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/categories/new - Admin can create category")
    @WithUserDetails("admin_user")
    void testCreateCategory_AdminUser_RedirectsToCategoriesList() throws Exception {
        mockMvc.perform(post("/admin/categories/new")
                .with(csrf())
                .param("name", "Test Category")
                .param("description", "Test description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        // Verify category was created
        assert categoryRepository.findByName("Test Category").isPresent();
    }

    @Test
    @DisplayName("POST /admin/categories/new - Borrower receives 403 Forbidden")
    @WithUserDetails("borrower_user")
    void testCreateCategory_BorrowerUser_Returns403() throws Exception {
        mockMvc.perform(post("/admin/categories/new")
                .with(csrf())
                .param("name", "Test Category")
                .param("description", "Test description"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/users/{id}/toggle-status - Admin can toggle user status")
    @WithUserDetails("admin_user")
    void testToggleUserStatus_AdminUser_TogglesSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/toggle-status", borrowerUser.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        // Verify status was toggled
        User toggledUser = userRepository.findById(borrowerUser.getId()).orElseThrow();
        assert !toggledUser.isEnabled();
    }

    @Test
    @DisplayName("POST /admin/users/{id}/toggle-status - Borrower receives 403 Forbidden")
    @WithUserDetails("borrower_user")
    void testToggleUserStatus_BorrowerUser_Returns403() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/toggle-status", adminUser.getId())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
