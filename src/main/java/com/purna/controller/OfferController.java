//comments for push into main branch
package com.purna.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.purna.dto.OfferResponseDTO;
import com.purna.dto.OfferSubmitRequestDTO;
import com.purna.dto.OfferUpdateRequestDTO;
import com.purna.dto.OfferActionResponseDTO;
import com.purna.dto.OfferListResponseDTO;
import com.purna.exception.UnauthorizedBargainException;
import com.purna.model.UserObj;
import com.purna.repository.UserRepository;
import com.purna.service.OfferServices;

import lombok.RequiredArgsConstructor;

/**
 * OfferController (Bargaining Sub-System)
 *
 * WHY USE THIS:
 * Manages the "Make an Offer" business logic between buyers and sellers. 
 * Notice that it strictly extracts IDs via `getAuthenticatedUserId()` server-side rather than trusting the 
 * frontend UI to pass `{ "buyerId": 5 }`. This prevents "BOLA" (Broken Object Level Authorization) attacks 
 * where Hacker A could maliciously submit an offer on behalf of User B.
 */
@RestController
@RequestMapping("/api/bargain")
@RequiredArgsConstructor
public class OfferController {

    private final OfferServices offerServices;
    private final UserRepository userRepository;

    /**
     * Resolves the active user ID from the currently logged in Security Context.
     * Prevents clients from spoofing IDs by trusting only the server-decrypted JWT
     * credentials.
     * 
     * @return Long database ID of the currently logged-in user.
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Ensure standard authorization state
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedBargainException("Unauthorized Access: No active user session.");
        }

        // Cross-check JWT Subject with Database
        String email = authentication.getName();
        UserObj user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UnauthorizedBargainException("User token is valid, but account doesn't exist.");
        }

        // Safely extract the ID explicitly cast to Long matching our other Models
        return Long.valueOf(user.getId());
    }

    @PostMapping("/submit")
    public ResponseEntity<OfferActionResponseDTO> submitOffer(@Valid @RequestBody OfferSubmitRequestDTO request) {
        Long buyerId = getAuthenticatedUserId();
        OfferResponseDTO response = offerServices.submitOffer(buyerId, request);
        return ResponseEntity.ok(new OfferActionResponseDTO("Offer submitted successfully", response));
    }

    @GetMapping("/seller")
    public ResponseEntity<OfferListResponseDTO> getOffersForSeller(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long sellerId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<OfferResponseDTO> responsePage = offerServices.getOffersForSeller(sellerId, pageable);
        return ResponseEntity.ok(new OfferListResponseDTO(responsePage.getContent()));
    }

    @GetMapping("/buyer")
    public ResponseEntity<OfferListResponseDTO> getOffersForBuyer(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long buyerId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<OfferResponseDTO> responsePage = offerServices.getOffersForBuyer(buyerId, pageable);
        return ResponseEntity.ok(new OfferListResponseDTO(responsePage.getContent()));
    }

    @PostMapping("/update")
    public ResponseEntity<OfferActionResponseDTO> updateOffer(@Valid @RequestBody OfferUpdateRequestDTO request) {
        Long sellerId = getAuthenticatedUserId();
        OfferResponseDTO response = offerServices.updateOffer(sellerId, request);
        return ResponseEntity.ok(new OfferActionResponseDTO("Offer updated by seller", response));
    }

    @PostMapping("/buyer/update")
    public ResponseEntity<OfferActionResponseDTO> buyerUpdateOffer(@Valid @RequestBody OfferUpdateRequestDTO request) {
        Long buyerId = getAuthenticatedUserId();
        OfferResponseDTO response = offerServices.buyerUpdateOffer(buyerId, request);
        return ResponseEntity.ok(new OfferActionResponseDTO("Buyer response recorded", response));
    }
}
