package com.purna.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name= "amazon_products")
public class Product {
	Product(){
		
	}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Double price;   // base price (starting price)

    private String imageUrl;

    private String category;

    private Boolean isAvailable; // true = still open for bargaining

    private Long sellerId; // reference to User

    private Double minAcceptablePrice; 
    // optional → seller lowest limit (very useful for bargaining logic)

    private String status; 
    // ACTIVE, SOLD, REMOVED

    private Integer quantity; // for future scalability

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Boolean getIsAvailable() {
		return isAvailable;
	}

	public void setIsAvailable(Boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public Long getSellerId() {
		return sellerId;
	}

	public void setSellerId(Long sellerId) {
		this.sellerId = sellerId;
	}

	public Double getMinAcceptablePrice() {
		return minAcceptablePrice;
	}

	public void setMinAcceptablePrice(Double minAcceptablePrice) {
		this.minAcceptablePrice = minAcceptablePrice;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Product(String title, String description, Double price, String imageUrl, String category,
			Boolean isAvailable, Long sellerId, Double minAcceptablePrice, String status, Integer quantity) {
		super();
		this.title = title;
		this.description = description;
		this.price = price;
		this.imageUrl = imageUrl;
		this.category = category;
		this.isAvailable = isAvailable;
		this.sellerId = sellerId;
		this.minAcceptablePrice = minAcceptablePrice;
		this.status = status;
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Product [id=" + id + ", title=" + title + ", description=" + description + ", price=" + price
				+ ", imageUrl=" + imageUrl + ", category=" + category + ", isAvailable=" + isAvailable + ", sellerId="
				+ sellerId + ", minAcceptablePrice=" + minAcceptablePrice + ", status=" + status + ", quantity="
				+ quantity + "]";
	}
    
}