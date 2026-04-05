package com.kuet.hub.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * GlobalExceptionHandler: Centralized error handling for the Research Equipment Hub.
 *
 * This @ControllerAdvice class intercepts ALL exceptions thrown across all controllers,
 * preventing raw error pages from reaching users. It maps exceptions to appropriate
 * HTTP status codes and custom Thymeleaf error templates.
 *
 * ARCHITECTURE:
 * - Level 1 (Spring Security): AccessDeniedHandler (for URL/method-level 403 errors)
 * - Level 2 (Application Logic): This class - catches Java exceptions (BusinessLogic errors)
 * - Level 3 (Database): FK constraints caught here as IllegalStateException
 *
 * LOGGING STRATEGY:
 * - ERROR level: For 500 errors (system failures) — requires investigation
 * - WARN level: For 403/404 (client mistakes or probing attempts) — informational
 * - DEBUG level: For method entry/exit and ignorable events
 *
 * MODEL ATTRIBUTES:
 * - errorTitle: User-friendly title (e.g., "Item Cannot Be Deleted")
 * - errorMessage: Specific error reason (e.g., "This item is currently borrowed.")
 * - errorCode: HTTP status code (e.g., 403, 404, 500)
 * - errorId: Unique error identifier for support tickets (500 errors)
 * - timestamp: When error occurred (helps admin debug)
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle NoResourceFoundException (404 - Resource Not Found).
     *
     * Common Causes:
     * 1. User types non-existent URL: /microscope-xyz
     * 2. Browser auto-requests /favicon.ico but it doesn't exist (silently handled)
     * 3. User deletes item, then tries to access /browse/999
     *
     * FIX: Spring Security permits /favicon.ico in SecurityConfig,
     * so the application doesn't throw 500 for favicon.
     * But if user manually accesses a non-existent item detail page,
     * this handler catches it and shows a friendly 404.
     *
     * Root cause of favicon issue:
     * Browsers silently request /favicon.ico on every page load. When no
     * favicon file exists in /static, Spring throws NoResourceFoundException.
     * This was falling into the generic Exception handler, which called
     * return "error/generic" — turning a harmless missing-file situation into
     * a full 500 error page. That HTML error page was then returned to the
     * browser for the favicon request, and app.js tried to JSON.parse() it,
     * causing: SyntaxError: Unexpected token '🔬' (the navbar emoji).
     *
     * Fix: catch NoResourceFoundException explicitly and return a plain 404
     * with no body. The browser silently discards missing favicons.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFound(NoResourceFoundException ex,
                                      HttpServletResponse response) throws Exception {
        // Only log at DEBUG level — this is expected browser behaviour, not an error
        log.debug("[STATIC] Resource not found (ignored): {}", ex.getResourcePath());
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Handle AccessDeniedException (403 - Unauthorized Access).
     *
     * This complements Spring Security's AccessDeniedHandler.
     * Spring Security handles URL/method-level 403 errors,
     * but if a programmatic check throws AccessDeniedException,
     * this handler catches it for a custom response.
     *
     * Example: Service method contains custom authorization check
     * and explicitly throws AccessDeniedException.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("🚫 Access denied - Forbidden resource access: {}", ex.getMessage());

        model.addAttribute("errorCode", 403);
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage",
                ex.getMessage() != null ? ex.getMessage() :
                "You do not have permission to access this resource.");
        model.addAttribute("timestamp", System.currentTimeMillis());

        return "access-denied";
    }

    /**
     * Handle IllegalArgumentException (422 - Business Logic Violation).
     *
     * Thrown by service layer when business rules are violated.
     * Examples:
     * 1. ItemService.deleteItem(): "Cannot delete equipment that is currently borrowed"
     * 2. UserService.toggleUserEnabled(): "Cannot deactivate user who has active borrows"
     * 3. RequestService.createRequest(): "Item is not available for borrowing"
     * 4. CategoryService.deleteCategory(): "Cannot delete category - X items linked"
     *
     * These are EXPECTED exceptions (not program errors), so they get user-friendly messages.
     * Admin can see details in logs, but end-user sees only what they need to know.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)  // HTTP 422
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("⚠️ Business logic violation: {}", ex.getMessage());

        model.addAttribute("errorCode", 422);
        model.addAttribute("errorTitle", "Operation Not Allowed");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("timestamp", System.currentTimeMillis());

        return "error/business-error";
    }

    /**
     * Handle IllegalStateException (422 - Invalid State Transition).
     *
     * Thrown when an operation conflicts with current system state.
     * Examples (Dependency Lock Pattern):
     * 1. UserService.toggleUserEnabled(): "Cannot deactivate user with active borrows"
     * 2. ItemService.deleteItem(): "Item is currently borrowed"
     * 3. RequestService.approveRequest(): "Item already approved for another borrower"
     *
     * These indicate the user must take a prerequisite action before retrying.
     * For example: "Complete all borrowed items before disabling account"
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)  // HTTP 422
    public String handleIllegalState(IllegalStateException ex, Model model) {
        log.warn("🔒 State constraint violation (Dependency Lock enforced): {}", ex.getMessage());

        model.addAttribute("errorCode", 422);
        model.addAttribute("errorTitle", "Cannot Complete Operation");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("timestamp", System.currentTimeMillis());

        return "error/business-error";
    }

    /**
     * Handle NullPointerException (500 - Application Bug).
     *
     * Thrown when code tries to dereference a null object.
     * This is a PROGRAMMING ERROR—something that should never happen in production.
     *
     * SECURITY: Don't expose stack trace to user. Log full error for admin debugging.
     * USABILITY: Show generic message to user (don't confuse them with technical details).
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleNullPointer(NullPointerException ex, Model model) {
        log.error("💥 Null pointer exception – Programming bug detected!", ex);

        String errorId = "NPE-" + System.currentTimeMillis();
        log.error("Error ID: {}", errorId);

        model.addAttribute("errorCode", 500);
        model.addAttribute("errorTitle", "Server Error");
        model.addAttribute("errorMessage",
                "An unexpected error occurred on our server. " +
                "Error ID: " + errorId + " Our team has been notified. Please try again later.");
        model.addAttribute("errorId", errorId);
        model.addAttribute("timestamp", System.currentTimeMillis());

        return "error/500";
    }

    /**
     * Handle SecurityException (403 - Security Violation).
     *
     * This is for generic SecurityException, distinct from AccessDeniedException.
     * Maps to 403 Forbidden with appropriate message.
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleSecurity(SecurityException ex, Model model) {
        log.warn("🔐 Security violation: {}", ex.getMessage());

        model.addAttribute("errorCode", 403);
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "Access denied: " + ex.getMessage());
        model.addAttribute("timestamp", System.currentTimeMillis());

        return "access-denied";
    }

    /**
     * Handle Exception (500 - Catch-All for Unhandled Errors).
     *
     * This is the ultimate safety net. If an exception reaches here,
     * it means no specific @ExceptionHandler matched it.
     *
     * IMPORTANT: Place this LAST in the class, as Spring processes
     * @ExceptionHandler methods in order and uses the most specific match.
     *
     * Error Prevention:
     * - Stack trace logged for admin debugging
     * - User shown generic message (security best practice)
     * - Error ID generated for support tickets
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("❌ Unhandled exception – Requires investigation!", ex);

        String errorId = "ERR-" + System.currentTimeMillis();
        log.error("Error ID: {} | Exception: {}", errorId, ex.getMessage());

        model.addAttribute("errorCode", 500);
        model.addAttribute("errorTitle", "Internal Server Error");
        model.addAttribute("errorMessage",
                "An unexpected error occurred. Error ID: " + errorId +
                " Please report this to the administrator if the problem persists.");
        model.addAttribute("errorId", errorId);
        model.addAttribute("timestamp", System.currentTimeMillis());

        return "error/500";
    }
}