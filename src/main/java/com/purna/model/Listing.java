package com.purna.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Listing Database Entity
 *
 * WHY USE THIS:
 * Separates the abstract "Product" (e.g., iPhone 14) from the specific seller's "Listing" 
 * (e.g., iPhone 14 sold by John for $500, quantity 10). 
 * 
 * WHY USE FetchType.LAZY:
 * The @ManyToOne constraints are loaded lazily. If we pull 1,000 listings to display on the 
 * homepage, we don't accidentally execute 1,000 extra SQL queries fetching the full Seller and Product 
 * objects unless explicitly requested, avoiding the infamous N+1 Query Performance bottleneck.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name= "amazon_listings", indexes = {
    @Index(name = "idx_listing_seller", columnList = "sellerId"),
    @Index(name = "idx_listing_product", columnList = "productId"),
    @Index(name = "idx_listing_status", columnList = "status")
})
@SQLDelete(sql = "UPDATE amazon_listings SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted=false")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", referencedColumnName = "id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sellerId", referencedColumnName = "id")
    private UserObj seller;

    private Double price;

    private Double minAcceptablePrice;

    private Integer quantity;

    private String status; // active, sold_out, inactive

    @org.hibernate.annotations.CreationTimestamp
    private java.time.LocalDateTime createdAt;

    private Integer yearsOld;
    
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String customImageUrls;

    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * ⚡ ADVANCED PRODUCTION: Optimistic Locking
     * WHY USE THIS:
     * High-concurrency environments experience Race Conditions. If Buyer A and Buyer B both buy 
     * the exact last remaining iPhone quantity=1 at the exact same millisecond, raw SQL might let 
     * both reads pass, crashing quantity down to -1! 
     * @Version explicitly blocks the 2nd transaction immediately at the MySQL engine level by 
     * asserting the Version Hash still matches what was read.
     */
    @jakarta.persistence.Version
    private Long version;
}
