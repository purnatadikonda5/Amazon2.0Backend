package com.purna.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.purna.dto.OrderResponseDTO;
import com.purna.service.OrderServices;
import com.purna.exception.UnauthorizedBargainException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderServices orderServices;
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedBargainException("Unauthorized Access");
        }

        // Instantly increment order views/accesses in Redis for analytics
        redisTemplate.opsForValue().increment("order_access_count:" + orderId);

        // O(1) Redis Cached Database Fetch via Service Layer Component
        OrderResponseDTO order = orderServices.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/{orderId}/increment")
    public ResponseEntity<Long> instantlyIncrementOrderMetric(@PathVariable Long orderId) {
        // Fast Redis Increment capability for order updates and tracking (O(1))
        Long newCount = redisTemplate.opsForValue().increment("order_metric:" + orderId);
        return ResponseEntity.ok(newCount);
    }
}
