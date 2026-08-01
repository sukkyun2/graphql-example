package com.example.graphqlexample.order.api;

import com.example.graphqlexample.order.application.OrderCommandService;
import com.example.graphqlexample.order.domain.Order;
import com.example.graphqlexample.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class UpdateOrder {

    private final OrderCommandService orderCommandService;

    @MutationMapping
    Order updateOrderStatus(@Argument Long id, @Argument OrderStatus status) {
        return orderCommandService.updateOrderStatus(id, status);
    }
}
