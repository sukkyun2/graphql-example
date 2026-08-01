package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductRepository;
import com.example.graphqlexample.product.domain.ProductSearchCondition;
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
        var condition = new ProductSearchCondition(
            criteria.status(),
            criteria.nameKeyword(),
            criteria.minPrice(),
            criteria.maxPrice(),
            criteria.createdFrom(),
            criteria.createdTo()
        );
        return productRepository.search(condition, criteria.page(), criteria.size());
    }
}
