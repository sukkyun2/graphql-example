package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetProductsCriteria(
    ProductStatus status,
    String nameKeyword,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    LocalDateTime createdFrom,
    LocalDateTime createdTo,
    int page,
    int size
) {}
