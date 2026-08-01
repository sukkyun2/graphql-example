package com.example.graphqlexample.order.application;

import com.example.graphqlexample.order.domain.Order;
import com.example.graphqlexample.order.domain.OrderRepository;
import com.example.graphqlexample.order.domain.OrderStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getOrders(OrderStatus status) {
        return orderRepository.search(status);
    }
}
