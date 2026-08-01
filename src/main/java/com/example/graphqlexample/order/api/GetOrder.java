package com.example.graphqlexample.order.api;

import com.example.graphqlexample.order.application.OrderQueryService;
import com.example.graphqlexample.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class GetOrder {

    private final OrderQueryService orderQueryService;

    @QueryMapping
    Order order(@Argument Long id) {
        return orderQueryService.getOrder(id);
    }
}
