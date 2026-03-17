package com.kuet.hub.repository;

import com.kuet.hub.entity.Request;
import com.kuet.hub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository for managing Request entities.
 * Provides access to request data with specialized query methods for borrowers and providers.
 */
public interface RequestRepository extends JpaRepository<Request, Long> {

    /**
     * Find all requests made by a specific borrower (student).
     * Allows students to view their borrowing history.
     *
     * @param borrower the User making the requests
     * @return a list of requests made by the borrower
     */
    List<Request> findByBorrower(User borrower);

    /**
     * Find all requests for items owned by a specific provider.
     * Allows providers to see all requests for their equipment.
     *
     * @param owner the User who owns the items
     * @return a list of requests for the provider's equipment
     */
    List<Request> findByItemOwner(User owner);
}
