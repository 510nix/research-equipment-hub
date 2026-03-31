package com.kuet.hub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
public class Request {

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED, COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The date the borrower plans to start using the equipment.
     */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * The planned return date for the equipment.
     */
    @Column(nullable = false)
    private LocalDate endDate;

    /**
     * Optional note from the borrower to the provider.
     */
    @Column(length = 500)
    private String message;

    /**
     * Status of the request: PENDING, APPROVED, REJECTED, or COMPLETED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    /**
     * Timestamp when the request was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * The borrower (student) making the request.
     * This is the "Many" side: one user can make many requests.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    /**
     * The item (research equipment) being requested.
     * This is the "Many" side: one item can have many requests.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
}
