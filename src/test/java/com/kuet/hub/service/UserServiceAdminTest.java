package com.kuet.hub.service;


import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RequestRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService administrative operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Administrative Tests")
@SuppressWarnings("null")
class UserServiceAdminTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    // FIX: UserService now injects RequestRepository and ItemRepository to support
    // the Dependency Lock cascade logic in toggleUserEnabled().
    // Without these mocks, Mockito's @InjectMocks cannot wire the service and
    // any call into toggleUserEnabled() throws NullPointerException on
    // "this.requestRepository" (as seen in the test log at line 779).
    @Mock private RequestRepository requestRepository;
    @Mock private ItemRepository itemRepository;

    @InjectMocks private UserService userService;

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
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // FIX: The Dependency Lock check in enforceDependencyLock() calls
        // requestRepository.findByBorrowerAndStatus() before deactivating.
        // Stub it to return an empty list so the guard passes (no active borrows).
        when(requestRepository.findByBorrowerAndStatus(testUser, Request.RequestStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        // The cascade cleanup after deactivation calls findByBorrowerAndStatus(PENDING).
        when(requestRepository.findByBorrowerAndStatus(testUser, Request.RequestStatus.PENDING))
                .thenReturn(Collections.emptyList());

        userService.toggleUserEnabled(1L);

        assertFalse(testUser.isEnabled(), "User should be disabled after toggleUserEnabled");
        verify(userRepository, atLeastOnce()).findById(1L);
    }

    @Test
    @DisplayName("toggleUserEnabled - Enables disabled user successfully")
    void testToggleUserEnabled_FromDisabledToEnabled() {
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Re-activation path: no cascade needed, no requestRepository calls expected.

        userService.toggleUserEnabled(1L);

        assertTrue(testUser.isEnabled(), "User should be enabled after toggleUserEnabled");
        verify(userRepository, atLeastOnce()).findById(1L);
    }

    @Test
    @DisplayName("toggleUserEnabled - Throws exception when user not found")
    void testToggleUserEnabled_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.toggleUserEnabled(999L));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("toggleUserEnabled - Blocks deactivation if borrower has active borrow")
    void testToggleUserEnabled_BorrowerWithActiveBorrow_ThrowsIllegalState() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Simulate an active (APPROVED) request — this should block deactivation
        Request activeRequest = new Request();
        activeRequest.setId(1L);
        activeRequest.setStatus(Request.RequestStatus.APPROVED);
        when(requestRepository.findByBorrowerAndStatus(testUser, Request.RequestStatus.APPROVED))
                .thenReturn(List.of(activeRequest));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userService.toggleUserEnabled(1L));

        assertTrue(exception.getMessage().contains("item(s) borrowed"),
                "Should explain why deactivation is blocked");
        assertTrue(testUser.isEnabled(), "User should remain enabled — deactivation was blocked");
    }

    @Test
    @DisplayName("findById - Returns user when found")
    void testFindById_UserFound_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User foundUser = userService.findById(1L);

        assertNotNull(foundUser);
        assertEquals(1L, foundUser.getId());
        assertEquals("testuser", foundUser.getUsername());
        assertEquals("test@example.com", foundUser.getEmail());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById - Throws exception when user not found")
    void testFindById_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.findById(999L));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, times(1)).findById(999L);
    }
}