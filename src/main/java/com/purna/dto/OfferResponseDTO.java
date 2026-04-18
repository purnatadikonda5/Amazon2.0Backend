package com.purna.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OfferResponseDTO {
    private Long id;
    private Long listingId;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private Double offerPrice;
    private Double counterPrice;
    private Integer quantity;
    private String status;
    private Long createdOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
