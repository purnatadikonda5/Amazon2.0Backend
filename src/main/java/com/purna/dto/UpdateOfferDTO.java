package com.purna.dto;

public class UpdateOfferDTO {
	private Long  id;
	private String status;
	private Double counterPrice;
	public UpdateOfferDTO() {
		
	}
	public UpdateOfferDTO(Long id, String status) {
		this.id = id;
		this.status = status;
	}
	public UpdateOfferDTO(Long id, String status, Double counterPrice) {
		this.id = id;
		this.status = status;
		this.counterPrice = counterPrice;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return "UpdateOfferDTO [id=" + id + ", status=" + status + ", counterPrice=" + counterPrice + "]";
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Double getCounterPrice() {
		return counterPrice;
	}
	public void setCounterPrice(Double counterPrice) {
		this.counterPrice = counterPrice;
	}
}
