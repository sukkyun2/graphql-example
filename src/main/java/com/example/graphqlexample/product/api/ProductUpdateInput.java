package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.domain.ProductStatus;
import java.math.BigDecimal;

record ProductUpdateInput(String name, BigDecimal price, Integer stock, ProductStatus status) {}
