package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductNotFoundException;
import com.example.graphqlexample.product.domain.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;

    public void createProduct(String name, BigDecimal price, int stock) {
        Product product = Product.create(name, price, stock);
        productRepository.save(product);
    }

    public void updateProduct(Long id, Product.UpdateCommand command) {
        Product product = getProductInternal(id);
        product.update(command);
    }

    public void deleteProduct(Long id) {
        Product product = getProductInternal(id);
        product.delete();
    }

    private Product getProductInternal(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
