package com.kuet.hub.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * FIX: Handle NoResourceFoundException BEFORE the generic Exception handler.
     *
     * Root cause of the favicon 500:
     * Browsers silently request /favicon.ico on every page load. When no
     * favicon file exists in /static, Spring throws NoResourceFoundException.
     * This was falling into the generic Exception handler below, which called
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

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.error("[ERROR HANDLER] IllegalArgumentException caught: {}", ex.getMessage(), ex);
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorTitle", "Invalid Request");
        return "error/generic";
    }

    @ExceptionHandler(SecurityException.class)
    public String handleSecurity(SecurityException ex, Model model,
                                 HttpServletResponse response) {
        log.error("[ERROR HANDLER] SecurityException caught: {}", ex.getMessage(), ex);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("errorMessage", "Access denied: " + ex.getMessage());
        model.addAttribute("errorTitle", "Access Denied");
        return "error/generic";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model,
                                HttpServletResponse response) {
        log.error("[ROOT-CAUSE] Fatal error during request: {}", ex.getMessage(), ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        model.addAttribute("errorTitle", "Server Error");
        return "error/generic";
    }
}