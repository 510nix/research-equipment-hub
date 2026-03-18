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

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RequestService {

    private final RequestRepository requestRepository;
    private final ItemRepository itemRepository;

    public Request createRequest(Long itemId, User borrower, RequestDto requestDto) {
        log.info("[REQUEST] Creating request for borrower {} on item {}", borrower.getUsername(), itemId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment item not found"));

        if (!item.getStatus().equals(Item.ItemStatus.AVAILABLE))
            throw new IllegalArgumentException("This equipment is not currently available for request");

        if (item.getOwner().getId().equals(borrower.getId()))
            throw new IllegalArgumentException("You cannot request your own equipment");

        validateDates(requestDto.getStartDate(), requestDto.getEndDate());

        Request request = new Request();
        request.setBorrower(borrower);
        request.setItem(item);
        request.setStartDate(requestDto.getStartDate());
        request.setEndDate(requestDto.getEndDate());
        request.setMessage(requestDto.getMessage());
        request.setStatus(Request.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        Request saved = requestRepository.save(request);
        log.info("[REQUEST] Request {} created for borrower {} on item {}",
                saved.getId(), borrower.getUsername(), itemId);
        return saved;
    }

    public List<Request> getRequestsForBorrower(User borrower) {
        return requestRepository.findByBorrower(borrower);
    }

    public List<Request> getRequestsForProvider(User provider) {
        return requestRepository.findByItemOwner(provider);
    }

    /**
     * Approve a request with double-booking prevention.
     *
     * Step 1 — Security check: verify the provider owns the item being requested.
     *
     * Step 2 — Approve this request and mark the item as BORROWED.
     *
     * Step 3 — DOUBLE-BOOKING PREVENTION (Gatekeeper):
     *   After approval, find all remaining PENDING requests for the same item
     *   (excluding the one just approved) and automatically REJECT them.
     *
     *   Why this works:
     *   - Item is now BORROWED, so no new requests can be created (createRequest
     *     checks item.status == AVAILABLE before proceeding).
     *   - But requests submitted BEFORE approval (while item was still AVAILABLE)
     *     may still be PENDING. These are now impossible to fulfill — auto-rejecting
     *     them immediately notifies borrowers and keeps the queue clean.
     *
     *   All changes (approve one + reject others + update item) are inside a single
     *   @Transactional method — they are atomic. No partial state is possible.
     */
    public Request approveRequest(Long requestId, User provider) {
        log.info("[REQUEST] Provider {} approving request {}", provider.getUsername(), requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getItem().getOwner().getId().equals(provider.getId()))
            throw new SecurityException("You are not authorized to approve this request");

        // Step 2: approve this request + mark item as BORROWED
        Item item = request.getItem();
        item.setStatus(Item.ItemStatus.BORROWED);
        itemRepository.save(item);
        log.info("[REQUEST] Item {} → BORROWED", item.getId());

        request.setStatus(Request.RequestStatus.APPROVED);
        Request approved = requestRepository.save(request);
        log.info("[REQUEST] Request {} → APPROVED", requestId);

        // Step 3: auto-reject all other PENDING requests for the same item
        List<Request> competing = requestRepository.findByItemAndStatusAndIdNot(
                item, Request.RequestStatus.PENDING, requestId);

        if (!competing.isEmpty()) {
            log.info("[REQUEST] Double-booking prevention: auto-rejecting {} competing request(s) for item {}",
                    competing.size(), item.getId());
            for (Request c : competing) {
                c.setStatus(Request.RequestStatus.REJECTED);
                requestRepository.save(c);
                log.info("[REQUEST] Auto-rejected competing request {} (borrower: {})",
                        c.getId(), c.getBorrower().getUsername());
            }
        }

        return approved;
    }

    /**
     * Reject a request and ensure the item is AVAILABLE.
     * Item is set back to AVAILABLE explicitly to handle any edge cases.
     */
    public Request rejectRequest(Long requestId, User provider) {
        log.info("[REQUEST] Provider {} rejecting request {}", provider.getUsername(), requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getItem().getOwner().getId().equals(provider.getId()))
            throw new SecurityException("You are not authorized to reject this request");

        Item item = request.getItem();
        item.setStatus(Item.ItemStatus.AVAILABLE);
        itemRepository.save(item);
        log.info("[REQUEST] Item {} → AVAILABLE (after rejection)", item.getId());

        request.setStatus(Request.RequestStatus.REJECTED);
        Request updated = requestRepository.save(request);
        log.info("[REQUEST] Request {} → REJECTED", requestId);
        return updated;
    }

    /**
     * Mark a request as COMPLETED (borrower clicks "Mark as Returned").
     * Item is returned to AVAILABLE so it can be requested again.
     */
    public Request completeRequest(Long requestId, User borrower) {
        log.info("[REQUEST] Borrower {} completing request {}", borrower.getUsername(), requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getBorrower().getId().equals(borrower.getId()))
            throw new SecurityException("You are not authorized to complete this request");

        Item item = request.getItem();
        item.setStatus(Item.ItemStatus.AVAILABLE);
        itemRepository.save(item);
        log.info("[REQUEST] Item {} → AVAILABLE (equipment returned)", item.getId());

        request.setStatus(Request.RequestStatus.COMPLETED);
        Request updated = requestRepository.save(request);
        log.info("[REQUEST] Request {} → COMPLETED", requestId);
        return updated;
    }

    public Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate))
            throw new IllegalArgumentException("End date cannot be before start date");
    }
}