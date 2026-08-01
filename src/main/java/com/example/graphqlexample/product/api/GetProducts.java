package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.application.GetProductsCriteria;
import com.example.graphqlexample.product.application.ProductQueryService;
import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class GetProducts {

    private final ProductQueryService productQueryService;

    @QueryMapping
    List<Product> products(@Arguments SearchRequestRecord request) {
        return productQueryService.getProducts(request.toCriteria());
    }

    record SearchRequestRecord(
        ProductStatus status,
        String nameKeyword,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo,
        int page,
        int size
    ) {

        GetProductsCriteria toCriteria() {
            return new GetProductsCriteria(
                status, nameKeyword, minPrice, maxPrice, createdFrom, createdTo, page, size);
        }
    }
}
