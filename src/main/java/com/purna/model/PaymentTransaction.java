package com.purna.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * PaymentTransaction Entity
 * 
 * WHY USE THIS:
 * We use a dedicated entity for transactions to adhere to the Durability property of DBMS ACID.
 * If the application crashes midway through a payment, the MySQL Write-Ahead-Log maintains our state here.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    // WHY USE INDEXES: Database indexes create a B-Tree structure allowing O(log n) lookups. 
    // This strictly prevents the database from performing slow Full Table Scans when matching idempotency keys.
    @Index(name = "idx_payment_idempotency", columnList = "idempotencyKey"),
    @Index(name = "idx_payment_razorpay", columnList = "razorpayOrderId"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_buyer", columnList = "buyerId"),
    @Index(name = "idx_payment_listing", columnList = "listingId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * WHY USE UNIQUE IDEMPOTENCY KEY:
     * This physically blocks the database from accepting duplicate network traffic (e.g., if a user 
     * furiously spams the "Buy" button during a lag spike, the DB throws a natural constraint violation).
     */
    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @Column(unique = true)
    private String razorpayOrderId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status; // CREATED, FAILED, SUCCESS

    /**
     * WHY USE @ManyToOne FOREIGN KEYS:
     * Using strict relational objects (instead of raw IDs) enforces Referential Integrity.
     * The DB will error if someone tries to attach a payment to a Listing or Buyer that doesn't exist.
     * FetchType.LAZY ensures memory isn't wasted heavily loading the linked table unprompted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listingId", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyerId", nullable = false)
    private UserObj buyer;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = "CREATED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
