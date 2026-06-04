package com.example.web.service;

import com.example.web.model.Product;
import com.example.web.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    ProductRepository repo;

    public List<Product> getProducts() {
        return repo.findAll();
    }

    public Product getProductById(int productId) {
        return repo.findById(productId)
                .orElse(new Product());
    }

    public void addProduct(Product product) {
        repo.save(product);
    }

    public void updateProduct(Product product) {
        repo.save(product);
    }

    public void deleteProduct(int productId) {
        repo.deleteById(productId);
    }
}
