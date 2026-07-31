package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductNotFoundException;
import com.example.graphqlexample.product.domain.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;

    public Product getProduct(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> getProducts(GetProductsCriteria criteria) {
        return productRepository.search(criteria.status(), criteria.page(), criteria.size());
    }
}
