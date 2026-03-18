package com.kuet.hub.controller;

import com.kuet.hub.dto.RequestDto;
import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.User;
import com.kuet.hub.service.CustomUserDetailsService;
import com.kuet.hub.service.RequestService;
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
 * All endpoints require BORROWER role for the borrower-specific endpoints.
 * Some endpoints may also require PROVIDER role for request management.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final RequestService requestService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Display the form to create a new request for a specific item.
     * Accessible only to borrowers.
     *
     * @param itemId the ID of the item to request
     * @param model the Spring MVC model
     * @param userDetails the authenticated user
     * @return the request form view
     */
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

    /**
     * Submit a new equipment request.
     * Validates the RequestDto and creates the request if valid.
     *
     * @param itemId the ID of the item being requested
     * @param requestDto the request data from the form
     * @param bindingResult validation results
     * @param redirectAttributes for flash messages
     * @param userDetails the authenticated borrower
     * @return redirect to the My Requests page on success, or back to form on error
     */
    @PostMapping("/requests/submit")
    @PreAuthorize("hasRole('BORROWER')")
    public String submitRequest(@RequestParam Long itemId,
                               @Valid @ModelAttribute RequestDto requestDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Borrower {} submitting request for item {}", userDetails.getUsername(), itemId);

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            log.warn("[REQUEST_CTRL] Request submission validation errors for borrower {}", userDetails.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the form errors and try again");
            return "redirect:/requests/new/" + itemId;
        }

        try {
            // Get the authenticated borrower
            User borrower = (User) userDetailsService.loadUserByUsername(userDetails.getUsername());

            // Create the request
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

    /**
     * Display all requests made by the currently authenticated borrower.
     * Borrowers can view their request history and current status.
     * Accessible at /my-requests (root level) for convenience.
     *
     * @param model the Spring MVC model
     * @param userDetails the authenticated user
     * @return the My Requests view
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('BORROWER')")
    public String myRequests(Model model,
                            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[RUNTIME] Borrower {} is accessing their requests", userDetails.getUsername());

        try {
            User borrower = (User) userDetailsService.loadUserByUsername(userDetails.getUsername());
            List<Request> requests = requestService.getRequestsForBorrower(borrower);

            // Ensure requests is never null
            if (requests == null) {
                requests = java.util.Collections.emptyList();
            }

            model.addAttribute("requests", requests);
            model.addAttribute("requestCount", requests.size());

            log.info("[REQUEST_CTRL] My Requests dashboard loaded with {} requests", requests.size());
            return "borrower/my-requests";

        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error loading My Requests", e);
            model.addAttribute("errorMessage", "Failed to load your requests");
            model.addAttribute("requests", java.util.Collections.emptyList());
            return "borrower/my-requests";
        }
    }

    /**
     * Provider approves an incoming request.
     * Changes the request status from PENDING to APPROVED.
     *
     * @param requestId the ID of the request to approve
     * @param redirectAttributes for flash messages
     * @param userDetails the authenticated provider
     * @return redirect to the provider's requests page
     */
    @PostMapping("/requests/{requestId}/approve")
    @PreAuthorize("hasRole('PROVIDER')")
    public String approveRequest(@PathVariable Long requestId,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Provider {} approving request {}", userDetails.getUsername(), requestId);

        try {
            User provider = (User) userDetailsService.loadUserByUsername(userDetails.getUsername());
            requestService.approveRequest(requestId, provider);

            log.info("[REQUEST_CTRL] Request {} approved successfully", requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Request approved successfully");
            return "redirect:/provider/requests";

        } catch (SecurityException e) {
            log.error("[REQUEST_CTRL] Authorization error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to approve this request");
            return "redirect:/provider/requests";

        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error approving request {}", requestId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve request");
            return "redirect:/provider/requests";
        }
    }

    /**
     * Provider rejects an incoming request.
     * Changes the request status from PENDING to REJECTED.
     *
     * @param requestId the ID of the request to reject
     * @param redirectAttributes for flash messages
     * @param userDetails the authenticated provider
     * @return redirect to the provider's requests page
     */
    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasRole('PROVIDER')")
    public String rejectRequest(@PathVariable Long requestId,
                               RedirectAttributes redirectAttributes,
                               @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Provider {} rejecting request {}", userDetails.getUsername(), requestId);

        try {
            User provider = (User) userDetailsService.loadUserByUsername(userDetails.getUsername());
            requestService.rejectRequest(requestId, provider);

            log.info("[REQUEST_CTRL] Request {} rejected successfully", requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Request rejected");
            return "redirect:/provider/requests";

        } catch (SecurityException e) {
            log.error("[REQUEST_CTRL] Authorization error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to reject this request");
            return "redirect:/provider/requests";

        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error rejecting request {}", requestId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject request");
            return "redirect:/provider/requests";
        }
    }

    /**
     * Borrower marks a request as completed after returning the equipment.
     *
     * @param requestId the ID of the request to mark complete
     * @param redirectAttributes for flash messages
     * @param userDetails the authenticated borrower
     * @return redirect to My Requests
     */
    @PostMapping("/requests/{requestId}/complete")
    @PreAuthorize("hasRole('BORROWER')")
    public String completeRequest(@PathVariable Long requestId,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[REQUEST_CTRL] Borrower {} marking request {} as completed", userDetails.getUsername(), requestId);

        try {
            User borrower = (User) userDetailsService.loadUserByUsername(userDetails.getUsername());
            requestService.completeRequest(requestId, borrower);

            log.info("[REQUEST_CTRL] Request {} marked as completed", requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Request marked as completed");
            return "redirect:/my-requests";

        } catch (SecurityException e) {
            log.error("[REQUEST_CTRL] Authorization error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to complete this request");
            return "redirect:/my-requests";

        } catch (Exception e) {
            log.error("[REQUEST_CTRL] Error completing request {}", requestId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete request");
            return "redirect:/my-requests";
        }
    }
}
