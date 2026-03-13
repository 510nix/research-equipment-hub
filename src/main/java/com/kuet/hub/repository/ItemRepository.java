package com.kuet.hub.repository;

import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByOwner(User owner);
    List<Item> findByStatus(Item.ItemStatus status);
    List<Item> findByTitleContainingIgnoreCase(String keyword);
}