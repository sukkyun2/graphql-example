package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.application.ProductQueryService;
import com.example.graphqlexample.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class GetProduct {

    private final ProductQueryService productQueryService;

    @QueryMapping
    Product product(@Argument Long id) {
        return productQueryService.getProduct(id);
    }
}
