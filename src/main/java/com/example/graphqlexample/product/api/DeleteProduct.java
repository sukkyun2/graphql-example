package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.application.ProductCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class DeleteProduct {

    private final ProductCommandService productCommandService;

    @MutationMapping
    boolean deleteProduct(@Argument Long id) {
        productCommandService.deleteProduct(id);
        return true;
    }
}
