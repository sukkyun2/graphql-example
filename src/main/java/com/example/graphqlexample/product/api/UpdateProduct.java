package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.application.ProductCommandService;
import com.example.graphqlexample.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class UpdateProduct {

    private final ProductCommandService productCommandService;
    private final ProductStatusMapper productStatusMapper;

    @MutationMapping
    boolean updateProduct(@Argument Long id, @Argument ProductUpdateInput input) {
        var command = new Product.UpdateCommand(
            input.name(), input.price(), input.stock(),
            productStatusMapper.toDomain(input.status()));
        productCommandService.updateProduct(id, command);
        return true;
    }
}
