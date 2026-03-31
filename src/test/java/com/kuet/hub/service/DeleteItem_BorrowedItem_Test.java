package com.kuet.hub.service;



import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests the Dependency Lock guard in ItemService.deleteItem():
 * a provider cannot delete an item while it is currently BORROWED.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteItem - Borrowed Item Guard Tests")
@SuppressWarnings("null")
class DeleteItem_BorrowedItem_Test {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private RequestRepository requestRepository;

    @InjectMocks private ItemService itemService;

    @Test
    @DisplayName("deleteItem - throws IllegalStateException when item is BORROWED")
    void deleteItem_whenItemIsBorrowed_throwsIllegalStateException() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("provider");

        Item borrowedItem = new Item();
        borrowedItem.setId(5L);
        borrowedItem.setTitle("Oscilloscope");
        borrowedItem.setOwner(owner);
        borrowedItem.setStatus(Item.ItemStatus.BORROWED); // currently borrowed

        when(itemRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(borrowedItem));

        assertThatThrownBy(() -> itemService.deleteItem(5L, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently borrowed");

        // Confirm neither requests nor the item were touched
        verify(requestRepository, never()).deleteByItem(any());
        verify(itemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteItem - succeeds when item is AVAILABLE (not borrowed)")
    void deleteItem_whenItemIsAvailable_deletesSuccessfully() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("provider");

        Item availableItem = new Item();
        availableItem.setId(6L);
        availableItem.setTitle("Microscope");
        availableItem.setOwner(owner);
        availableItem.setStatus(Item.ItemStatus.AVAILABLE);

        when(itemRepository.findByIdWithCategory(6L)).thenReturn(Optional.of(availableItem));

        itemService.deleteItem(6L, owner);

        // Requests deleted first, then item deleted
        verify(requestRepository).deleteByItem(availableItem);
        verify(itemRepository).delete(availableItem);
    }

    @Test
    @DisplayName("deleteItem - throws IllegalStateException when item is UNAVAILABLE and being tracked")
    void deleteItem_whenItemIsUnavailable_throwsIllegalStateException() {
        // UNAVAILABLE items may have pending arrangements — block deletion
        // to be safe. Provider should set back to AVAILABLE first.
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("provider");

        Item unavailableItem = new Item();
        unavailableItem.setId(7L);
        unavailableItem.setTitle("Power Supply");
        unavailableItem.setOwner(owner);
        unavailableItem.setStatus(Item.ItemStatus.BORROWED);

        when(itemRepository.findByIdWithCategory(7L)).thenReturn(Optional.of(unavailableItem));

        assertThatThrownBy(() -> itemService.deleteItem(7L, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently borrowed");

        verify(requestRepository, never()).deleteByItem(any());
        verify(itemRepository, never()).delete(any());
    }
}
