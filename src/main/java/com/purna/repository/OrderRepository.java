package com.purna.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.purna.model.Order;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"buyer", "listing.seller", "listing.product"})
    Page<Order> findByBuyer_Id(Long buyer_Id, Pageable pageable);
}
