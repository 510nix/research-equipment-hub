package com.kuet.hub.service;

import com.kuet.hub.dto.ItemDto;
import com.kuet.hub.entity.*;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import com.kuet.hub.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    @Transactional
    public Item createItem(ItemDto dto, User owner) {
        // FIX: Validate blank title FIRST — before any repository call.
        // Previously the category lookup ran first, so a blank title with no
        // categoryId would throw "Invalid Category ID" instead of the expected
        // "title cannot be blank", causing CreateItem_BlankTitle_Test to fail.
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Item title cannot be blank");
        }

        Item item = new Item();
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setCondition(dto.getCondition());
        item.setOwner(owner);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Category ID"));
        item.setCategory(category);

        item.setStatus(Item.ItemStatus.AVAILABLE);
        item.setCreatedAt(java.time.LocalDateTime.now());

        return itemRepository.save(item);
    }

    @Transactional
    public Item updateItem(Long itemId, ItemDto dto, User requestingUser) {
        Item item = getItemOwnedBy(itemId, requestingUser);
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setCondition(dto.getCondition());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            item.setCategory(category);
        }
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long itemId, User requestingUser) {
        Item item = getItemOwnedBy(itemId, requestingUser);
        requestRepository.deleteByItem(item);
        itemRepository.delete(item);
        log.info("[ITEM] Item {} deleted successfully", itemId);
    }

    @Transactional(readOnly = true)
    public List<Item> getItemsByOwner(User owner) {
        return itemRepository.findByOwnerWithCategory(owner);
    }

    @Transactional(readOnly = true)
    public List<Item> getAvailableItems() {
        return itemRepository.findByStatusWithCategory(Item.ItemStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public Item findById(Long id) {
        return itemRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Item> searchByTitle(String keyword) {
        return itemRepository.findByTitleContainingIgnoreCase(keyword);
    }

    private Item getItemOwnedBy(Long itemId, User user) {
        Item item = itemRepository.findByIdWithCategory(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (!item.getOwner().getId().equals(user.getId()))
            throw new SecurityException("You do not own this item");
        return item;
    }
}