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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RequestService
 * Tests the business logic layer for equipment request management
 */
@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private RequestService requestService;

    private User borrower;
    private User provider;
    private Item item;
    private Request request;
    private RequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup test data
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
        // Arrange - borrower is the same as item owner
        RequestDto ownerRequestDto = new RequestDto();
        ownerRequestDto.setItemId(1L);
        ownerRequestDto.setStartDate(LocalDate.now().plusDays(1));
        ownerRequestDto.setEndDate(LocalDate.now().plusDays(5));
        ownerRequestDto.setMessage("I need this for my research");

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        // Act & Assert - provider cannot request their own item
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestService.createRequest(2L, provider, ownerRequestDto); // provider is owner
        });

        assertEquals("You cannot request your own equipment", exception.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when endDate is before startDate")
    void testCreateRequest_EndDateBeforeStartDate_ThrowsException() {
        // Arrange
        requestDto.setStartDate(LocalDate.now().plusDays(5));
        requestDto.setEndDate(LocalDate.now().plusDays(1)); // Before start date
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestService.createRequest(1L, borrower, requestDto);
        });

        assertEquals("End date cannot be before start date", exception.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when item is not found")
    void testCreateRequest_ItemNotFound_ThrowsException() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestService.createRequest(1L, borrower, requestDto);
        });

        assertEquals("Equipment item not found", exception.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when item is not available")
    void testCreateRequest_ItemNotAvailable_ThrowsException() {
        // Arrange
        item.setStatus(Item.ItemStatus.BORROWED);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestService.createRequest(1L, borrower, requestDto);
        });

        assertEquals("This equipment is not currently available for request", exception.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create request with valid data")
    void testCreateRequest_ValidData_SuccessfullySaved() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        // Act
        Request createdRequest = requestService.createRequest(1L, borrower, requestDto);

        // Assert
        assertNotNull(createdRequest);
        assertEquals(borrower, createdRequest.getBorrower());
        assertEquals(item, createdRequest.getItem());
        assertEquals(Request.RequestStatus.PENDING, createdRequest.getStatus());
        assertEquals(requestDto.getMessage(), createdRequest.getMessage());
        verify(requestRepository, times(1)).save(any(Request.class));
    }

    @Test
    @DisplayName("Should approve request successfully when provider is authorized")
    void testApproveRequest_AuthorizedProvider_SuccessfullyApproved() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        // Act
        Request approvedRequest = requestService.approveRequest(1L, provider);

        // Assert
        assertNotNull(approvedRequest);
        assertEquals(Request.RequestStatus.APPROVED, approvedRequest.getStatus());
        verify(requestRepository, times(1)).save(any(Request.class));
    }

    @Test
    @DisplayName("Should throw exception when non-owner provider tries to approve request")
    void testApproveRequest_UnauthorizedProvider_ThrowsException() {
        // Arrange
        User unauthorizedProvider = new User();
        unauthorizedProvider.setId(99L);
        unauthorizedProvider.setUsername("other_provider");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            requestService.approveRequest(1L, unauthorizedProvider);
        });

        assertEquals("You are not authorized to approve this request", exception.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve all requests for a borrower")
    void testGetRequestsForBorrower_ReturnsAllBorrowerRequests() {
        // Arrange
        when(requestRepository.findByBorrower(borrower)).thenReturn(java.util.List.of(request));

        // Act
        var requests = requestService.getRequestsForBorrower(borrower);

        // Assert
        assertEquals(1, requests.size());
        assertEquals(borrower, requests.get(0).getBorrower());
        verify(requestRepository, times(1)).findByBorrower(borrower);
    }

    @Test
    @DisplayName("Should retrieve all requests for provider's items")
    void testGetRequestsForProvider_ReturnsAllProviderRequests() {
        // Arrange
        when(requestRepository.findByItemOwner(provider)).thenReturn(java.util.List.of(request));

        // Act
        var requests = requestService.getRequestsForProvider(provider);

        // Assert
        assertEquals(1, requests.size());
        assertEquals(provider, requests.get(0).getItem().getOwner());
        verify(requestRepository, times(1)).findByItemOwner(provider);
    }

    @Test
    @DisplayName("Should reject request successfully when provider is authorized")
    void testRejectRequest_AuthorizedProvider_SuccessfullyRejected() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        // Act
        Request rejectedRequest = requestService.rejectRequest(1L, provider);

        // Assert
        assertNotNull(rejectedRequest);
        assertEquals(Request.RequestStatus.REJECTED, rejectedRequest.getStatus());
        verify(requestRepository, times(1)).save(any(Request.class));
    }

    @Test
    @DisplayName("Should mark request as completed successfully when borrower is authorized")
    void testCompleteRequest_AuthorizedBorrower_SuccessfullyCompleted() {
        // Arrange
        request.setStatus(Request.RequestStatus.APPROVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        // Act
        Request completedRequest = requestService.completeRequest(1L, borrower);

        // Assert
        assertNotNull(completedRequest);
        assertEquals(Request.RequestStatus.COMPLETED, completedRequest.getStatus());
        verify(requestRepository, times(1)).save(any(Request.class));
    }

    @Test
    @DisplayName("Should throw exception when non-owner borrower tries to complete request")
    void testCompleteRequest_UnauthorizedBorrower_ThrowsException() {
        // Arrange
        User otherBorrower = new User();
        otherBorrower.setId(99L);
        otherBorrower.setUsername("other_borrower");

        request.setStatus(Request.RequestStatus.APPROVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            requestService.completeRequest(1L, otherBorrower);
        });

        assertEquals("You are not authorized to complete this request", exception.getMessage());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve request by ID successfully")
    void testGetRequestById_RequestExists_ReturnsRequest() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        // Act
        Request retrievedRequest = requestService.getRequestById(1L);

        // Assert
        assertNotNull(retrievedRequest);
        assertEquals(request.getId(), retrievedRequest.getId());
        verify(requestRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when request not found by ID")
    void testGetRequestById_RequestNotFound_ThrowsException() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestService.getRequestById(1L);
        });

        assertEquals("Request not found", exception.getMessage());
    }
}
