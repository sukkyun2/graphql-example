package com.example.graphqlexample.product.api;

import java.math.BigDecimal;

record ProductCreateInput(String name, BigDecimal price, int stock) {}
