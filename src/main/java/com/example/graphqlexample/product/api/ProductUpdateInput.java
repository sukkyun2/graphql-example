package com.example.graphqlexample.product.api;

import java.math.BigDecimal;

record ProductUpdateInput(String name, BigDecimal price, Integer stock, String status) {}
