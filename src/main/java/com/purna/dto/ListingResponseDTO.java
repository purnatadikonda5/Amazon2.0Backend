package com.purna.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ListingResponseDTO implements Serializable {
    private Long id;
    private Long productId;
    private Long sellerId;
    private ProductResponseDTO product;
    private Double price;
    private Double minAcceptablePrice;
    private Integer quantity;
    private String status;
    private Integer yearsOld;
    private String customImageUrls;
}
