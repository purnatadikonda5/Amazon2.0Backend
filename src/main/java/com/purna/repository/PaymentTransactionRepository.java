package com.purna.repository;

import com.purna.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * PaymentTransactionRepository
 * 
 * WHY USE THIS:
 * Abstraction layer directly querying the PaymentTransaction entity. We separate queries here 
 * to adhere to the Single Responsibility Principle.
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * WHY USE @Lock(LockModeType.PESSIMISTIC_WRITE):
     * This is the strongest DBMS concurrency control. It executes 'SELECT ... FOR UPDATE' in SQL MySQL.
     * If webhook A and webhook B hit this method simultaneously for the exact same order, the DB 
     * engine acts as a traffic light and completely pauses Thread B until Thread A finishes its 
     * transactional save! This categorically prevents Race Conditions and Double-Fulfillment!
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentTransaction p WHERE p.razorpayOrderId = :orderId")
    Optional<PaymentTransaction> findByRazorpayOrderIdWithPessimisticLock(@Param("orderId") String orderId);

    // Standard read for non-critical dashboard rendering
    Optional<PaymentTransaction> findByRazorpayOrderId(String orderId);

    // Determines if we should block duplicate requests instantly
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
