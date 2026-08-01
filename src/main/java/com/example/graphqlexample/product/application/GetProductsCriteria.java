package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GetProductsCriteria(
    ProductStatus status,
    String nameKeyword,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    OffsetDateTime createdFrom,
    OffsetDateTime createdTo,
    int page,
    int size
) {}
