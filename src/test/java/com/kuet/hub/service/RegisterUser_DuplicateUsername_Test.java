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
@SuppressWarnings("null")
class RegisterUser_DuplicateUsername_Test {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    @Test
    void registerUser_withDuplicateUsername_throwsException() {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("test@uni.edu");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setRole("BORROWER");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }
}
