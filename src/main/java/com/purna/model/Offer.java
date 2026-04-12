package com.purna.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "offers", indexes = {
    @Index(name = "idx_offer_buyer", columnList = "buyerId"),
    @Index(name = "idx_offer_seller", columnList = "sellerId"),
    @Index(name = "idx_offer_product", columnList = "productId"),
    @Index(name = "idx_offer_listing", columnList = "listingId"),
    @Index(name = "idx_offer_status", columnList = "status")
})
@SQLDelete(sql = "UPDATE offers SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted=false")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Global catalog lookup
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", referencedColumnName = "id")
    private Product product;

    // Seller's specific listing holding pricing & inventory
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listingId", referencedColumnName = "id")
    private Listing listing;

    // buyer who made the offer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyerId", referencedColumnName = "id")
    private UserObj buyer;

    // seller (stored for fast access)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sellerId", referencedColumnName = "id")
    private UserObj seller;

    // buyer's offered price
    private Double offerPrice;

    // seller's counter offer (optional)
    private Double counterPrice;

    // quantity of products buyer wants to purchase
    private Integer quantity;

    // PENDING, ACCEPTED, REJECTED, COUNTERED
    private String status;

    @Builder.Default
    private Boolean isDeleted = false;

    // auto timestamps
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}