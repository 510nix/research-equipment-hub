package com.kuet.hub.service;

import com.kuet.hub.dto.RequestDto;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for managing equipment requests.
 * Encapsulates all borrowing business logic and data integrity validation.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RequestService {

    private final RequestRepository requestRepository;
    private final ItemRepository itemRepository;

    /**
     * Create a new equipment request.
     *
     * Validation:
     * - Item must exist and be marked as AVAILABLE
     * - End date must not be before start date
     * - Dates must be in the future (optional: can be adjusted based on requirements)
     *
     * @param itemId the ID of the equipment being requested
     * @param borrower the User (student) making the request
     * @param requestDto contains startDate, endDate, and a message
     * @return the newly created Request entity
     * @throws IllegalArgumentException if validation fails
     */
    public Request createRequest(Long itemId, User borrower, RequestDto requestDto) {
        log.info("[REQUEST] Creating new request for borrower {} on item {}", borrower.getUsername(), itemId);

        // Validate item exists and is available
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("[REQUEST] Item {} not found", itemId);
                    return new IllegalArgumentException("Equipment item not found");
                });

        if (!item.getStatus().equals(Item.ItemStatus.AVAILABLE)) {
            log.warn("[REQUEST] Item {} is not available (status: {})", itemId, item.getStatus());
            throw new IllegalArgumentException("This equipment is not currently available for request");
        }

        // Validate that borrower is not the owner
        if (item.getOwner().getId().equals(borrower.getId())) {
            log.warn("[REQUEST] User {} attempted to request their own equipment", borrower.getUsername());
            throw new IllegalArgumentException("You cannot request your own equipment");
        }

        // Validate dates
        validateDates(requestDto.getStartDate(), requestDto.getEndDate());

        // Create and persist the request
        Request request = new Request();
        request.setBorrower(borrower);
        request.setItem(item);
        request.setStartDate(requestDto.getStartDate());
        request.setEndDate(requestDto.getEndDate());
        request.setMessage(requestDto.getMessage());
        request.setStatus(Request.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);
        log.info("[REQUEST] Request {} created successfully for borrower {} on item {}", 
                savedRequest.getId(), borrower.getUsername(), itemId);

        return savedRequest;
    }

    /**
     * Retrieve all requests made by a specific borrower.
     * Used to populate the borrower's "My Requests" dashboard.
     *
     * @param borrower the User whose requests should be fetched
     * @return a list of all requests made by the borrower
     */
    public List<Request> getRequestsForBorrower(User borrower) {
        log.info("[REQUEST] Fetching requests for borrower {}", borrower.getUsername());
        List<Request> requests = requestRepository.findByBorrower(borrower);
        log.info("[REQUEST] Found {} requests for borrower {}", requests.size(), borrower.getUsername());
        return requests;
    }

    /**
     * Retrieve all incoming requests for items owned by a provider.
     * Used to populate the provider's request management dashboard.
     *
     * @param provider the User (provider) whose items' requests should be fetched
     * @return a list of all requests for the provider's equipment
     */
    public List<Request> getRequestsForProvider(User provider) {
        log.info("[REQUEST] Fetching incoming requests for provider {}", provider.getUsername());
        List<Request> requests = requestRepository.findByItemOwner(provider);
        log.info("[REQUEST] Found {} incoming requests for provider {}", requests.size(), provider.getUsername());
        return requests;
    }

    /**
     * Approve a request (provider action).
     * Changes request status from PENDING to APPROVED.
     *
     * @param requestId the ID of the request to approve
     * @param provider the User (provider) approving the request
     * @return the updated Request entity
     * @throws IllegalArgumentException if request not found or doesn't belong to provider's items
     */
    public Request approveRequest(Long requestId, User provider) {
        log.info("[REQUEST] Provider {} attempting to approve request {}", provider.getUsername(), requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("[REQUEST] Request {} not found", requestId);
                    return new IllegalArgumentException("Request not found");
                });

        // Verify provider owns the requested item
        if (!request.getItem().getOwner().getId().equals(provider.getId())) {
            log.warn("[REQUEST] Provider {} attempted to approve request they don't own", provider.getUsername());
            throw new SecurityException("You are not authorized to approve this request");
        }

        request.setStatus(Request.RequestStatus.APPROVED);
        Request updatedRequest = requestRepository.save(request);
        log.info("[REQUEST] Request {} approved by provider {}", requestId, provider.getUsername());

        return updatedRequest;
    }

    /**
     * Reject a request (provider action).
     * Changes request status from PENDING to REJECTED.
     *
     * @param requestId the ID of the request to reject
     * @param provider the User (provider) rejecting the request
     * @return the updated Request entity
     * @throws IllegalArgumentException if request not found or doesn't belong to provider's items
     */
    public Request rejectRequest(Long requestId, User provider) {
        log.info("[REQUEST] Provider {} attempting to reject request {}", provider.getUsername(), requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("[REQUEST] Request {} not found", requestId);
                    return new IllegalArgumentException("Request not found");
                });

        // Verify provider owns the requested item
        if (!request.getItem().getOwner().getId().equals(provider.getId())) {
            log.warn("[REQUEST] Provider {} attempted to reject request they don't own", provider.getUsername());
            throw new SecurityException("You are not authorized to reject this request");
        }

        request.setStatus(Request.RequestStatus.REJECTED);
        Request updatedRequest = requestRepository.save(request);
        log.info("[REQUEST] Request {} rejected by provider {}", requestId, provider.getUsername());

        return updatedRequest;
    }

    /**
     * Mark a request as COMPLETED (borrower action after returning equipment).
     *
     * @param requestId the ID of the request to mark as completed
     * @param borrower the User (borrower) marking the request complete
     * @return the updated Request entity
     * @throws IllegalArgumentException if request not found or doesn't belong to borrower
     */
    public Request completeRequest(Long requestId, User borrower) {
        log.info("[REQUEST] Borrower {} marking request {} as completed", borrower.getUsername(), requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("[REQUEST] Request {} not found", requestId);
                    return new IllegalArgumentException("Request not found");
                });

        // Verify borrower owns the request
        if (!request.getBorrower().getId().equals(borrower.getId())) {
            log.warn("[REQUEST] Borrower {} attempted to complete request they don't own", borrower.getUsername());
            throw new SecurityException("You are not authorized to complete this request");
        }

        request.setStatus(Request.RequestStatus.COMPLETED);
        Request updatedRequest = requestRepository.save(request);
        log.info("[REQUEST] Request {} marked as completed by borrower {}", requestId, borrower.getUsername());

        return updatedRequest;
    }

    /**
     * Retrieve a single request by ID.
     *
     * @param requestId the ID of the request
     * @return the Request entity
     * @throws IllegalArgumentException if request not found
     */
    public Request getRequestById(Long requestId) {
        log.info("[REQUEST] Fetching request {}", requestId);
        return requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("[REQUEST] Request {} not found", requestId);
                    return new IllegalArgumentException("Request not found");
                });
    }

    /**
     * Validate that end date is not before start date.
     * Throws IllegalArgumentException if validation fails.
     *
     * @param startDate the start date of the request
     * @param endDate the end date of the request
     * @throws IllegalArgumentException if endDate is before startDate
     */
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            log.warn("[REQUEST] Date validation failed: endDate {} is before startDate {}", endDate, startDate);
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        log.info("[REQUEST] Date validation passed: {} to {}", startDate, endDate);
    }
}
