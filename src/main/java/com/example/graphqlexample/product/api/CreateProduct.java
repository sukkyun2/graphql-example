package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.application.ProductCommandService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class CreateProduct {

    private final ProductCommandService productCommandService;

    @MutationMapping
    boolean createProduct(@Argument ProductCreateInput input) {
        productCommandService.createProduct(input.name(), input.price(), input.stock());
        return true;
    }

    record ProductCreateInput(String name, BigDecimal price, int stock) {}
}
