package com.kuet.hub.controller;

//package com.university.researchhub.controller;

import com.kuet.hub.dto.ItemDto;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.service.ItemService;
import com.kuet.hub.service.UserService;

import jakarta.validation.Valid;

import com.kuet.hub.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Collections;

@Controller
@RequestMapping("/provider")
@PreAuthorize("hasRole('PROVIDER')")
@Slf4j
public class ItemController {

    private final ItemService itemService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final RequestService requestService;

    public ItemController(ItemService itemService,
                          UserService userService,
                          CategoryRepository categoryRepository,
                          RequestService requestService) {
        this.itemService = itemService;
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.requestService = requestService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        try {
            log.info("[ITEM-FLOW] Loading provider dashboard for user: {}", userDetails.getUsername());
            
            User currentUser = userService.findByUsername(userDetails.getUsername());
            var items = itemService.getItemsByOwner(currentUser);
            
            model.addAttribute("items", items != null ? items : Collections.emptyList());
            model.addAttribute("username", userDetails.getUsername());

            // Pending requests count for the stats card on the dashboard
            long pendingCount = requestService.getRequestsForProvider(currentUser)
                    .stream()
                    .filter(r -> r.getStatus() == com.kuet.hub.entity.Request.RequestStatus.PENDING)
                    .count();
            model.addAttribute("pendingRequestCount", pendingCount);
            
            log.info("[ITEM-FLOW] Dashboard loaded with {} items", items != null ? items.size() : 0);
            return "provider/dashboard";
        } catch (Exception e) {
            log.error("[ITEM-FLOW] Error loading provider dashboard", e);
            model.addAttribute("errorMessage", "Failed to load dashboard: " + e.getMessage());
            model.addAttribute("items", Collections.emptyList());
            return "provider/dashboard";
        }
    }

    @GetMapping("/items/new")
    public String newItemForm(Model model) {
        try {
            log.info("[ITEM-FLOW] Loading create form for new item");
            
            ItemDto itemDto = new ItemDto();
            model.addAttribute("itemDto", itemDto);
            log.debug("[ITEM-FLOW] Added empty ItemDto to model");
            
            var categories = categoryRepository.findAll();
            model.addAttribute("categories", categories);
            log.info("[ITEM-FLOW] Found {} categories for dropdown", categories.size());
            
            var conditions = Item.Condition.values();
            model.addAttribute("conditions", conditions);
            log.debug("[ITEM-FLOW] Added {} item conditions to model", conditions.length);
            
            if (categories.isEmpty()) {
                log.warn("[ITEM-FLOW] WARNING: No categories available. Form will show warning message.");
            }
            
            return "provider/item-form";
        } catch (Exception e) {
            log.error("[ITEM-FLOW] Error loading create form", e);
            model.addAttribute("errorMessage", "Failed to load the form: " + e.getMessage());
            return "provider/item-form";
        }
    }

    @PostMapping("/items/new")
    public String createItem(
            @Valid @ModelAttribute("itemDto") ItemDto itemDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("[ITEM-FLOW] Creating new item: {}", itemDto.getTitle());
        
        if (bindingResult.hasErrors()) {
            log.warn("[ITEM-FLOW] Validation errors detected: {}", bindingResult.getErrorCount());
            var categories = categoryRepository.findAll();
            model.addAttribute("categories", categories);
            model.addAttribute("conditions", Item.Condition.values());
            model.addAttribute("itemDto", itemDto);
            return "provider/item-form";
        }

        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            log.info("[ITEM-FLOW] User: {} creating item", currentUser.getUsername());
            
            itemService.createItem(itemDto, currentUser);
            log.info("[ITEM-FLOW] Item '{}' created successfully", itemDto.getTitle());
            
            redirectAttributes.addFlashAttribute("successMessage", "Item '" + itemDto.getTitle() + "' listed successfully!");
            return "redirect:/provider/dashboard";
        } catch (Exception e) {
            log.error("[ITEM-FLOW] Error creating item: {}", itemDto.getTitle(), e);
            model.addAttribute("errorMessage", "Failed to create item: " + e.getMessage());
            var categories = categoryRepository.findAll();
            model.addAttribute("categories", categories);
            model.addAttribute("conditions", Item.Condition.values());
            model.addAttribute("itemDto", itemDto);
            return "provider/item-form";
        }
    }

    @GetMapping("/items/{id}/edit")
    public String editItemForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        try {
            log.info("[ITEM-FLOW] Loading edit form for item ID: {}", id);
            
            User currentUser = userService.findByUsername(userDetails.getUsername());
            Item item = itemService.findById(id);
            
            if (item == null) {
                log.warn("[ITEM-FLOW] Item {} not found", id);
                return "redirect:/provider/dashboard?error=Item%20not%20found";
            }

            if (!item.getOwner().getId().equals(currentUser.getId())) {
                log.warn("[ITEM-FLOW] User {} attempted to edit item {} they don't own", currentUser.getUsername(), id);
                return "redirect:/provider/dashboard?error=Not%20authorized";
            }

            ItemDto dto = new ItemDto();
            dto.setId(item.getId());
            dto.setTitle(item.getTitle());
            dto.setDescription(item.getDescription());
            dto.setCondition(item.getCondition());
            if (item.getCategory() != null) dto.setCategoryId(item.getCategory().getId());

            model.addAttribute("itemDto", dto);
            
            var categories = categoryRepository.findAll();
            model.addAttribute("categories", categories);
            log.info("[ITEM-FLOW] Found {} categories for edit form", categories.size());
            
            model.addAttribute("conditions", Item.Condition.values());
            return "provider/item-form";
        } catch (Exception e) {
            log.error("[ITEM-FLOW] Error loading edit form for item {}", id, e);
            model.addAttribute("errorMessage", "Failed to load item for editing: " + e.getMessage());
            return "provider/item-form";
        }
    }

    @PostMapping("/items/{id}/edit")
    public String updateItem(
            @PathVariable Long id,
            @Valid @ModelAttribute("itemDto") ItemDto itemDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("[ITEM-FLOW] Updating item ID: {}", id);
        
        if (bindingResult.hasErrors()) {
            log.warn("[ITEM-FLOW] Validation errors in update: {}", bindingResult.getErrorCount());
            var categories = categoryRepository.findAll();
            model.addAttribute("categories", categories);
            model.addAttribute("conditions", Item.Condition.values());
            model.addAttribute("itemDto", itemDto);
            return "provider/item-form";
        }

        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            itemService.updateItem(id, itemDto, currentUser);
            log.info("[ITEM-FLOW] Item {} updated successfully", id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Item updated successfully!");
        } catch (Exception e) {
            log.error("[ITEM-FLOW] Error updating item {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update: " + e.getMessage());
        }
        return "redirect:/provider/dashboard";
    }

    @PostMapping("/items/{id}/delete")
    public String deleteItem(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        log.info("[ITEM-FLOW] Deleting item ID: {}", id);
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            itemService.deleteItem(id, currentUser);
            log.info("[ITEM-FLOW] Item {} deleted successfully", id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Item deleted successfully.");
        } catch (Exception e) {
            log.error("[ITEM-FLOW] Error deleting item {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete: " + e.getMessage());
        }
        return "redirect:/provider/dashboard";
    }
}