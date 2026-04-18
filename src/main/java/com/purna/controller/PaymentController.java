package com.purna.controller;

import com.purna.dto.PaymentRequestDTO;
import com.purna.dto.PaymentResponseDTO;
import com.purna.dto.PaymentStatusResponseDTO;
import com.purna.dto.PaymentVerificationRequestDTO;
import com.purna.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.purna.model.UserObj;
import com.purna.repository.UserRepository;
import com.purna.exception.UnauthorizedBargainException;

/**
 * PaymentController
 * 
 * WHY USE THIS:
 * The controller exclusively handles routing the HTTP network requests mapping payloads 
 * securely into DTO objects (which strip out malicious data via @Valid). It separates the 
 * Web Layer completely away from the Business Logic Layer.
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
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
     * WHY USE THIS ENDPOINT:
     * Called when the buyer first hits "Pay". We don't process the money here; we generate 
     * a secure encrypted Gateway Session holding an idempotency key to stop double-hits!
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDTO> createPayment(@Valid @RequestBody PaymentRequestDTO request) {
        // Securely override any frontend tampered Buyer ID with the actual cryptographically signed token ID!
        request.setBuyerId(getAuthenticatedUserId());
        
        // Guaranteed constant-time protection against redundant multi-clicks via Database ACID Isolation Limits
        PaymentResponseDTO response = paymentService.createPaymentSession(request);
        return ResponseEntity.ok(response);
    }

    /**
     * WHY USE THIS ENDPOINT:
     * A light-weight endpoint just for rendering "Is Payment Pending or Success?" on the Frontend safely.
     */
    @GetMapping("/status")
    public ResponseEntity<PaymentStatusResponseDTO> checkStatus(@RequestParam("order_id") String orderId) {
        PaymentStatusResponseDTO response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * WHY USE THIS ENDPOINT (CRITICAL STEP):
     * Razorpay Payment Verification (Webhook Reciever). 
     * The Frontend triggers this after Razorpay completes successfully. This endpoint seals the 
     * deal with atomic integrity (Creates the marketplace Order, deducts inventory, credits seller).
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentStatusResponseDTO> verifyPayment(@Valid @RequestBody PaymentVerificationRequestDTO verificationDto) {
        PaymentStatusResponseDTO response = paymentService.verifyAndUpdatePayment(verificationDto);
        return ResponseEntity.ok(response);
    }
}
