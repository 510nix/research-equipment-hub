package com.kuet.hub.service;

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
class FindById_NotFound_Test {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    // FIX: ItemService requires RequestRepository — must be present for @InjectMocks to work.
    @Mock private RequestRepository requestRepository;

    @InjectMocks private ItemService itemService;

    @Test
    void findById_whenItemNotFound_throwsException() {
        // FIX: ItemService.findById() calls itemRepository.findByIdWithCategory(),
        // NOT itemRepository.findById(). The original test stubbed findById(999L)
        // which Mockito correctly flagged as UnnecessaryStubbingException because
        // that method was never actually invoked by the code under test.
        when(itemRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Item not found");
    }
}