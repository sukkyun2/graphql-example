package com.example.graphqlexample.order.domain;

import java.util.List;

public record OrderCancelledEvent(Long orderId, List<Item> items) {

    public record Item(Long productId, int quantity) {}
}
