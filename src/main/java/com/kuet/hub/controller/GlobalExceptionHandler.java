package com.kuet.hub.controller;

//package com.university.researchhub.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.error("[ERROR HANDLER] IllegalArgumentException caught: {}", ex.getMessage(), ex);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/generic";
    }

    @ExceptionHandler(SecurityException.class)
    public String handleSecurity(SecurityException ex, Model model) {
        log.error("[ERROR HANDLER] SecurityException caught: {}", ex.getMessage(), ex);
        model.addAttribute("errorMessage", "Access denied: " + ex.getMessage());
        return "access-denied";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("[ERROR HANDLER] Unexpected exception caught: {}", ex.getMessage(), ex);
        model.addAttribute("errorMessage", "An unexpected error occurred.");
        return "error/generic";
    }
}
