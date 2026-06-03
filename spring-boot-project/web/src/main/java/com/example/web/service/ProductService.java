package com.example.web.service;

import com.example.web.model.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ProductService {

    List<Product> products = new ArrayList<>(Arrays.asList(
            new Product(101, "Iphone", 50000),
            new Product(102, "Canon Camera", 70000)
    ));

    public List<Product> getProducts() {
        return products;
    }

    public Product getProductById(int productId) {
        return products.stream()
                .filter(p -> p.getProductId() == productId)
                .findFirst()
                .orElse(new Product(100, "No item", 0));
    }

    public void addProduct(Product product) {
        products.add(product);
    }

    public void updateProduct(Product product) {
        int index = 0;
        for(int i=0;i<products.size();i++) {
            if(products.get(i).getProductId() == product.getProductId()) {
                index = i;
            }
        }
        products.set(index, product);
    }

    public void deleteProduct(int productId) {
        int index = 0;
        for(int i=0;i<products.size();i++) {
            if(products.get(i).getProductId() == productId) {
                index = i;
            }
        }
        products.remove(index);
    }
}
