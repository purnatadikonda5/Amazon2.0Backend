package com.purna.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.purna.model.Product;
import com.purna.repository.ProductsRepository;

@Service
public class ProductServices {
	
	@Autowired
	private ProductsRepository repository;
	
	public List<Product> getProducts(){
		List<Product> products= new ArrayList<Product>();
		products= repository.findAll();
		System.out.println(products);
		return products;
	}
	
	public Product getProductById(Long id) {
		Product product= repository.findById(id).get();
		return product;
	}
	
	public Product addProduct(Product product) throws Exception {
		Product productobj= repository.findByTitleAndSellerId(
                product.getTitle(),
                product.getSellerId()
        );
        boolean exists =(productobj!=null); 

        if (exists) {
            throw new DataIntegrityViolationException("Product already exists");
        }
		return repository.save(product);
	}
}
