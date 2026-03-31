package com.kuet.hub.service;

import com.kuet.hub.dto.RegistrationDto;
import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RegisterUser_ValidData_Test {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    @Test
    void registerUser_withValidData_savesUser() {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("test@uni.edu");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setRole("BORROWER");

        Role borrowerRole = new Role(Role.RoleName.ROLE_BORROWER);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@uni.edu")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_BORROWER)).thenReturn(Optional.of(borrowerRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.registerUser(dto);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@uni.edu");
        assertThat(result.getPassword()).isEqualTo("hashed");
        verify(userRepository, times(1)).save(any(User.class));
    }
}
