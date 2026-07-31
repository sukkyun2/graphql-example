package com.example.graphqlexample.product.api;

import com.example.graphqlexample.product.domain.ProductStatus;
import org.springframework.stereotype.Component;

@Component
class ProductStatusMapper {

    ProductStatus toDomain(String status) {
        return status != null ? ProductStatus.valueOf(status) : null;
    }
}
