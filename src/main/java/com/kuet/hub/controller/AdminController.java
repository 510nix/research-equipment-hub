package com.kuet.hub.controller;

import com.kuet.hub.dto.CategoryDto;
import com.kuet.hub.entity.Category;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
@SuppressWarnings("null")
public class AdminController {

    private final UserService userService;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Admin Dashboard - Displays all users and items in the system.
     * Provides an overview of system state for administrative purposes.
     *
     * @param model the Spring MVC model to populate
     * @return the admin dashboard view name
     */
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        log.info("[ADMIN] Admin accessing dashboard");
        
        try {
            // Fetch all users in the system
            List<User> allUsers = userService.findAllUsers();
            log.info("[ADMIN] Loaded {} users for admin dashboard", allUsers.size());
            
            // Fetch all items in the system
            List<Item> allItems = itemRepository.findAll();
            log.info("[ADMIN] Loaded {} items for admin dashboard", allItems.size());
            
            // Add to model for view rendering
            model.addAttribute("users", allUsers);
            model.addAttribute("items", allItems);
            
            log.info("[ADMIN] Admin dashboard initialized successfully");
            return "admin/dashboard";
            
        } catch (Exception e) {
            log.error("[ADMIN] Error loading admin dashboard", e);
            model.addAttribute("error", "Failed to load dashboard data");
            return "error/generic";
        }
    }

    /**
     * Toggle user enabled/disabled status.
     * Admin can enable or disable user accounts to maintain platform security.
     *
     * @param userId the ID of the user to toggle
     * @param redirectAttributes for flash messages
     * @return redirect to admin dashboard
     */
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long userId, RedirectAttributes redirectAttributes) {
        log.info("[ADMIN] Attempting to toggle status for user ID: {}", userId);
        
        try {
            User user = userService.findById(userId);
            userService.toggleUserEnabled(userId);
            
            String status = user.isEnabled() ? "deactivated" : "activated";
            log.info("[ADMIN] User ID {} has been {}", userId, status);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "User '" + user.getUsername() + "' has been " + status + " successfully");
            
        } catch (IllegalArgumentException e) {
            log.warn("[ADMIN] User not found for toggle: {}", userId);
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
        } catch (Exception e) {
            log.error("[ADMIN] Error toggling user status", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to toggle user status");
        }
        
        return "redirect:/admin/dashboard";
    }

    /**
     * List all categories in the system.
     *
     * @param model the Spring MVC model to populate
     * @return the categories list view name
     */
    @GetMapping("/categories")
    public String listCategories(Model model) {
        log.info("[ADMIN] Listing all categories");
        
        try {
            List<Category> categories = categoryRepository.findAll();
            log.info("[ADMIN] Retrieved {} categories", categories.size());
            
            model.addAttribute("categories", categories);
            model.addAttribute("categoryDto", new CategoryDto());
            
            return "admin/categories";
            
        } catch (Exception e) {
            log.error("[ADMIN] Error listing categories", e);
            model.addAttribute("error", "Failed to load categories");
            return "error/generic";
        }
    }

    /**
     * Create a new category.
     *
     * @param categoryDto the category data transfer object
     * @param bindingResult binding result for validation errors
     * @param redirectAttributes for flash messages
     * @return redirect to categories list
     */
    @PostMapping("/categories/new")
    public String createCategory(@Valid @ModelAttribute CategoryDto categoryDto, 
                                BindingResult bindingResult, 
                                RedirectAttributes redirectAttributes) {
        log.info("[ADMIN] Attempting to create category: {}", categoryDto.getName());
        
        if (bindingResult.hasErrors()) {
            log.warn("[ADMIN] Category validation failed: {}", bindingResult.getFieldError());
            redirectAttributes.addFlashAttribute("validationErrors", bindingResult.getAllErrors());
            return "redirect:/admin/categories";
        }
        
        try {
            if (categoryRepository.existsByName(categoryDto.getName())) {
                log.warn("[ADMIN] Category already exists: {}", categoryDto.getName());
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Category '" + categoryDto.getName() + "' already exists");
                return "redirect:/admin/categories";
            }
            
            Category category = new Category();
            category.setName(categoryDto.getName());
            category.setDescription(categoryDto.getDescription());
            categoryRepository.save(category);
            
            log.info("[ADMIN] Category created successfully: {}", categoryDto.getName());
            redirectAttributes.addFlashAttribute("successMessage", 
                "Category '" + categoryDto.getName() + "' created successfully");
            
        } catch (Exception e) {
            log.error("[ADMIN] Error creating category", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create category");
        }
        
        return "redirect:/admin/categories";
    }

    /**
     * Update an existing category.
     *
     * @param id the category ID
     * @param categoryDto the updated category data
     * @param bindingResult binding result for validation errors
     * @param redirectAttributes for flash messages
     * @return redirect to categories list
     */
    @PostMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable Long id, 
                              @Valid @ModelAttribute CategoryDto categoryDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        log.info("[ADMIN] Attempting to edit category ID: {}", id);
        
        if (bindingResult.hasErrors()) {
            log.warn("[ADMIN] Category validation failed during edit");
            redirectAttributes.addFlashAttribute("validationErrors", bindingResult.getAllErrors());
            return "redirect:/admin/categories";
        }
        
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
            
            category.setName(categoryDto.getName());
            category.setDescription(categoryDto.getDescription());
            categoryRepository.save(category);
            
            log.info("[ADMIN] Category updated successfully: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Category updated successfully");
            
        } catch (IllegalArgumentException e) {
            log.warn("[ADMIN] Category not found for edit: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found");
        } catch (Exception e) {
            log.error("[ADMIN] Error editing category", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update category");
        }
        
        return "redirect:/admin/categories";
    }

    /**
     * Delete a category if no items are linked to it.
     *
     * @param id the category ID
     * @param redirectAttributes for flash messages
     * @return redirect to categories list
     */
    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("[ADMIN] Attempting to delete category ID: {}", id);
        
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
            
            // Check if any items are linked to this category
            long itemCount = itemRepository.countByCategory(category);
            
            if (itemCount > 0) {
                log.warn("[ADMIN] Cannot delete category with {} linked items", itemCount);
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Cannot delete category: " + itemCount + " item(s) are linked to this category");
                return "redirect:/admin/categories";
            }
            
            categoryRepository.deleteById(id);
            log.info("[ADMIN] Category deleted successfully: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Category deleted successfully");
            
        } catch (IllegalArgumentException e) {
            log.warn("[ADMIN] Category not found for deletion: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found");
        } catch (Exception e) {
            log.error("[ADMIN] Error deleting category", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete category");
        }
        
        return "redirect:/admin/categories";
    }
}
