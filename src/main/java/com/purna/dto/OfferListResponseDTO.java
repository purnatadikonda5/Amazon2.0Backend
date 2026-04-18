package com.purna.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferListResponseDTO {
    private List<OfferResponseDTO> offers;
    private int pageNumber;
    private int totalPages;
    private long totalElements;
    private boolean last;
    
    // Legacy constructor wrapper for backward compatibility with List-only initialization
    public OfferListResponseDTO(List<OfferResponseDTO> offers) {
        this.offers = offers;
    }
}
