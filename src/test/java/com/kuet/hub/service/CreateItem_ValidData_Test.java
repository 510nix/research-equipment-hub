package com.kuet.hub.service;

import com.kuet.hub.dto.ItemDto;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CreateItem_ValidData_Test {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private ItemService itemService;

    @Test
    void createItem_withValidData_savesItemWithAvailableStatus() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("provider");

        ItemDto dto = new ItemDto();
        dto.setTitle("Oscilloscope");
        dto.setDescription("4-channel, 200MHz");
        dto.setCondition(Item.Condition.USED);

        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        Item result = itemService.createItem(dto, owner);

        assertThat(result.getTitle()).isEqualTo("Oscilloscope");
        assertThat(result.getStatus()).isEqualTo(Item.ItemStatus.AVAILABLE);
        assertThat(result.getOwner()).isEqualTo(owner);
        verify(itemRepository).save(any(Item.class));
    }
}
