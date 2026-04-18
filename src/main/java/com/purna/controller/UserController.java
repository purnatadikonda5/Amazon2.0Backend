package com.purna.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.purna.dto.OrderResponseDTO;
import com.purna.dto.ListingResponseDTO;
import com.purna.exception.UnauthorizedBargainException;
import com.purna.model.UserObj;
import com.purna.repository.UserRepository;
import com.purna.service.UserServices;

import lombok.RequiredArgsConstructor;

/**
 * Exposed endpoints for resolving User specific internal metrics.
 * Such as retrieving historical purchases and monitoring active store listings.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserServices userServices;
    private final UserRepository userRepository;

    /**
     * Resolves the active user ID from the currently logged in Security Context.
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedBargainException("Unauthorized Access: No active user session.");
        }
        
        String email = authentication.getName();
        UserObj user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UnauthorizedBargainException("User token is valid, but account doesn't exist.");
        }
        
        return Long.valueOf(user.getId());
    }

    /**
     * Returns previous completed orders/purchases for the user.
     * 
     * GET /api/user/orders
     */
    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponseDTO>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<OrderResponseDTO> response = userServices.getCompletedOrders(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns items the user specifically listed for sale.
     * 
     * GET /api/user/selling
     */
    @GetMapping("/selling")
    public ResponseEntity<Page<ListingResponseDTO>> getUserSellingListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ListingResponseDTO> response = userServices.getListedItems(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve precise account platform wallet escrow metrics securely
     */
    @GetMapping("/wallet")
    public ResponseEntity<java.util.Map<String, Double>> getWalletBalance() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new UnauthorizedBargainException("Unauthorized");
        UserObj user = userRepository.findByEmail(authentication.getName());
        return ResponseEntity.ok(java.util.Map.of("balance", user.getBalance() != null ? user.getBalance() : 0.0));
    }

    /**
     * Simulate Platform Escrow Withdrawal straight to Razorpay verified Accounts
     */
    @PostMapping("/wallet/withdraw")
    public ResponseEntity<java.util.Map<String, String>> withdrawWallet() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserObj user = userRepository.findByEmail(authentication.getName());
        
        Double currentBalance = user.getBalance() != null ? user.getBalance() : 0.0;
        if (currentBalance <= 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Wallet is empty."));
        }
        
        // Simulates Razorpay Payouts directly hitting clearinghouses!
        user.setBalance(0.0);
        userRepository.save(user);
        return ResponseEntity.ok(java.util.Map.of("message", "Successfully originated ACH transfer of ₹" + currentBalance + " to your linked account."));
    }
}
