package com.purna.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.purna.model.Offer;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

}
