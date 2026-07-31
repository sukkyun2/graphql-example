package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductNotFoundException;
import com.example.graphqlexample.product.domain.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;

    public void createProduct(String name, BigDecimal price, int stock) {
        productRepository.save(Product.create(name, price, stock));
    }

    public void updateProduct(Long id, Product.UpdateCommand command) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        product.update(command);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        product.delete();
    }
}
