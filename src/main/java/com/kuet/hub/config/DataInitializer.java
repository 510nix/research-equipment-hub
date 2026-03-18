package com.kuet.hub.config;

import com.kuet.hub.entity.Category;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
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
            
            // Verify categories were persisted
            long categoryCount = categoryRepository.count();
            log.info("[STABILITY] Category initialization complete. Total categories in DB: {}", categoryCount);
            if (categoryCount == 0) {
                log.error("[STABILITY] WARNING: No categories persisted to database!");
            }
        } else {
            log.info("[STABILITY] Categories already exist. Count: {}", categoryRepository.count());
        }

        // Initialize Admin User
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@university.edu");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            
            final User finalAdmin = admin;
            roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                    .ifPresent(role -> finalAdmin.getRoles().add(role));
            userRepository.save(admin);
            log.info("[STABILITY] Admin user initialized");
            
            // Initialize Sample Items for Admin Provider
            if (itemRepository.findByOwnerWithCategory(admin).isEmpty()) { 
                log.info("[STABILITY] Creating sample items for admin user...");
                Category firstCategory = categoryRepository.findAll().stream().findFirst().orElse(null);
                if (firstCategory != null) {
                    Item sampleItem = new Item();
                    sampleItem.setTitle("High-Power Microscope");
                    sampleItem.setDescription("Professional laboratory microscope for research purposes. 1000x magnification, excellent condition.");
                    sampleItem.setCategory(firstCategory);
                    sampleItem.setOwner(admin);
                    sampleItem.setStatus(Item.ItemStatus.AVAILABLE);
                    sampleItem.setCondition(Item.Condition.NEW);
                    itemRepository.save(sampleItem);
                    log.info("[STABILITY] Sample item created: High-Power Microscope");
                } else {
                    log.warn("[STABILITY] No categories found. Skipping sample item creation.");
                }
            }
        }
        
        // Final verification
        log.info("[STABILITY] ========== DataInitializer Verification ==========");
        log.info("[STABILITY] Total Roles: {}", roleRepository.count());
        log.info("[STABILITY] Total Categories: {}", categoryRepository.count());
        log.info("[STABILITY] Total Users: {}", userRepository.count());
        log.info("[STABILITY] Total Items: {}", itemRepository.count());
        log.info("[STABILITY] ========== DataInitializer completed successfully! ==========");
    }
}