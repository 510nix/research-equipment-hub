package com.kuet.hub.controller;

import com.kuet.hub.dto.RequestDto;
import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.User;
import com.kuet.hub.service.RequestService;
import com.kuet.hub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for handling equipment request operations.
 *
 * ROOT CAUSE FIX (ClassCastException):
 * Every method was doing:
 *   User borrower = (User) userDetailsService.loadUserByUsername(username);
 *
 * loadUserByUsername() returns org.springframework.security.core.userdetails.UserDetails,
 * which Spring Security implements as its own internal User class —
 * NOT com.kuet.hub.entity.User. The cast always fails at runtime.
 *
 * Fix: Replaced CustomUserDetailsService with UserService (same pattern as
 * ItemController) and call userService.findByUsername() which returns
 * com.kuet.hub.entity.User directly from the database.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final RequestService requestService;

    // FIX: Use UserService instead of CustomUserDetailsService.
    // UserService.findByUsername() returns com.kuet.hub.entity.User directly.
    // CustomUserDetailsService.loadUserByUsername() returns Spring Security's
    // UserDetails — casting it to our entity User throws ClassCastException.
    private final UserService userService;

    @GetMapping("/requests/new/{itemId}")
    @PreAuthorize("hasRole('BORROWER')")
    public String showRequestForm(@PathVariable Long itemId, Model model,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Borrower {} accessing request form for item {}",
                userDetails.getUsername(), itemId);
        try {
            model.addAttribute("itemId", itemId);
            model.addAttribute("requestDto", new RequestDto());
            model.addAttribute("today", java.time.LocalDate.now());
            log.info("[REQUEST_CTRL] Request form displayed for item {}", itemId);
            return "borrower/request-form";
        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error displaying request form", e);
            model.addAttribute("errorMessage", "Failed to load request form");
            return "error/generic";
        }
    }

    @PostMapping("/requests/submit")
    @PreAuthorize("hasRole('BORROWER')")
    public String submitRequest(@RequestParam Long itemId,
                                @Valid @ModelAttribute RequestDto requestDto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Borrower {} submitting request for item {}",
                userDetails.getUsername(), itemId);

        if (bindingResult.hasErrors()) {
            log.warn("[REQUEST_CTRL] Validation errors for borrower {}", userDetails.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the form errors and try again");
            return "redirect:/requests/new/" + itemId;
        }

        try {
            // FIX: was (User) userDetailsService.loadUserByUsername(...) → ClassCastException
            User borrower = userService.findByUsername(userDetails.getUsername());

            Request createdRequest = requestService.createRequest(itemId, borrower, requestDto);
            log.info("[REQUEST_CTRL] Request {} created successfully", createdRequest.getId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Equipment request submitted successfully! The provider will review your request.");
            return "redirect:/my-requests";

        } catch (IllegalArgumentException e) {
            log.error("[REQUEST_CTRL] Request submission failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/requests/new/" + itemId;
        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Unexpected error submitting request", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while submitting your request. Please try again.");
            return "redirect:/requests/new/" + itemId;
        }
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('BORROWER')")
    public String myRequests(Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Borrower {} accessing their requests", userDetails.getUsername());
        try {
            // FIX: was (User) userDetailsService.loadUserByUsername(...) → ClassCastException
            User borrower = userService.findByUsername(userDetails.getUsername());
            List<Request> requests = requestService.getRequestsForBorrower(borrower);

            if (requests == null) requests = java.util.Collections.emptyList();

            model.addAttribute("requests", requests);
            model.addAttribute("requestCount", requests.size());
            log.info("[REQUEST_CTRL] My Requests loaded with {} requests", requests.size());
            return "borrower/my-requests";

        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error loading My Requests", e);
            model.addAttribute("errorMessage", "Failed to load your requests");
            model.addAttribute("requests", java.util.Collections.emptyList());
            return "borrower/my-requests";
        }
    }

    /**
     * Provider views all incoming requests for their items.
     */
    @GetMapping("/provider/requests")
    @PreAuthorize("hasRole('PROVIDER')")
    public String providerRequests(Model model,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Provider {} viewing incoming requests", userDetails.getUsername());
        try {
            // FIX: same pattern — use UserService
            User provider = userService.findByUsername(userDetails.getUsername());
            List<Request> requests = requestService.getRequestsForProvider(provider);

            if (requests == null) requests = java.util.Collections.emptyList();

            model.addAttribute("requests", requests);
            model.addAttribute("requestCount", requests.size());
            log.info("[REQUEST_CTRL] Provider requests loaded: {} items", requests.size());
            return "provider/incoming-requests";

        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error loading provider requests", e);
            model.addAttribute("errorMessage", "Failed to load incoming requests");
            model.addAttribute("requests", java.util.Collections.emptyList());
            return "provider/incoming-requests";
        }
    }

    @PostMapping("/requests/{requestId}/approve")
    @PreAuthorize("hasRole('PROVIDER')")
    public String approveRequest(@PathVariable Long requestId,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Provider {} approving request {}", userDetails.getUsername(), requestId);
        try {
            // FIX: was (User) userDetailsService.loadUserByUsername(...) → ClassCastException
            User provider = userService.findByUsername(userDetails.getUsername());
            requestService.approveRequest(requestId, provider);

            log.info("[REQUEST_CTRL] Request {} approved", requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Request approved successfully");
            return "redirect:/provider/requests";

        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to approve this request");
            return "redirect:/provider/requests";
        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error approving request {}", requestId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve request");
            return "redirect:/provider/requests";
        }
    }

    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasRole('PROVIDER')")
    public String rejectRequest(@PathVariable Long requestId,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Provider {} rejecting request {}", userDetails.getUsername(), requestId);
        try {
            // FIX: was (User) userDetailsService.loadUserByUsername(...) → ClassCastException
            User provider = userService.findByUsername(userDetails.getUsername());
            requestService.rejectRequest(requestId, provider);

            log.info("[REQUEST_CTRL] Request {} rejected", requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Request rejected");
            return "redirect:/provider/requests";

        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to reject this request");
            return "redirect:/provider/requests";
        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error rejecting request {}", requestId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject request");
            return "redirect:/provider/requests";
        }
    }

    @PostMapping("/requests/{requestId}/complete")
    @PreAuthorize("hasRole('BORROWER')")
    public String completeRequest(@PathVariable Long requestId,
                                  RedirectAttributes redirectAttributes,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Borrower {} completing request {}", userDetails.getUsername(), requestId);
        try {
            // FIX: was (User) userDetailsService.loadUserByUsername(...) → ClassCastException
            User borrower = userService.findByUsername(userDetails.getUsername());
            requestService.completeRequest(requestId, borrower);

            log.info("[REQUEST_CTRL] Request {} completed", requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Request marked as completed");
            return "redirect:/my-requests";

        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to complete this request");
            return "redirect:/my-requests";
        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error completing request {}", requestId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete request");
            return "redirect:/my-requests";
        }
    }
}