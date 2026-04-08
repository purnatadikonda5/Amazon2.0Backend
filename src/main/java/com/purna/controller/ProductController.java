package com.purna.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.purna.model.Product;
import com.purna.repository.ProductsRepository;
import com.purna.service.ProductServices;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductServices service;

    @Autowired
    private ProductsRepository repository;

    // GET all products
    @GetMapping
    public ResponseEntity<?> getProducts() {
        try {
            List<Product> products = service.getProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch products");
        }
    }

    // GET product by id
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            Optional<Product> product = repository.findById(id);

            if (product.isEmpty()) {
                return ResponseEntity.status(404).body("Product not found");
            }

            return ResponseEntity.ok(product.get());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching product");
        }
    }

    // POST add product
    @PostMapping
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        try {
            Product saved= service.addProduct(product);
            return ResponseEntity.status(201).body(saved);

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(400).body("Duplicate or invalid data");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error adding product");
        }
    }
}