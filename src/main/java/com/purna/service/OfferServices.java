package com.purna.service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.purna.dto.OfferResponseDTO;
import com.purna.dto.OfferSubmitRequestDTO;
import com.purna.dto.OfferUpdateRequestDTO;
import com.purna.exception.InvalidOfferException;
import com.purna.exception.ResourceNotFoundException;
import com.purna.exception.UnauthorizedBargainException;
import com.purna.model.Offer;
import com.purna.model.Product;
import com.purna.model.UserObj;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.purna.model.Listing;
import com.purna.model.Order;
import com.purna.repository.ListingRepository;
import com.purna.repository.OfferRepository;
import com.purna.repository.OrderRepository;
import com.purna.repository.ProductsRepository;
import com.purna.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service layer responsible for the core logic of the Bargaining system.
 * Handles the submission, retrieval, and negotiation updates of offers.
 */
@Service
@RequiredArgsConstructor
public class OfferServices {
    private final OfferRepository offerRepository;
    private final ListingRepository listingRepository;
    private final ProductsRepository productsRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Submits a fresh offer from a buyer on a specific product.
     * 
     * @param buyerId The ID of the authenticated buyer.
     * @param request The data payload containing the completely new offer details.
     * @return DTO representation of the newly created Offer.
     */
    @Transactional
    @CacheEvict(value = {"market_listings", "seller_offers", "buyer_offers", "product_details", "user_listings"}, allEntries = true)
    public OfferResponseDTO submitOffer(Long buyerId, OfferSubmitRequestDTO request) {
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!"active".equalsIgnoreCase(listing.getStatus())) {
            throw new InvalidOfferException("Listing is no longer active for bargaining.");
        }
        
        if (listing.getMinAcceptablePrice() != null && request.getOfferedPrice() < listing.getMinAcceptablePrice()) {
            throw new InvalidOfferException("Offered price must be strictly greater than or equal to the minimum acceptable price: " + listing.getMinAcceptablePrice());
        }

        boolean hasActive = offerRepository.existsByBuyer_IdAndListing_IdAndStatusIn(
                buyerId, 
                listing.getId(), 
                java.util.Arrays.asList("pending", "countered")
        );
        if (hasActive) {
            throw new InvalidOfferException("You already have an active bargaining offer pending or countered for this listing!");
        }

        Integer requestedQty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;
        if (listing.getQuantity() == null || listing.getQuantity() < requestedQty) {
            throw new InvalidOfferException("Not enough listing quantity available for this offer.");
        }

        UserObj buyer = userRepository.findById(buyerId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        UserObj seller = listing.getSeller();

        Offer offer = Offer.builder()
                .product(listing.getProduct())
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .offerPrice(request.getOfferedPrice())
                .quantity(requestedQty)
                .status("pending")
                .build();

        offer = offerRepository.save(offer);
        OfferResponseDTO responseDTO = mapToResponseDTO(offer);
        
        messagingTemplate.convertAndSend("/topic/seller/" + seller.getId(), responseDTO);
        
        return responseDTO;
    }

    @Transactional(readOnly = true)
    // @org.springframework.cache.annotation.Cacheable(value = "seller_offers", key = "#sellerId + '-' + #pageable.pageNumber")
    public Page<OfferResponseDTO> getOffersForSeller(Long sellerId, Pageable pageable) {
        return offerRepository.findBySeller_Id(sellerId, pageable).map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    // @org.springframework.cache.annotation.Cacheable(value = "buyer_offers", key = "#buyerId + '-' + #pageable.pageNumber")
    public Page<OfferResponseDTO> getOffersForBuyer(Long buyerId, Pageable pageable) {
        return offerRepository.findByBuyer_Id(buyerId, pageable).map(this::mapToResponseDTO);
    }

    @Transactional
    @CacheEvict(value = {"market_listings", "seller_offers", "buyer_offers", "product_details", "user_listings"}, allEntries = true)
    public OfferResponseDTO updateOffer(Long sellerId, OfferUpdateRequestDTO request) {
        Offer offer = offerRepository.findById(request.getOfferId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!Long.valueOf(offer.getSeller().getId()).equals(sellerId)) {
            throw new UnauthorizedBargainException("Unauthorized: You are not the seller for this offer.");
        }

        if (!"pending".equalsIgnoreCase(offer.getStatus())) {
            throw new InvalidOfferException("Action not permitted. Sellers can only respond to explicitly 'pending' offers.");
        }

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        switch (action) {
            case "accept":
                deductInventory(offer);
                offer.setStatus("accepted");
                Order o = generateOrder(offer, offer.getOfferPrice());
                offer = offerRepository.save(offer);
                OfferResponseDTO responseDTO = mapToResponseDTO(offer);
                responseDTO.setCreatedOrderId(o.getId());
                messagingTemplate.convertAndSend("/topic/buyer/" + offer.getBuyer().getId(), responseDTO);
                return responseDTO;
            case "reject":
                offer.setStatus("rejected");
                break;
            case "counter":
                if (request.getCounterPrice() == null || request.getCounterPrice() <= 0) {
                    throw new InvalidOfferException("A valid counterPrice must be provided when action is 'counter'.");
                }
                offer.setCounterPrice(request.getCounterPrice());
                offer.setStatus("countered");
                break;
            default:
                throw new InvalidOfferException("Invalid action. Sellers can only 'accept', 'reject', or 'counter'.");
        }

        offer = offerRepository.save(offer);
        OfferResponseDTO responseDTO = mapToResponseDTO(offer);
        messagingTemplate.convertAndSend("/topic/buyer/" + offer.getBuyer().getId(), responseDTO);
        return responseDTO;
    }

    @Transactional
    @CacheEvict(value = {"market_listings", "seller_offers", "buyer_offers", "product_details", "user_listings"}, allEntries = true)
    public OfferResponseDTO buyerUpdateOffer(Long buyerId, OfferUpdateRequestDTO request) {
        Offer offer = offerRepository.findById(request.getOfferId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!Long.valueOf(offer.getBuyer().getId()).equals(buyerId)) {
            throw new UnauthorizedBargainException("Unauthorized: You are not the buyer for this offer.");
        }

        if (!"countered".equalsIgnoreCase(offer.getStatus())) {
            throw new InvalidOfferException("Action not permitted. Buyers can only respond to explicitly 'countered' offers.");
        }

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        switch (action) {
            case "accept":
                deductInventory(offer);
                offer.setStatus("accepted");
                Order ob = generateOrder(offer, offer.getCounterPrice());
                offer = offerRepository.save(offer);
                OfferResponseDTO responseDTOb = mapToResponseDTO(offer);
                responseDTOb.setCreatedOrderId(ob.getId());
                messagingTemplate.convertAndSend("/topic/seller/" + offer.getSeller().getId(), responseDTOb);
                return responseDTOb;
            case "reject":
                offer.setStatus("rejected");
                break;
            case "counter":
                if (request.getCounterPrice() == null || request.getCounterPrice() <= 0) {
                    throw new InvalidOfferException("A valid counterPrice must be provided when action is 'counter'.");
                }
                offer.setOfferPrice(request.getCounterPrice());
                offer.setCounterPrice(null);
                offer.setStatus("pending");
                break;
            case "cancel":
                offer.setStatus("cancelled");
                break;
            default:
                throw new InvalidOfferException("Invalid action. Buyers can only 'accept', 'reject', or 'cancel'.");
        }

        offer = offerRepository.save(offer);
        OfferResponseDTO responseDTO = mapToResponseDTO(offer);
        messagingTemplate.convertAndSend("/topic/seller/" + offer.getSeller().getId(), responseDTO);
        return responseDTO;
    }

    private void deductInventory(Offer offer) {
        Listing listing = offer.getListing();
        // Auto-heal legacy database entries that were instantiated before Quantity fields were introduced
        if (listing.getQuantity() == null) {
            listing.setQuantity(1);
        }
        
        Integer offerQuantity = offer.getQuantity() != null ? offer.getQuantity() : 1;

        if (listing.getQuantity() < offerQuantity) {
            throw new InvalidOfferException("Cannot accept offer: Insufficient listing quantity. Available: " + listing.getQuantity());
        }
        
        listing.setQuantity(listing.getQuantity() - offerQuantity);
        
        if (listing.getQuantity() <= 0) {
            listing.setStatus("sold_out");
        }
        listingRepository.save(listing);
    }

    private Order generateOrder(Offer offer, Double finalPrice) {
        Order order = Order.builder()
                .buyer(offer.getBuyer())
                .listing(offer.getListing())
                .purchasePrice(finalPrice)
                .status("completed")
                .build();
        return orderRepository.save(order);
    }

    /**
     * Internal Utility hook to convert the JPA Database Entity precisely
     * back into a Safe Controller DTO shielding sensitive back-end structure.
     */
    private OfferResponseDTO mapToResponseDTO(Offer offer) {
        return OfferResponseDTO.builder()
                .id(offer.getId())
                .listingId(offer.getListing().getId())
                .productId(offer.getProduct().getId())
                .buyerId(Long.valueOf(offer.getBuyer().getId()))
                .sellerId(Long.valueOf(offer.getSeller().getId()))
                .offerPrice(offer.getOfferPrice())
                .counterPrice(offer.getCounterPrice())
                .quantity(offer.getQuantity())
                .status(offer.getStatus())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }
}
