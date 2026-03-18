package com.kuet.hub.controller;

import com.kuet.hub.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/browse")
@PreAuthorize("hasRole('BORROWER')")
@RequiredArgsConstructor
@Slf4j
public class BrowseController {

    private final ItemService itemService;

    /**
     * Display all available items for borrowers to browse.
     * FIX: Changed @GetMapping("") to @GetMapping to avoid ambiguous mapping
     * with Spring MVC when the trailing slash is absent.
     */
    @GetMapping
    public String browse(Model model) {
        try {
            log.info("[BROWSE] Fetching available items for borrower");
            var items = itemService.getAvailableItems();
            model.addAttribute("items", items);
            model.addAttribute("itemCount", (long) items.size());
            model.addAttribute("searchKeyword", "");
            log.info("[BROWSE] Successfully loaded {} items", items.size());
            return "borrower/browse";
        } catch (Exception e) {
            log.error("[BROWSE] Error loading items", e);
            model.addAttribute("errorMessage", "Failed to load equipment items.");
            return "error/generic";
        }
    }

    /**
     * View details of a specific item.
     */
    @GetMapping("/{itemId}")
    public String viewItem(@PathVariable Long itemId, Model model) {
        try {
            log.info("[BROWSE] Fetching details for item ID: {}", itemId);
            model.addAttribute("item", itemService.findById(itemId));
            log.info("[BROWSE] Item {} loaded successfully", itemId);
            return "borrower/item-detail";
        } catch (IllegalArgumentException e) {
            log.error("[BROWSE] Item not found: {}", itemId);
            model.addAttribute("errorMessage", "Item not found.");
            return "error/generic";
        } catch (Exception e) {
            log.error("[BROWSE] Error loading item {}", itemId, e);
            model.addAttribute("errorMessage", "Failed to load item details.");
            return "error/generic";
        }
    }

    /**
     * Search items by title (queryable by borrowers).
     */
    @GetMapping("/search")
    public String search(@RequestParam(value = "q", defaultValue = "") String keyword, Model model) {
        try {
            log.info("[BROWSE] Searching items with keyword: '{}'", keyword);
            var items = (keyword == null || keyword.trim().isEmpty())
                    ? itemService.getAvailableItems()
                    : itemService.searchByTitle(keyword);

            model.addAttribute("items", items);
            model.addAttribute("searchKeyword", keyword);
            model.addAttribute("itemCount", (long) items.size());
            return "borrower/browse";
        } catch (Exception e) {
            log.error("[BROWSE] Error searching items", e);
            model.addAttribute("errorMessage", "Search failed.");
            return "error/generic";
        }
    }
}