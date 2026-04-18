package com.purna.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ProductResponseDTO implements Serializable {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String category;
}
