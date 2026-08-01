package com.example.graphqlexample.order.api;

import com.example.graphqlexample.order.application.OrderCommandService;
import com.example.graphqlexample.order.domain.Order;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
class CreateOrder {

    private final OrderCommandService orderCommandService;

    @MutationMapping
    Order createOrder(@Argument OrderCreateInput input) {
        return orderCommandService.createOrder(input.toCommands());
    }

    record OrderCreateInput(List<OrderItemInput> items) {

        List<OrderCommandService.CreateOrderItemCommand> toCommands() {
            return items.stream()
                .map(item -> new OrderCommandService.CreateOrderItemCommand(item.productId(), item.quantity()))
                .toList();
        }
    }

    record OrderItemInput(Long productId, int quantity) {}
}
