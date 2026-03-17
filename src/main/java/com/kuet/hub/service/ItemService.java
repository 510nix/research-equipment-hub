package com.kuet.hub.service;

//package com.kuet.researchequipmenthub.service;

import com.kuet.hub.dto.ItemDto;
import com.kuet.hub.entity.*;
import com.kuet.hub.repository.CategoryRepository;
import com.kuet.hub.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public Item createItem(ItemDto dto, User owner) {
        if (dto.getTitle() == null || dto.getTitle().isBlank())
            throw new IllegalArgumentException("Item title cannot be blank");

        Item item = new Item();
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setCondition(dto.getCondition());
        item.setOwner(owner);
        item.setStatus(Item.ItemStatus.AVAILABLE);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            item.setCategory(category);
        }
        return itemRepository.save(item);
    }

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

    public void deleteItem(Long itemId, User requestingUser) {
        Item item = getItemOwnedBy(itemId, requestingUser);
        itemRepository.delete(item);
    }

    public List<Item> getItemsByOwner(User owner) {
        return itemRepository.findByOwner(owner);
    }

    public List<Item> getAvailableItems() {
        return itemRepository.findByStatus(Item.ItemStatus.AVAILABLE);
    }

    public Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }

    public List<Item> searchByTitle(String keyword) {
        return itemRepository.findByTitleContainingIgnoreCase(keyword);
    }

    private Item getItemOwnedBy(Long itemId, User user) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (!item.getOwner().getId().equals(user.getId()))
            throw new SecurityException("You do not own this item");
        return item;
    }
}