package com.example.graphqlexample.order.api;

import com.example.graphqlexample.order.application.OrderCommandService;
import com.example.graphqlexample.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class CancelOrder {

    private final OrderCommandService orderCommandService;

    @MutationMapping
    Order cancelOrder(@Argument Long id) {
        return orderCommandService.cancelOrder(id);
    }
}
