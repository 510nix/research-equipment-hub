package com.kuet.hub.config;

import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        }

        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@university.edu");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                    .ifPresent(role -> admin.getRoles().add(role));
            userRepository.save(admin);
        }
    }
}