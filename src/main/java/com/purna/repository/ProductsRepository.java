package com.purna.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.purna.model.Product;

@Repository
public interface ProductsRepository extends JpaRepository<Product, Long> {

	Product findByTitleAndSellerId(String title, Long sellerId);
	
}
