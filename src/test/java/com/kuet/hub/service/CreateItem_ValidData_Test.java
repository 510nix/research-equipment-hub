package com.kuet.hub.service;

import com.kuet.hub.dto.ItemDto;
import com.kuet.hub.entity.Category;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CreateItem_ValidData_Test {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    // FIX: ItemService now requires RequestRepository (injected via @RequiredArgsConstructor).
    // Without this mock, @InjectMocks cannot construct the service → NPE or missing field.
    @Mock private RequestRepository requestRepository;

    @InjectMocks private ItemService itemService;

    @Test
    void createItem_withValidData_savesItemWithAvailableStatus() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("provider");

        // FIX: Set a valid categoryId so the category lookup doesn't throw
        // "Invalid Category ID" before we even reach the save call.
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        ItemDto dto = new ItemDto();
        dto.setTitle("Oscilloscope");
        dto.setDescription("4-channel, 200MHz");
        dto.setCondition(Item.Condition.USED);
        dto.setCategoryId(1L); // FIX: was missing — caused "Invalid Category ID" error

        // FIX: mock the category lookup that ItemService.createItem() calls
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        Item result = itemService.createItem(dto, owner);

        assertThat(result.getTitle()).isEqualTo("Oscilloscope");
        assertThat(result.getStatus()).isEqualTo(Item.ItemStatus.AVAILABLE);
        assertThat(result.getOwner()).isEqualTo(owner);
        assertThat(result.getCategory()).isEqualTo(category);
        verify(itemRepository).save(any(Item.class));
    }
}