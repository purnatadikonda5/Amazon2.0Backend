package com.purna.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // product on which offer is made
    private Long productId;

    // buyer who made the offer
    private Long buyerId;

    // seller (stored for fast access)
    private Long sellerId;

    // buyer's offered price
    private Double offerPrice;

    // seller's counter offer (optional)
    private Double counterPrice;

    // PENDING, ACCEPTED, REJECTED, COUNTERED
    private String status;

    // auto timestamps
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // default constructor 
    public Offer() {}

    // constructor
    public Offer(Long productId, Long buyerId, Long sellerId, Double offerPrice, String status) {
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.offerPrice = offerPrice;
        this.status = status;
    }

    // getters and setters

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Double getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(Double offerPrice) {
        this.offerPrice = offerPrice;
    }

    public Double getCounterPrice() {
        return counterPrice;
    }

    public void setCounterPrice(Double counterPrice) {
        this.counterPrice = counterPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}