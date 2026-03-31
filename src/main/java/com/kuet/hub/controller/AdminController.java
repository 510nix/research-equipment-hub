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

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        log.info("[ADMIN] Loading admin dashboard");
        try {
            List<User> allUsers = userService.findAllUsers();
            // Eager-fetch owner + category to prevent LazyInitializationException
            // Shows ALL items including from deactivated providers (admin needs full visibility)
            List<Item> allItems = itemRepository.findAllWithOwnerAndCategory();

            long availableCount = allItems.stream()
                    .filter(i -> i.getStatus() == Item.ItemStatus.AVAILABLE
                              && i.getOwner().isEnabled()).count();
            long borrowedCount = allItems.stream()
                    .filter(i -> i.getStatus() == Item.ItemStatus.BORROWED).count();
            long activeUsers = allUsers.stream()
                    .filter(User::isEnabled).count();

            model.addAttribute("users", allUsers);
            model.addAttribute("items", allItems);
            model.addAttribute("availableCount", availableCount);
            model.addAttribute("borrowedCount", borrowedCount);
            model.addAttribute("activeUsers", activeUsers);

            log.info("[ADMIN] Dashboard: {} users ({} active), {} items ({} available, {} borrowed)",
                    allUsers.size(), activeUsers, allItems.size(), availableCount, borrowedCount);
            return "admin/dashboard";

        } catch (Exception e) {
            log.error("[ADMIN] Error loading dashboard", e);
            model.addAttribute("errorMessage", "Failed to load dashboard data");
            return "error/generic";
        }
    }

    /**
     * Toggle user enabled/disabled status.
     *
     * UserService.toggleUserEnabled() enforces the Dependency Lock:
     * it throws IllegalStateException if an active borrow blocks deactivation.
     * We catch that specifically and surface a clear message to the admin.
     */
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long userId,
                                   RedirectAttributes redirectAttributes) {
        log.info("[ADMIN] Toggle status for user ID: {}", userId);
        try {
            User user = userService.findById(userId);
            boolean wasEnabled = user.isEnabled();
            userService.toggleUserEnabled(userId);

            String action = wasEnabled ? "deactivated" : "activated";
            String detail = wasEnabled ? " (pending requests cancelled, items hidden from browse)" : "";
            redirectAttributes.addFlashAttribute("successMessage",
                    "User '" + user.getUsername() + "' has been " + action + " successfully." + detail);

        } catch (IllegalStateException e) {
            // Dependency Lock violation — active borrow prevents deactivation
            log.warn("[ADMIN] Deactivation blocked: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
        } catch (Exception e) {
            log.error("[ADMIN] Error toggling user status", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to toggle user status: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        try {
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("categoryDto", new CategoryDto());
            return "admin/categories";
        } catch (Exception e) {
            log.error("[ADMIN] Error listing categories", e);
            model.addAttribute("errorMessage", "Failed to load categories");
            return "error/generic";
        }
    }

    @PostMapping("/categories/new")
    public String createCategory(@Valid @ModelAttribute CategoryDto categoryDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("validationErrors", bindingResult.getAllErrors());
            return "redirect:/admin/categories";
        }
        try {
            if (categoryRepository.existsByName(categoryDto.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Category '" + categoryDto.getName() + "' already exists.");
                return "redirect:/admin/categories";
            }
            Category category = new Category();
            category.setName(categoryDto.getName());
            category.setDescription(categoryDto.getDescription());
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category '" + categoryDto.getName() + "' created successfully.");
        } catch (Exception e) {
            log.error("[ADMIN] Error creating category", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create category.");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable Long id,
                               @Valid @ModelAttribute CategoryDto categoryDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("validationErrors", bindingResult.getAllErrors());
            return "redirect:/admin/categories";
        }
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
            category.setName(categoryDto.getName());
            category.setDescription(categoryDto.getDescription());
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found.");
        } catch (Exception e) {
            log.error("[ADMIN] Error editing category", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update category.");
        }
        return "redirect:/admin/categories";
    }

    /**
     * Delete a category — enforces two-level Dependency Lock:
     *
     * Level 1 (existing): category cannot be deleted if ANY items belong to it.
     *
     * Level 2 (NEW): category cannot be deleted if ANY of its items are currently
     * BORROWED — even if we were willing to delete non-borrowed items, an active
     * borrow means a borrower's request and provider's dashboard still reference
     * this category. Deleting it would cause NullPointerException when rendering
     * category names in those views.
     *
     * The check order:
     *   1. Are there any BORROWED items in this category? → hard block, clearest message
     *   2. Are there any items at all in this category?   → standard block
     *   3. Safe to delete.
     */
    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

            // Level 2 check: any item BORROWED right now?
            long borrowedCount = itemRepository.countBorrowedByCategory(category);
            if (borrowedCount > 0) {
                log.warn("[ADMIN] Cannot delete category '{}' — {} item(s) currently BORROWED",
                        category.getName(), borrowedCount);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Cannot delete category '" + category.getName() + "': "
                        + borrowedCount + " item(s) in this category are currently borrowed. "
                        + "All equipment must be returned before this category can be deleted.");
                return "redirect:/admin/categories";
            }

            // Level 1 check: any items at all in this category?
            long totalItems = itemRepository.countByCategory(category);
            if (totalItems > 0) {
                log.warn("[ADMIN] Cannot delete category '{}' — {} item(s) linked",
                        category.getName(), totalItems);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Cannot delete category '" + category.getName() + "': "
                        + totalItems + " item(s) are linked to it. "
                        + "Remove or re-categorise all items first.");
                return "redirect:/admin/categories";
            }

            categoryRepository.deleteById(id);
            log.info("[ADMIN] Category '{}' deleted", category.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category '" + category.getName() + "' deleted successfully.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found.");
        } catch (Exception e) {
            log.error("[ADMIN] Error deleting category", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete category.");
        }
        return "redirect:/admin/categories";
    }
}