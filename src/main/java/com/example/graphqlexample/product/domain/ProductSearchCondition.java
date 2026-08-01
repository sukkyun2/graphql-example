package com.example.graphqlexample.product.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductSearchCondition(
    ProductStatus status,
    String nameKeyword,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    OffsetDateTime createdFrom,
    OffsetDateTime createdTo
) {}
