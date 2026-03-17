package com.kuet.hub.controller;

import com.kuet.hub.entity.Category;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.Role;
import com.kuet.hub.entity.User;
import com.kuet.hub.service.ItemService;
import com.kuet.hub.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BrowseController
 * Tests the full request-response cycle with Spring context
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BrowseController Integration Tests")
class BrowseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    private Item testItem;
    private User testProvider;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        itemRepository.deleteAll();

        // Create test provider user
        testProvider = new User();
        testProvider.setUsername("test_provider");
        testProvider.setEmail("provider@test.com");
        testProvider.setPassword("encoded_password");
        testProvider.setEnabled(true);

        // Create test category
        testCategory = new Category();
        testCategory.setName("Test Equipment");
        testCategory.setDescription("Test category for integration tests");

        // Create test item
        testItem = new Item();
        testItem.setTitle("Test Oscilloscope");
        testItem.setDescription("A test oscilloscope for electrical measurements");
        testItem.setCondition(Item.Condition.NEW);
        testItem.setStatus(Item.ItemStatus.AVAILABLE);
        testItem.setOwner(testProvider);
        testItem.setCategory(testCategory);
        testItem.setCreatedAt(LocalDateTime.now());

        // Save test item
        itemRepository.save(testItem);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when accessing /browse without authentication")
    void testBrowse_NotAuthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/browse"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 and load available items when browsing as borrower")
    void testBrowse_AsAuthenticatedBorrower_ReturnsAvailableItems() throws Exception {
        MvcResult result = mockMvc.perform(get("/browse")
                .with(request -> {
                    // Mock authentication as BORROWER
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("borrower/browse"))
                .andExpect(model().attributeExists("items"))
                .andReturn();

        // Verify that items list contains the test item
        @SuppressWarnings("unchecked")
        java.util.List<Item> items = (java.util.List<Item>) result.getModelAndView().getModel().get("items");
        assertThat(items).isNotEmpty();
        assertThat(items).anySatisfy(item -> 
            assertThat(item.getTitle()).isEqualTo("Test Oscilloscope")
        );
    }

    @Test
    @DisplayName("Should exclude unavailable items from browse results")
    void testBrowse_OnlyShowsAvailableItems() throws Exception {
        // Create an unavailable item
        Item unavailableItem = new Item();
        unavailableItem.setTitle("Unavailable Equipment");
        unavailableItem.setDescription("This item is not available");
        unavailableItem.setCondition(Item.Condition.USED);
        unavailableItem.setStatus(Item.ItemStatus.BORROWED);
        unavailableItem.setOwner(testProvider);
        unavailableItem.setCategory(testCategory);
        itemRepository.save(unavailableItem);

        MvcResult result = mockMvc.perform(get("/browse")
                .with(request -> {
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.List<Item> items = (java.util.List<Item>) result.getModelAndView().getModel().get("items");
        
        // Only available item should be in the list
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getStatus()).isEqualTo(Item.ItemStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should search items by keyword in title")
    void testSearch_WithKeyword_ReturnsMatchingItems() throws Exception {
        // Create another item with different title
        Item anotherItem = new Item();
        anotherItem.setTitle("Power Supply");
        anotherItem.setDescription("Electronic power supply");
        anotherItem.setCondition(Item.Condition.NEW);
        anotherItem.setStatus(Item.ItemStatus.AVAILABLE);
        anotherItem.setOwner(testProvider);
        anotherItem.setCategory(testCategory);
        itemRepository.save(anotherItem);

        MvcResult result = mockMvc.perform(get("/browse/search")
                .param("q", "Oscilloscope")
                .with(request -> {
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("borrower/browse"))
                .andExpect(model().attributeExists("items", "searchKeyword"))
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.List<Item> items = (java.util.List<Item>) result.getModelAndView().getModel().get("items");
        
        // Should only return items matching "Oscilloscope"
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getTitle()).contains("Oscilloscope");
    }

    @Test
    @DisplayName("Should return empty list when search keyword matches no items")
    void testSearch_NoMatches_ReturnsEmptyList() throws Exception {
        MvcResult result = mockMvc.perform(get("/browse/search")
                .param("q", "NonexistentItem")
                .with(request -> {
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.List<Item> items = (java.util.List<Item>) result.getModelAndView().getModel().get("items");
        
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Should handle search with empty keyword by returning all available items")
    void testSearch_EmptyKeyword_ReturnsAllAvailableItems() throws Exception {
        MvcResult result = mockMvc.perform(get("/browse/search")
                .param("q", "")
                .with(request -> {
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.List<Item> items = (java.util.List<Item>) result.getModelAndView().getModel().get("items");
        
        // Should return all available items
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("Should display item details when accessing specific item")
    void testViewItem_WithValidId_ReturnsItemDetails() throws Exception {
        mockMvc.perform(get("/browse/" + testItem.getId())
                .with(request -> {
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("borrower/item-detail"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", testItem));
    }

    @Test
    @DisplayName("Should return error view when item not found")
    void testViewItem_InvalidId_ReturnsErrorPage() throws Exception {
        mockMvc.perform(get("/browse/99999")
                .with(request -> {
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("error/generic"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Should include item count in browse model")
    void testBrowse_IncludesItemCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/browse")
                .with(request -> {
                    org.springframework.security.core.Authentication auth = 
                        new org.springframework.security.authentication.TestingAuthenticationToken(
                            "borrower_user", null, "ROLE_BORROWER");
                    request.setUserPrincipal(auth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("itemCount"))
                .andReturn();

        Long itemCount = (Long) result.getModelAndView().getModel().get("itemCount");
        assertThat(itemCount).isGreaterThan(0L);
    }
}
