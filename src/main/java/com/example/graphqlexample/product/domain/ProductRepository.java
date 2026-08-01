package com.example.graphqlexample.product.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    List<Product> search(ProductSearchCondition condition, int page, int size);
}
