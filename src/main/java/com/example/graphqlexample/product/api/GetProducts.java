package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.application.GetProductsCriteria;
import com.example.graphqlexample.product.application.ProductQueryService;
import com.example.graphqlexample.product.domain.Product;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class GetProducts {

    private final ProductQueryService productQueryService;
    private final ProductStatusMapper productStatusMapper;

    @QueryMapping
    List<Product> products(@Argument String status, @Argument int page, @Argument int size) {
        var criteria = new GetProductsCriteria(productStatusMapper.toDomain(status), page, size);
        return productQueryService.getProducts(criteria);
    }
}
