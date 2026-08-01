package com.example.graphqlexample.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductSearchCondition(
    ProductStatus status,
    String nameKeyword,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    LocalDateTime createdFrom,
    LocalDateTime createdTo
) {}
