package com.kuet.hub.service;

import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DeleteItem_NonOwner_Test {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    // FIX: ItemService now requires RequestRepository (deleteByItem is called inside deleteItem).
    // Without this mock, @InjectMocks cannot wire the service and calls will NPE.
    @Mock private RequestRepository requestRepository;

    @InjectMocks private ItemService itemService;

    @Test
    void deleteItem_byNonOwner_throwsSecurityException() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("provider");

        User intruder = new User();
        intruder.setId(99L);
        intruder.setUsername("intruder");

        Item item = new Item();
        item.setId(10L);
        item.setTitle("Microscope");
        item.setOwner(owner);

        // FIX: ItemService.deleteItem() calls getItemOwnedBy() which calls
        // itemRepository.findByIdWithCategory() — NOT findById().
        // The original test stubbed findById(10L) which was never called,
        // so findByIdWithCategory returned empty → "Item not found: 10"
        // instead of reaching the ownership check → wrong exception type.
        when(itemRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.deleteItem(10L, intruder))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("do not own");

        verify(itemRepository, never()).delete(any());
    }
}