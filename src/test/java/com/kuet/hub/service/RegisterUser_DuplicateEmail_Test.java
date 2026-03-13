package com.kuet.hub.service;

import com.kuet.hub.dto.RegistrationDto;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUser_DuplicateEmail_Test {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    @Test
    void registerUser_withDuplicateEmail_throwsException() {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("test@uni.edu");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setRole("BORROWER");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@uni.edu")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }
}
