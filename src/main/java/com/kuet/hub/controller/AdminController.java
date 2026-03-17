package com.kuet.hub.controller;

import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

/**
 * Admin Controller to manage administrative dashboard and system-wide operations.
 * All endpoints in this controller require ROLE_ADMIN authority.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;
    private final ItemRepository itemRepository;

    /**
     * Admin Dashboard - Displays all users and items in the system.
     * Provides an overview of system state for administrative purposes.
     *
     * @param model the Spring MVC model to populate
     * @return the admin dashboard view name
     */
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        log.info("[STABILITY] Admin accessing dashboard");
        
        try {
            // Fetch all users in the system
            List<User> allUsers = userService.findAllUsers();
            log.info("[STABILITY] Loaded {} users for admin dashboard", allUsers.size());
            
            // Fetch all items in the system
            List<Item> allItems = itemRepository.findAll();
            log.info("[STABILITY] Loaded {} items for admin dashboard", allItems.size());
            
            // Add to model for view rendering
            model.addAttribute("users", allUsers);
            model.addAttribute("items", allItems);
            
            log.info("[STABILITY] Admin dashboard initialized successfully");
            return "admin/dashboard";
            
        } catch (Exception e) {
            log.error("[STABILITY] Error loading admin dashboard", e);
            model.addAttribute("error", "Failed to load dashboard data");
            return "error/generic";
        }
    }
}
