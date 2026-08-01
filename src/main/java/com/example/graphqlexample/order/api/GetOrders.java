package com.example.graphqlexample.order.api;

import com.example.graphqlexample.order.application.OrderQueryService;
import com.example.graphqlexample.order.domain.Order;
import com.example.graphqlexample.order.domain.OrderStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class GetOrders {

    private final OrderQueryService orderQueryService;

    @QueryMapping
    List<Order> orders(@Argument OrderStatus status) {
        return orderQueryService.getOrders(status);
    }
}
