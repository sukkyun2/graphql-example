package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.ProductStatus;

public record GetProductsCriteria(ProductStatus status, int page, int size) {}
