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
        // Validate blank title FIRST — before any repository call
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

    /**
     * Delete an item owned by the requesting user.
     *
     * Guard 1 — Ownership: only the item's owner can delete it.
     *
     * Guard 2 — Borrowed status (NEW):
     *   An item cannot be deleted while its status is BORROWED.
     *   Reason: a borrower currently holds the physical equipment and has an
     *   APPROVED request pointing to this item. Deleting the item would:
     *     - Orphan the Request record (item_id FK → deleted row)
     *     - Remove the borrower's ability to see what they're holding
     *     - Corrupt the provider's dashboard counts
     *     - Prevent the return flow (completeRequest updates item.status)
     *   The provider must wait until the borrower returns the equipment
     *   (request → COMPLETED, item → AVAILABLE) before deleting.
     *
     * Both checks run inside a single @Transactional method so no partial
     * state is possible if a check fails mid-way.
     */
    @Transactional
    public void deleteItem(Long itemId, User requestingUser) {
        Item item = getItemOwnedBy(itemId, requestingUser); // Guard 1: ownership

        // Guard 2: reject deletion if the item is currently BORROWED
        if (item.getStatus() == Item.ItemStatus.BORROWED) {
            log.warn("[ITEM] Deletion blocked — item {} is currently BORROWED", itemId);
            throw new IllegalStateException(
                    "Cannot delete item '" + item.getTitle() + "': it is currently borrowed. "
                    + "Wait for the borrower to return the equipment before deleting.");
        }

        // Safe to delete — remove linked requests first to avoid FK violation
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