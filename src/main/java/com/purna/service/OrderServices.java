package com.purna.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.purna.dto.OrderResponseDTO;
import com.purna.model.Order;
import com.purna.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service Layer for Order management
 */
@Service
@RequiredArgsConstructor
public class OrderServices {

    private final OrderRepository orderRepository;

    /**
     * O(1) Fetch using Redis Caching.
     * Caches the specific order details indefinitely to avoid DB hits.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "order_details", key = "#orderId")
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
                
        return OrderResponseDTO.builder()
                .id(order.getId())
                .buyerId(order.getBuyer() != null ? Long.valueOf(order.getBuyer().getId()) : null)
                .productId(order.getListing() != null && order.getListing().getProduct() != null ? order.getListing().getProduct().getId() : null)
                .purchasePrice(order.getPurchasePrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
