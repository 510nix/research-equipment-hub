package com.kuet.hub.service;

import com.kuet.hub.dto.RegistrationDto;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RequestRepository;
import com.kuet.hub.repository.RoleRepository;
import com.kuet.hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RequestRepository requestRepository;
    private final ItemRepository itemRepository;

    public User registerUser(RegistrationDto dto) {
        if (userRepository.existsByUsername(dto.getUsername()))
            throw new IllegalArgumentException("Username already taken: " + dto.getUsername());
        if (userRepository.existsByEmail(dto.getEmail()))
            throw new IllegalArgumentException("Email already registered: " + dto.getEmail());
        if (!dto.getPassword().equals(dto.getConfirmPassword()))
            throw new IllegalArgumentException("Passwords do not match");

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        Role.RoleName roleName = "PROVIDER".equalsIgnoreCase(dto.getRole())
                ? Role.RoleName.ROLE_PROVIDER : Role.RoleName.ROLE_BORROWER;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /**
     * Toggle a user's enabled/disabled status with Dependency Lock enforcement.
     *
     * DEACTIVATION GUARD RULES (throws IllegalStateException if violated):
     *
     * Rule 1 — BORROWER with active borrow:
     *   A borrower cannot be deactivated if they have any APPROVED requests
     *   (i.e. they are currently holding borrowed equipment).
     *   Reason: deactivating them would make the item permanently stuck as BORROWED
     *   since no one can click "Return" on behalf of a deactivated account.
     *
     * Rule 2 — PROVIDER with active borrow on their items:
     *   A provider cannot be deactivated if any of their items are currently BORROWED.
     *   Reason: the borrower still holds the physical equipment and needs to return it.
     *   The return flow (completeRequest) updates item status — if the provider is
     *   deactivated first, the item record becomes orphaned in BORROWED state.
     *
     * WHAT IS ALLOWED when deactivating (no active borrows):
     *   - PENDING requests by the borrower → REJECTED (auto-cleanup)
     *   - PENDING requests for provider's items → REJECTED (auto-cleanup)
     *   - Provider's items → hidden from browse (owner.enabled = false filters them)
     *
     * RE-ACTIVATION:
     *   No cascade needed. The user regains login access and their items
     *   reappear on the browse page automatically (the browse query filters
     *   by owner.enabled = true, so re-enabling makes them visible again).
     */
    public void toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        boolean currentlyEnabled = user.isEnabled();

        if (currentlyEnabled) {
            // Enforce Dependency Lock before deactivating
            enforceDependencyLock(user);
            // Guards passed — safe to deactivate and cascade
            user.setEnabled(false);
            userRepository.save(user);
            log.info("[ADMIN] User {} deactivated — running cascade cleanup", user.getUsername());
            cascadeDeactivation(user);
        } else {
            // Re-activation: simply re-enable, items reappear automatically
            user.setEnabled(true);
            userRepository.save(user);
            log.info("[ADMIN] User {} re-activated", user.getUsername());
        }
    }

    /**
     * Dependency Lock check — throws IllegalStateException if the user is
     * involved in any active (BORROWED) transaction that blocks deactivation.
     */
    private void enforceDependencyLock(User user) {
        boolean isBorrower = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_BORROWER);
        boolean isProvider = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_PROVIDER);

        if (isBorrower) {
            // Rule 1: borrower must not have any APPROVED (currently borrowed) requests
            List<Request> activeBorrows = requestRepository
                    .findByBorrowerAndStatus(user, Request.RequestStatus.APPROVED);
            if (!activeBorrows.isEmpty()) {
                log.warn("[ADMIN] Cannot deactivate borrower {} — has {} active borrow(s)",
                        user.getUsername(), activeBorrows.size());
                throw new IllegalStateException(
                        "Cannot deactivate '" + user.getUsername() + "': they currently have "
                        + activeBorrows.size() + " item(s) borrowed. "
                        + "The borrower must return all equipment before their account can be deactivated.");
            }
        }

        if (isProvider) {
            // Rule 2: none of the provider's items may currently be BORROWED
            List<Item> providerItems = itemRepository.findByOwnerWithCategory(user);
            long borrowedCount = providerItems.stream()
                    .filter(i -> i.getStatus() == Item.ItemStatus.BORROWED)
                    .count();
            if (borrowedCount > 0) {
                log.warn("[ADMIN] Cannot deactivate provider {} — {} item(s) currently borrowed",
                        user.getUsername(), borrowedCount);
                throw new IllegalStateException(
                        "Cannot deactivate provider '" + user.getUsername() + "': "
                        + borrowedCount + " of their item(s) are currently borrowed. "
                        + "All equipment must be returned before this provider can be deactivated.");
            }
        }
    }

    /**
     * Cascade cleanup after a safe deactivation (no active borrows guaranteed).
     * Cancels all PENDING requests — no items are currently BORROWED so no
     * item status changes are needed.
     */
    private void cascadeDeactivation(User user) {
        boolean isBorrower = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_BORROWER);
        boolean isProvider = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_PROVIDER);

        if (isBorrower) {
            // Cancel all pending requests made by this borrower
            List<Request> pending = requestRepository
                    .findByBorrowerAndStatus(user, Request.RequestStatus.PENDING);
            for (Request r : pending) {
                r.setStatus(Request.RequestStatus.REJECTED);
                requestRepository.save(r);
                log.info("[ADMIN] Auto-rejected PENDING request {} for deactivated borrower {}",
                        r.getId(), user.getUsername());
            }
            log.info("[ADMIN] Borrower cascade: rejected {} pending request(s)", pending.size());
        }

        if (isProvider) {
            // Cancel all pending requests for this provider's items
            List<Request> pendingForProvider = requestRepository.findByItemOwner(user)
                    .stream()
                    .filter(r -> r.getStatus() == Request.RequestStatus.PENDING)
                    .toList();
            for (Request r : pendingForProvider) {
                r.setStatus(Request.RequestStatus.REJECTED);
                requestRepository.save(r);
                log.info("[ADMIN] Auto-rejected PENDING request {} (provider {} deactivated)",
                        r.getId(), user.getUsername());
            }
            // Items remain in DB — they are hidden from browse by owner.enabled=true filter.
            // They will reappear automatically when the provider is re-activated.
            log.info("[ADMIN] Provider cascade: rejected {} pending request(s). "
                    + "Items hidden from browse (owner.enabled=false).", pendingForProvider.size());
        }
    }
}