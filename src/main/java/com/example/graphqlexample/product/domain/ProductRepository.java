package com.example.graphqlexample.product.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    List<Product> search(ProductSearchCondition condition, int page, int size);

    List<Product> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    long decreaseStockIfSufficient(Long productId, int quantity);

    void increaseStock(Long productId, int quantity);
}
