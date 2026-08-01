package com.example.graphqlexample.order.domain;

public class InvalidOrderStatusTransitionException extends RuntimeException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("주문 상태를 %s에서 %s(으)로 변경할 수 없습니다".formatted(from, to));
    }
}
