package com.kuet.hub.service;

import com.kuet.hub.dto.RequestDto;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RequestServiceTest {

    @Mock private RequestRepository requestRepository;
    @Mock private ItemRepository itemRepository;

    @InjectMocks private RequestService requestService;

    private User borrower;
    private User provider;
    private Item item;
    private Request request;
    private RequestDto requestDto;

    @BeforeEach
    void setUp() {
        borrower = new User();
        borrower.setId(1L);
        borrower.setUsername("borrower_user");
        borrower.setEmail("borrower@test.com");

        provider = new User();
        provider.setId(2L);
        provider.setUsername("provider_user");
        provider.setEmail("provider@test.com");

        item = new Item();
        item.setId(1L);
        item.setTitle("Test Microscope");
        item.setStatus(Item.ItemStatus.AVAILABLE);
        item.setOwner(provider);

        request = new Request();
        request.setId(1L);
        request.setBorrower(borrower);
        request.setItem(item);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));
        request.setMessage("I need this for my research");
        request.setStatus(Request.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        requestDto = new RequestDto();
        requestDto.setItemId(1L);
        requestDto.setStartDate(LocalDate.now().plusDays(1));
        requestDto.setEndDate(LocalDate.now().plusDays(5));
        requestDto.setMessage("I need this for my research");
    }

    @Test
    @DisplayName("Should throw exception when user attempts to borrow their own item")
    void testCreateRequest_UserIsOwner_ThrowsException() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requestService.createRequest(1L, provider, requestDto));

        assertEquals("You cannot request your own equipment", ex.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when endDate is before startDate")
    void testCreateRequest_EndDateBeforeStartDate_ThrowsException() {
        requestDto.setStartDate(LocalDate.now().plusDays(5));
        requestDto.setEndDate(LocalDate.now().plusDays(1));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requestService.createRequest(1L, borrower, requestDto));

        assertEquals("End date cannot be before start date", ex.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when item is not found")
    void testCreateRequest_ItemNotFound_ThrowsException() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requestService.createRequest(1L, borrower, requestDto));

        assertEquals("Equipment item not found", ex.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when item is not available")
    void testCreateRequest_ItemNotAvailable_ThrowsException() {
        item.setStatus(Item.ItemStatus.BORROWED);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requestService.createRequest(1L, borrower, requestDto));

        assertEquals("This equipment is not currently available for request", ex.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create request with valid data")
    void testCreateRequest_ValidData_SuccessfullySaved() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        Request created = requestService.createRequest(1L, borrower, requestDto);

        assertNotNull(created);
        assertEquals(borrower, created.getBorrower());
        assertEquals(item, created.getItem());
        assertEquals(Request.RequestStatus.PENDING, created.getStatus());
        verify(requestRepository, times(1)).save(any(Request.class));
    }

    @Test
    @DisplayName("Should approve request, mark item BORROWED, and auto-reject competing requests")
    void testApproveRequest_AuthorizedProvider_SuccessfullyApproved() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // FIX: approveRequest() calls findByItemAndStatusAndIdNot() for double-booking
        // prevention. Without this stub Mockito throws UnnecessaryStubbingException
        // or NullPointerException depending on strict mode.
        when(requestRepository.findByItemAndStatusAndIdNot(
                eq(item), eq(Request.RequestStatus.PENDING), eq(1L)))
                .thenReturn(Collections.emptyList());

        Request approved = requestService.approveRequest(1L, provider);

        assertNotNull(approved);
        assertEquals(Request.RequestStatus.APPROVED, approved.getStatus());
        // Verify item was marked BORROWED
        verify(itemRepository).save(any(Item.class));
        verify(requestRepository, atLeastOnce()).save(any(Request.class));
    }

    @Test
    @DisplayName("Should auto-reject competing PENDING requests when one is approved")
    void testApproveRequest_AutoRejectsCompetingRequests() {
        // A second borrower has a PENDING request for the same item
        User secondBorrower = new User();
        secondBorrower.setId(3L);
        secondBorrower.setUsername("second_borrower");

        Request competingRequest = new Request();
        competingRequest.setId(2L);
        competingRequest.setBorrower(secondBorrower);
        competingRequest.setItem(item);
        competingRequest.setStatus(Request.RequestStatus.PENDING);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(requestRepository.findByItemAndStatusAndIdNot(
                eq(item), eq(Request.RequestStatus.PENDING), eq(1L)))
                .thenReturn(List.of(competingRequest));

        requestService.approveRequest(1L, provider);

        // Verify the competing request was rejected
        assertEquals(Request.RequestStatus.REJECTED, competingRequest.getStatus());
        // save called at least twice: once for the approved request, once for the competing one
        verify(requestRepository, atLeast(2)).save(any(Request.class));
    }

    @Test
    @DisplayName("Should throw exception when non-owner provider tries to approve request")
    void testApproveRequest_UnauthorizedProvider_ThrowsException() {
        User unauthorizedProvider = new User();
        unauthorizedProvider.setId(99L);
        unauthorizedProvider.setUsername("other_provider");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> requestService.approveRequest(1L, unauthorizedProvider));

        assertEquals("You are not authorized to approve this request", ex.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve all requests for a borrower")
    void testGetRequestsForBorrower_ReturnsAllBorrowerRequests() {
        when(requestRepository.findByBorrower(borrower)).thenReturn(List.of(request));

        var requests = requestService.getRequestsForBorrower(borrower);

        assertEquals(1, requests.size());
        assertEquals(borrower, requests.get(0).getBorrower());
        verify(requestRepository).findByBorrower(borrower);
    }

    @Test
    @DisplayName("Should retrieve all requests for provider's items")
    void testGetRequestsForProvider_ReturnsAllProviderRequests() {
        when(requestRepository.findByItemOwner(provider)).thenReturn(List.of(request));

        var requests = requestService.getRequestsForProvider(provider);

        assertEquals(1, requests.size());
        assertEquals(provider, requests.get(0).getItem().getOwner());
        verify(requestRepository).findByItemOwner(provider);
    }

    @Test
    @DisplayName("Should reject request and set item back to AVAILABLE")
    void testRejectRequest_AuthorizedProvider_SuccessfullyRejected() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        Request rejected = requestService.rejectRequest(1L, provider);

        assertNotNull(rejected);
        assertEquals(Request.RequestStatus.REJECTED, rejected.getStatus());
        verify(itemRepository).save(any(Item.class)); // item status reset to AVAILABLE
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    @DisplayName("Should mark request COMPLETED and set item back to AVAILABLE")
    void testCompleteRequest_AuthorizedBorrower_SuccessfullyCompleted() {
        request.setStatus(Request.RequestStatus.APPROVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        Request completed = requestService.completeRequest(1L, borrower);

        assertNotNull(completed);
        assertEquals(Request.RequestStatus.COMPLETED, completed.getStatus());
        verify(itemRepository).save(any(Item.class)); // item returned to AVAILABLE
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    @DisplayName("Should throw exception when non-owner borrower tries to complete request")
    void testCompleteRequest_UnauthorizedBorrower_ThrowsException() {
        User otherBorrower = new User();
        otherBorrower.setId(99L);
        otherBorrower.setUsername("other_borrower");

        request.setStatus(Request.RequestStatus.APPROVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> requestService.completeRequest(1L, otherBorrower));

        assertEquals("You are not authorized to complete this request", ex.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve request by ID successfully")
    void testGetRequestById_RequestExists_ReturnsRequest() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        Request retrieved = requestService.getRequestById(1L);

        assertNotNull(retrieved);
        assertEquals(request.getId(), retrieved.getId());
        verify(requestRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when request not found by ID")
    void testGetRequestById_RequestNotFound_ThrowsException() {
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requestService.getRequestById(1L));

        assertEquals("Request not found", ex.getMessage());
    }
}