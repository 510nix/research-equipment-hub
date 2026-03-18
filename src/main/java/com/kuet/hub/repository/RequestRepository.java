package com.kuet.hub.repository;

import com.kuet.hub.entity.Item;
import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    /**
     * Fetch all requests by a borrower, with item/category/owner eagerly loaded.
     * Used by: RequestController.myRequests() → borrower/my-requests.html
     */
    @Query("""
            SELECT r FROM Request r
            LEFT JOIN FETCH r.item i
            LEFT JOIN FETCH i.category
            LEFT JOIN FETCH i.owner
            LEFT JOIN FETCH r.borrower
            WHERE r.borrower = :borrower
            ORDER BY r.createdAt DESC
            """)
    List<Request> findByBorrower(@Param("borrower") User borrower);

    /**
     * Fetch all requests for a provider's items, with all associations loaded.
     * Used by: RequestController.providerRequests() → provider/incoming-requests.html
     */
    @Query("""
            SELECT r FROM Request r
            LEFT JOIN FETCH r.item i
            LEFT JOIN FETCH i.category
            LEFT JOIN FETCH i.owner
            LEFT JOIN FETCH r.borrower
            WHERE i.owner = :owner
            ORDER BY r.createdAt DESC
            """)
    List<Request> findByItemOwner(@Param("owner") User owner);

    /**
     * Find all requests by a specific borrower filtered by status.
     *
     * Used in UserService.toggleUserEnabled() to:
     * - Find PENDING requests to cancel when a borrower is deactivated
     * - Find APPROVED requests to complete and free items when a borrower is deactivated
     */
    List<Request> findByBorrowerAndStatus(User borrower, Request.RequestStatus status);

    /**
     * Find all PENDING requests for a specific item, excluding one request by ID.
     *
     * Used in RequestService.approveRequest() for double-booking prevention:
     * when request X is approved, all other PENDING requests for the same item
     * (id != approvedRequestId) are automatically rejected.
     */
    @Query("""
            SELECT r FROM Request r
            WHERE r.item = :item
            AND r.status = :status
            AND r.id <> :excludeId
            """)
    List<Request> findByItemAndStatusAndIdNot(
            @Param("item") Item item,
            @Param("status") Request.RequestStatus status,
            @Param("excludeId") Long excludeId);

    /**
     * Find all requests linked to a specific item.
     * Used in ItemService.deleteItem() before deleting the item
     * to prevent FK constraint violation.
     */
    List<Request> findByItem(Item item);

    /**
     * Bulk-delete all requests for a given item.
     * Used in ItemService.deleteItem().
     */
    @Modifying
    @Query("DELETE FROM Request r WHERE r.item = :item")
    void deleteByItem(@Param("item") Item item);
}