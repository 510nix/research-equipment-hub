package com.kuet.hub.config;

import com.kuet.hub.entity.Category;
import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("[STABILITY] DataInitializer starting...");
        
        // Initialize Roles
        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("[STABILITY] Role initialized: {}", roleName);
            }
        }

        // Initialize Default Categories
        if (categoryRepository.count() == 0) {
            log.info("[STABILITY] No categories found. Seeding default categories...");
            
            String[] defaultCategories = {
                "Electronics",
                "Lab Equipment",
                "Mechanical Tools",
                "Textbooks"
            };
            
            for (String categoryName : defaultCategories) {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription("Default " + categoryName + " category");
                categoryRepository.save(category);
                log.info("[STABILITY] Category created: {}", categoryName);
            }
        } else {
            log.info("[STABILITY] Categories already exist. Skipping category initialization.");
        }

        // Initialize Admin User
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@university.edu");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                    .ifPresent(role -> admin.getRoles().add(role));
            userRepository.save(admin);
            log.info("[STABILITY] Admin user initialized");
        }
        
        log.info("[STABILITY] DataInitializer completed successfully!");
    }
}