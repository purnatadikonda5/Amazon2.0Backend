package com.purna.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.purna.model.Product;
import com.purna.model.Listing;
import com.purna.model.UserObj;
import com.purna.dto.ListingResponseDTO;
import com.purna.repository.ProductsRepository;
import com.purna.repository.UserRepository;
import com.purna.service.ProductServices;
import com.purna.exception.ResourceNotFoundException;
import com.purna.exception.UnauthorizedBargainException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductServices service;
    private final ProductsRepository repository;
    private final UserRepository userRepository;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new UnauthorizedBargainException("Unauthorized Access: No active user session. Please Provide your JWT Bearer token!");
        }
        
        String email = authentication.getName();
        UserObj user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UnauthorizedBargainException("User token is valid, but account doesn't exist.");
        }
        
        return Long.valueOf(user.getId());
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<com.purna.dto.ListingResponseDTO>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(service.getAvailableProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.purna.dto.ListingResponseDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getListingById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<org.springframework.data.domain.Page<com.purna.dto.ListingResponseDTO>> searchProducts(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(service.getFuzzySearchListings(keyword, pageable));
    }

    @PostMapping
    public ResponseEntity<ListingResponseDTO> addProduct(@RequestBody @Valid com.purna.dto.ProductRequestDTO request) {
        Long sellerId = getAuthenticatedUserId();
        
        Listing saved = service.addProductWithListing(request, sellerId);
        ListingResponseDTO response = ListingResponseDTO.builder()
                .id(saved.getId())
                .productId(saved.getProduct().getId())
                .sellerId(Long.valueOf(saved.getSeller().getId()))
                .price(saved.getPrice())
                .minAcceptablePrice(saved.getMinAcceptablePrice())
                .quantity(saved.getQuantity())
                .status(saved.getStatus())
                .product(service.mapToProductResponseDTO(saved.getProduct()))
                .build();
                
        return ResponseEntity.status(201).body(response);
    }
}