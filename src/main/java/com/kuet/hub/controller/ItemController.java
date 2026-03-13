package com.kuet.hub.controller;

//package com.university.researchhub.controller;

import com.kuet.hub.dto.ItemDto;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.service.ItemService;
import com.kuet.hub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ItemController {

    private final ItemService itemService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;

    public ItemController(ItemService itemService,
                          UserService userService,
                          CategoryRepository categoryRepository) {
        this.itemService = itemService;
        this.userService = userService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("items", itemService.getItemsByOwner(currentUser));
        model.addAttribute("username", userDetails.getUsername());
        return "provider/dashboard";
    }

    @GetMapping("/items/new")
    public String newItemForm(Model model) {
        model.addAttribute("itemDto", new ItemDto());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("conditions", Item.Condition.values());
        return "provider/item-form";
    }

    @PostMapping("/items/new")
    public String createItem(
            @Valid @ModelAttribute("itemDto") ItemDto itemDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("conditions", Item.Condition.values());
            return "provider/item-form";
        }

        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            itemService.createItem(itemDto, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Item listed successfully!");
            return "redirect:/provider/dashboard";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "provider/item-form";
        }
    }

    @GetMapping("/items/{id}/edit")
    public String editItemForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Item item = itemService.findById(id);

        if (!item.getOwner().getId().equals(currentUser.getId())) {
            return "redirect:/provider/dashboard";
        }

        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setCondition(item.getCondition());
        if (item.getCategory() != null) dto.setCategoryId(item.getCategory().getId());

        model.addAttribute("itemDto", dto);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("conditions", Item.Condition.values());
        return "provider/item-form";
    }

    @PostMapping("/items/{id}/edit")
    public String updateItem(
            @PathVariable Long id,
            @Valid @ModelAttribute("itemDto") ItemDto itemDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("conditions", Item.Condition.values());
            return "provider/item-form";
        }

        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            itemService.updateItem(id, itemDto, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Item updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/provider/dashboard";
    }

    @PostMapping("/items/{id}/delete")
    public String deleteItem(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            itemService.deleteItem(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Item deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/provider/dashboard";
    }
}
