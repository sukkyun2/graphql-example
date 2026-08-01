package com.example.graphqlexample.order.api;

import com.example.graphqlexample.order.domain.OrderItem;
import com.example.graphqlexample.product.application.ProductQueryService;
import com.example.graphqlexample.product.domain.Product;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class GetOrderItemProduct {

    private final ProductQueryService productQueryService;

    @BatchMapping(typeName = "OrderItem", field = "product")
    Map<OrderItem, Product> product(List<OrderItem> items) {
        List<Long> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        Map<Long, Product> byId = productQueryService.findAllByIds(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<OrderItem, Product> result = new HashMap<>();
        for (OrderItem item : items) {
            result.put(item, byId.get(item.getProductId()));
        }
        return result;
    }
}
