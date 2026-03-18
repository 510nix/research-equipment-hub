package com.kuet.hub.repository;

import com.kuet.hub.entity.Category;
import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category LEFT JOIN FETCH i.owner WHERE i.owner = :owner")
    List<Item> findByOwnerWithCategory(@Param("owner") User owner);

    /**
     * FIX: Browse query now filters by BOTH item status AND owner.enabled = true.
     *
     * When a provider is deactivated, their items must disappear from the borrower's
     * Browse page and the admin's active item list immediately.
     * Rationale: a deactivated provider cannot approve requests, so showing their
     * items would let borrowers submit requests that can never be fulfilled.
     *
     * Items remain in the database — they reappear automatically when the provider
     * is re-activated, because this query will then include them again.
     */
    @Query("""
            SELECT i FROM Item i
            LEFT JOIN FETCH i.category
            LEFT JOIN FETCH i.owner
            WHERE i.status = :status
            AND i.owner.enabled = true
            """)
    List<Item> findByStatusWithCategory(@Param("status") Item.ItemStatus status);

    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category LEFT JOIN FETCH i.owner WHERE i.id = :id")
    Optional<Item> findByIdWithCategory(@Param("id") Long id);

    @Query("""
            SELECT i FROM Item i
            LEFT JOIN FETCH i.category
            LEFT JOIN FETCH i.owner
            WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND i.owner.enabled = true
            """)
    List<Item> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    /**
     * Fetch ALL items with owner and category eagerly loaded.
     * Used by AdminController — returns items from ALL providers including
     * deactivated ones, so the admin has full system visibility.
     * The template marks deactivated-provider items with a visual indicator.
     */
    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category LEFT JOIN FETCH i.owner ORDER BY i.createdAt DESC")
    List<Item> findAllWithOwnerAndCategory();

    /**
     * Check if any item in a category is currently BORROWED.
     * Used by AdminController.deleteCategory() to enforce the Dependency Lock:
     * a category cannot be deleted while any of its items are actively borrowed.
     */
    @Query("SELECT COUNT(i) FROM Item i WHERE i.category = :category AND i.status = 'BORROWED'")
    long countBorrowedByCategory(@Param("category") Category category);

    long countByCategory(Category category);
}