package com.purna.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.purna.model.Listing;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    
    // Allows searching for listings active globally across the site
    Page<Listing> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    // Allows users to find all listings for a specific global Product
    Page<Listing> findByProduct_IdAndStatusAndIsDeletedFalse(Long productId, String status, Pageable pageable);

    // Allows grabbing all listings owned by a specific seller
    Page<Listing> findBySeller_IdAndIsDeletedFalse(Long sellerId, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT l FROM Listing l WHERE l.id = :id")
    java.util.Optional<Listing> findByIdWithPessimisticLock(@org.springframework.data.repository.query.Param("id") Long id);

}
