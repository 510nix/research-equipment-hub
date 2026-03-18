package com.kuet.hub.service;

import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService administrative operations.
 * Tests user management functionality including toggle status and retrieval.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Administrative Tests")
@SuppressWarnings("null")
class UserServiceAdminTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName(Role.RoleName.ROLE_BORROWER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setEnabled(true);
        testUser.getRoles().add(testRole);
    }

    @Test
    @DisplayName("toggleUserEnabled - Disables enabled user successfully")
    void testToggleUserEnabled_FromEnabledToDisabled() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.toggleUserEnabled(1L);

        // Assert
        assertFalse(testUser.isEnabled(), "User should be disabled after toggleUserEnabled");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("toggleUserEnabled - Enables disabled user successfully")
    void testToggleUserEnabled_FromDisabledToEnabled() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.toggleUserEnabled(1L);

        // Assert
        assertTrue(testUser.isEnabled(), "User should be enabled after toggleUserEnabled");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("toggleUserEnabled - Throws exception when user not found")
    void testToggleUserEnabled_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.toggleUserEnabled(999L),
                "Should throw IllegalArgumentException when user not found");
        
        assertTrue(exception.getMessage().contains("User not found"),
                "Exception message should indicate user not found");
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("findById - Returns user when found")
    void testFindById_UserFound_ReturnsUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User foundUser = userService.findById(1L);

        // Assert
        assertNotNull(foundUser, "Should return user");
        assertEquals(1L, foundUser.getId(), "User ID should match");
        assertEquals("testuser", foundUser.getUsername(), "Username should match");
        assertEquals("test@example.com", foundUser.getEmail(), "Email should match");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById - Throws exception when user not found")
    void testFindById_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.findById(999L),
                "Should throw IllegalArgumentException when user not found");
        
        assertTrue(exception.getMessage().contains("User not found"),
                "Exception message should indicate user not found");
        verify(userRepository, times(1)).findById(999L);
    }
}
