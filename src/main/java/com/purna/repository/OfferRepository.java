package com.purna.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.purna.model.Offer;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    @EntityGraph(attributePaths = {"buyer", "seller", "product", "listing"})
    Page<Offer> findBySeller_Id(Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"buyer", "seller", "product", "listing"})
    Page<Offer> findByBuyer_Id(Long buyerId, Pageable pageable);
    
    boolean existsByBuyer_IdAndListing_IdAndStatusIn(Long buyerId, Long listingId, java.util.List<String> statuses);
}
