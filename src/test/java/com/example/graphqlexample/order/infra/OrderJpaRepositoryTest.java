package com.example.graphqlexample.order.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.graphqlexample.order.domain.Order;
import com.example.graphqlexample.order.domain.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class OrderJpaRepositoryTest {

    @Autowired
    private OrderJpaRepository repository;

    @Test
    @DisplayName("주문을 저장하면 주문 항목과 함께 조회된다")
    void save_persistsOrderWithItems() {
        Order order = Order.create(List.of(OrderItem.of(1L, 2, BigDecimal.valueOf(1000))));

        Order saved = repository.save(order);
        Order found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(found.getItems().get(0).getQuantity()).isEqualTo(2);
    }
}
