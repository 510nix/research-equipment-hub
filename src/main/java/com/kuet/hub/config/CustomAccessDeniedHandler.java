package com.kuet.hub.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CustomAccessDeniedHandler: Spring Security component for handling 403 Forbidden errors.
 *
 * When a user with ROLE_BORROWER tries to access /admin/dashboard,
 * Spring Security identifies the role mismatch and invokes this handler
 * instead of showing a default error page or redirecting abruptly.
 *
 * WORKFLOW:
 * 1. User attempts to access protected resource (e.g., /admin/dashboard)
 * 2. Spring Security checks user's roles against required roles
 * 3. Role mismatch detected → AccessDeniedException thrown
 * 4. filter catches exception → invokes this handler
 * 5. Handler logs the violation and redirects to /access-denied
 * 6. DashboardController.accessDenied() maps to Thymeleaf template
 * 7. Custom branded 403 page shown to user
 *
 * LOGGING:
 * - Logs (WARN level) all 403 attempts with user info
 * - Helps admin identify permission misconfiguration or probing attacks
 * - Example log: "User 'ali_borrower' denied access to /admin/dashboard"
 *
 * SECURITY IMPLICATIONS:
 * - Do NOT expose why access was denied (prevents user enumeration)
 * - Do NOT show system internals (prevents reconnaissance)
 * - Do show helpful redirect to appropriate resource
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Handle access denied scenario.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param accessDeniedException The exception thrown by Spring Security
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String username = request.getUserPrincipal() != null ? 
                request.getUserPrincipal().getName() : "ANONYMOUS";
        String requestedUrl = request.getRequestURI();

        log.warn("🚫 Access Denied (403) - User: '{}' | Requested: {} | Reason: {}",
                username, requestedUrl, accessDeniedException.getMessage());

        // Redirect to custom access-denied page
        response.sendRedirect(request.getContextPath() + "/access-denied");
    }
}
