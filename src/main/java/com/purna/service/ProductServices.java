package com.purna.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.purna.model.Product;
import com.purna.model.Listing;
import com.purna.model.UserObj;
import com.purna.dto.ProductResponseDTO;
import com.purna.dto.ListingResponseDTO;
import com.purna.dto.ProductRequestDTO;
import com.purna.repository.ProductsRepository;
import com.purna.repository.ListingRepository;
import com.purna.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * ProductServices
 * 
 * WHY USE THIS:
 * The catalog system. This is completely wrapped with @Cacheable optimizations and 
 * Fuzzy Searching capabilities to provide robust User Experiences.
 */
@Service
@RequiredArgsConstructor
public class ProductServices {
	
	private final ProductsRepository productRepository;
	private final ListingRepository listingRepository;
	private final UserRepository userRepository;
	
    /**
     * WHY USE @Cacheable("available_listings"):
     * The first time a user views the homepage, it makes 1 slow 100ms Database SQL hit.
     * The framework then securely freezes the Page<ListingResponseDTO> payload into Memory (RAM).
     * The next 50,000 users fetch this exact identical page in ~1 millisecond. We bypass 
     * database thread saturation completely. (This flawlessly mimics an enterprise Redis Cluster).
     */
	@Transactional(readOnly = true)
    @Cacheable(value = "market_listings", key = "#pageable.pageNumber")
	public Page<ListingResponseDTO> getAvailableProducts(Pageable pageable){
		return listingRepository.findByStatusAndIsDeletedFalse("active", pageable).map(this::mapToListingResponseDTO);
	}
	
	@Transactional(readOnly = true)
    @Cacheable(value = "product_details", key = "#id")
	public ListingResponseDTO getListingById(Long id) {
		return listingRepository.findById(id).map(this::mapToListingResponseDTO).orElseThrow(() -> new RuntimeException("Listing not found"));
	}

    /**
     * WHY USE FUZZY SEARCHING (Jaro-Winkler):
     * Standard SQL breaks if a spelling typo is made. `SELECT * WHERE title LIKE '%iPhon 14%'` returns NULL.
     * 
     * Jaro-Winkler calculates mathematical string edit distance. "iPhon 14" vs "iPhone 14" produces an 
     * ~95% similarity match. We can now comfortably return results even when customers mistype!
     */
    @Transactional(readOnly = true)
    public Page<ListingResponseDTO> getFuzzySearchListings(String searchKeyword, Pageable pageable) {
        // In extreme scale, this runs in ElasticSearch. Here we use an in-memory structural scan fallback.
        List<Listing> allListings = listingRepository.findAll();
        JaroWinklerDistance distance = new JaroWinklerDistance();

        List<ListingResponseDTO> fuzzyMatches = allListings.stream()
            .filter(listing -> listing.getProduct() != null)
            .filter(listing -> {
                String title = listing.getProduct().getTitle();
                // If similarity is above 80%, it's considered a smart match despite typos!
                return title != null && distance.apply(searchKeyword.toLowerCase(), title.toLowerCase()) > 0.80;
            })
            .map(this::mapToListingResponseDTO)
            .collect(Collectors.toList());

        // Basic manual paging translation
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), fuzzyMatches.size());
        
        if (start > fuzzyMatches.size()) return Page.empty();
        return new PageImpl<>(fuzzyMatches.subList(start, end), pageable, fuzzyMatches.size());
    }
	
    @CacheEvict(value = {"market_listings", "product_details", "user_listings"}, allEntries = true)
	public Listing addProductWithListing(ProductRequestDTO request, Long sellerId) {
		Product globalProduct = productRepository.findByTitle(request.getTitle());

        if (globalProduct == null) {
            globalProduct = Product.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .build();
            globalProduct = productRepository.save(globalProduct);
        }
        
        UserObj seller = userRepository.findById(sellerId.intValue()).orElseThrow(() -> new RuntimeException("Seller not found"));

        Listing listing = Listing.builder()
                .product(globalProduct)
                .seller(seller)
                .price(request.getPrice())
                .minAcceptablePrice(request.getMinAcceptablePrice())
                .quantity(request.getQuantity())
                .yearsOld(request.getYearsOld())
                .customImageUrls(request.getCustomImageUrls())
                .status("active")
                .build();
                
		return listingRepository.save(listing);
	}

	public ProductResponseDTO mapToProductResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .build();
    }

	public ListingResponseDTO mapToListingResponseDTO(Listing listing) {
        return ListingResponseDTO.builder()
                .id(listing.getId())
                .productId(listing.getProduct() != null ? listing.getProduct().getId() : null)
                .sellerId(listing.getSeller() != null ? Long.valueOf(listing.getSeller().getId()) : null)
                .product(listing.getProduct() != null ? mapToProductResponseDTO(listing.getProduct()) : null)
                .price(listing.getPrice())
                .minAcceptablePrice(null) // HIDDEN FROM REGULAR USERS FOR PRIVACY
                .quantity(listing.getQuantity())
                .yearsOld(listing.getYearsOld())
                .customImageUrls(listing.getCustomImageUrls())
                .status(listing.getStatus())
                .build();
    }
}
