package com.purna.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.purna.dto.OrderResponseDTO;
import com.purna.dto.ProductResponseDTO;
import com.purna.dto.ListingResponseDTO;
import com.purna.model.Order;
import com.purna.model.Product;
import com.purna.model.Listing;
import com.purna.repository.OrderRepository;
import com.purna.repository.ListingRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service Layer responsible for returning user specific datasets.
 * Includes paginated searches for historical orders and current active listings.
 */
@Service
@RequiredArgsConstructor
public class UserServices {

    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;

    /**
     * Look up the comprehensive history of all completed purchases the buyer made.
     */
    @Transactional(readOnly = true)
    // @org.springframework.cache.annotation.Cacheable(value = "user_orders", key = "#userId + '-' + #pageable.pageNumber")
    public Page<OrderResponseDTO> getCompletedOrders(Long userId, Pageable pageable) {
        return orderRepository.findByBuyer_Id(userId, pageable).map(this::mapToOrderResponseDTO);
    }

    /**
     * Look up every listing this specific seller currently has in the market.
     */
    @Transactional(readOnly = true)
    // @org.springframework.cache.annotation.Cacheable(value = "user_listings", key = "#userId + '-' + #pageable.pageNumber")
    public Page<ListingResponseDTO> getListedItems(Long userId, Pageable pageable) {
        Page<ListingResponseDTO> response = listingRepository.findBySeller_IdAndIsDeletedFalse(userId, pageable).map(this::mapToListingResponseDTO);
        return response;
    }

    private OrderResponseDTO mapToOrderResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .buyerId(order.getBuyer() != null ? Long.valueOf(order.getBuyer().getId()) : null)
                .productId(order.getListing() != null && order.getListing().getProduct() != null ? order.getListing().getProduct().getId() : null)
                .purchasePrice(order.getPurchasePrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private ProductResponseDTO mapToProductResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .build();
    }

    private ListingResponseDTO mapToListingResponseDTO(Listing listing) {
        return ListingResponseDTO.builder()
                .id(listing.getId())
                .productId(listing.getProduct() != null ? listing.getProduct().getId() : null)
                .sellerId(listing.getSeller() != null ? Long.valueOf(listing.getSeller().getId()) : null)
                .product(listing.getProduct() != null ? mapToProductResponseDTO(listing.getProduct()) : null)
                .price(listing.getPrice())
                .minAcceptablePrice(listing.getMinAcceptablePrice())
                .quantity(listing.getQuantity())
                .status(listing.getStatus())
                .build();
    }
}
