package com.kuet.hub.service;

import com.kuet.hub.dto.ItemDto;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CreateItem_BlankTitle_Test {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    // FIX: ItemService now requires RequestRepository.
    // Without this mock, @InjectMocks cannot construct the service.
    @Mock private RequestRepository requestRepository;

    @InjectMocks private ItemService itemService;

    @Test
    void createItem_withBlankTitle_throwsException() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("provider");

        ItemDto dto = new ItemDto();
        dto.setTitle("   "); // blank title
        dto.setCondition(Item.Condition.USED);
        // Note: no categoryId — that's fine because the blank title check
        // now runs FIRST in ItemService.createItem(), before the category lookup.

        // FIX: Updated expected message to match what ItemService actually throws:
        // "Item title cannot be blank" (was "title cannot be blank" — substring match
        // still works with hasMessageContaining so the assertion is correct as-is)
        assertThatThrownBy(() -> itemService.createItem(dto, owner))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title cannot be blank");

        verify(itemRepository, never()).save(any());
        verify(categoryRepository, never()).findById(any()); // confirm category lookup never reached
    }
}