package com.kuet.hub.service;

import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindById_NotFound_Test {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private ItemService itemService;

    @Test
    void findById_whenItemNotFound_throwsException() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Item not found");
    }
}
