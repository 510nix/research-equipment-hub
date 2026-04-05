package com.kuet.hub.config;

import com.kuet.hub.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig: Spring Security configuration for the Research Equipment Hub.
 *
 * This class establishes the entire authentication and authorization framework:
 *
 * LAYERS OF SECURITY:
 * 1. URL-level: @authorizeHttpRequests controls which roles can access which URLs
 * 2. Method-level: @PreAuthorize on controller/service methods for dynamic checks
 * 3. Field-level: Service layer validates ownership (e.g., only owner can edit item)
 * 4. Database level: FK constraints enforce referential integrity
 *
 * EXCEPTION HANDLING INTEGRATION:
 * - Unauthorized (403): Handled by CustomAccessDeniedHandler (redirects to /access-denied)
 * - Not Authenticated (401): Redirects to login page
 * - Application errors: Handled by GlobalExceptionHandler
 *
 * CSRF PROTECTION:
 * - Enabled by default for state-changing requests (POST, PUT, DELETE)
 * - Token auto-generated in forms by Thymeleaf
 * - Checked on every form submission
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    /**
     * Configure the security filter chain for HTTP security.
     *
     * ORDER MATTERS in requests() matchers:
     * 1. Most specific patterns first (e.g., /admin/**)
     * 2. Broader patterns later (e.g., /requests/**)
     * 3. Least specific pattern last (e.g., anyRequest())
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ACCESS (no authentication required)
                        .requestMatchers(
                                "/",
                                "/auth/register",
                                "/auth/login",
                                "/css/**",
                                "/js/**",
                                "/lib/**",
                                "/images/**",
                                // FIX: Browsers auto-request favicon.ico on every page load.
                                // Without this, Spring Security intercepts the request,
                                // finds no static file, and throws NoResourceFoundException
                                // which the GlobalExceptionHandler catches as a 404.
                                // This is now plain 404, not a full HTML error page.
                                "/favicon.ico",
                                // Also permit other common browser auto-requests
                                "/robots.txt",
                                "/error",
                                "/access-denied"
                        ).permitAll()

                        // BORROWER-ONLY ACCESS (role check at URL level)
                        .requestMatchers("/browse", "/browse/**", "/my-requests", "/my-requests/**")
                                .hasRole("BORROWER")

                        // REQUEST ENDPOINTS (mixed access)
                        // Specific role checks happen in RequestController
                        .requestMatchers("/requests/**").authenticated()

                        // PROVIDER-ONLY ACCESS (role check at URL level)
                        .requestMatchers("/provider/**").hasRole("PROVIDER")

                        // ADMIN-ONLY ACCESS (role check at URL level)
                        // Dependency Lock enforcement happens in AdminController
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // DEFAULT: Other authenticated endpoints require login
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/dashboard", true)  // Always redirect to dashboard (smart routing)
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        // Custom handler for 403 Forbidden (role/permission denied)
                        // Logs the unauthorized access and redirects to /access-denied
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}