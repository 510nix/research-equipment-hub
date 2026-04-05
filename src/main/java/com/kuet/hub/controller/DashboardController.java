package com.kuet.hub.controller;

//package com.university.researchhub.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * DashboardController: Handles smart role-based routing and error pages.
 *
 * SMART ROUTING:
 * - /dashboard endpoint analyzes user's roles and redirects to appropriate dashboard
 * - Admin → /admin/dashboard (system overview and user management)
 * - Provider → /provider/dashboard (equipment management)
 * - Borrower → /browse (equipment browsing)
 *
 * ERROR PAGES:
 * - /access-denied: Centralized 403 Forbidden error page
 *   Handles both Spring Security URL-level denials and programmatic AccessDeniedException
 *
 * LOGGING:
 * - Tracks all dashboard access for security auditing
 * - Logs role-based routing decisions
 * - Logs access denial attempts (with user info and timestamp)
 */
@Controller
@Slf4j
public class DashboardController {

    /**
     * Root path redirect to dashboard.
     * Ensures all users are directed through the smart routing system.
     */
    @GetMapping("/")
    public String home() {
        log.debug("📍 Root path access - redirecting to /dashboard");
        return "redirect:/dashboard";
    }

    /**
     * Smart dashboard routing based on user's roles.
     *
     * WORKFLOW:
     * 1. User logs in and is redirected to /dashboard
     * 2. This controller analyzes user's roles
     * 3. Redirects to appropriate dashboard:
     *    - ADMIN role → /admin/dashboard
     *    - PROVIDER role (and not admin) → /provider/dashboard
     *    - BORROWER role only → /browse
     *
     * This ensures users always land in the right section without
     * needing to remember specific URLs.
     *
     * @param userDetails Authenticated user (null if not authenticated)
     * @return Redirect to role-appropriate dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("⚠️ Dashboard access without authentication - redirecting to login");
            return "redirect:/auth/login";
        }

        String username = userDetails.getUsername();
        log.debug("📊 Dashboard routing for user: {}", username);

        // Check roles in order of precedence (ADMIN > PROVIDER > BORROWER)
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isProvider = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"));

        if (isAdmin) {
            log.info("🔑 Admin '{}' routing to admin dashboard", username);
            return "redirect:/admin/dashboard";
        }
        if (isProvider) {
            log.info("🏪 Provider '{}' routing to provider dashboard", username);
            return "redirect:/provider/dashboard";
        }

        log.info("👤 Borrower '{}' routing to browse equipment", username);
        return "redirect:/browse";
    }

    /**
     * Access denied error page (403 Forbidden).
     *
     * This endpoint is reached when:
     * 1. Spring Security intercepts unauthorized URL access and redirects here
     * 2. GlobalExceptionHandler catches AccessDeniedException and forwards here
     * 3. CustomAccessDeniedHandler redirects here after logging the violation
     *
     * MODEL ATTRIBUTES:
     * - errorMessage: Specific reason for denial
     * - errorCode: HTTP status code (403)
     * - timestamp: When the denial occurred
     *
     * TEMPLATE: access-denied.html (in templates/ root)
     * - Shows user-friendly message
     * - Provides navigation back to dashboard
     * - Explains role-based access system
     *
     * @param model Thymeleaf model for template rendering
     * @return Template name "access-denied"
     */
    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        log.debug("📌 Access denied page requested");

        // Add default error values if not already present (from exception handler)
        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "You do not have permission to access this resource.");
        }
        if (!model.containsAttribute("errorCode")) {
            model.addAttribute("errorCode", 403);
        }
        if (!model.containsAttribute("timestamp")) {
            model.addAttribute("timestamp", System.currentTimeMillis());
        }

        return "access-denied";
    }
}
