package com.purna.dto;

import lombok.Data;

@Data
public class ProductRequestDTO {
    private String title;
    private String description;
    private Double price;
    private String imageUrl;
    private String category;
    private Integer quantity;
    private Double minAcceptablePrice;
    private Boolean isAvailable;
    private Integer yearsOld;
    private String customImageUrls;
}
