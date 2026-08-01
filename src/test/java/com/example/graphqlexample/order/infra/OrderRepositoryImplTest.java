package com.example.graphqlexample.order.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.graphqlexample.order.domain.Order;
import com.example.graphqlexample.order.domain.OrderItem;
import com.example.graphqlexample.order.domain.OrderRepository;
import com.example.graphqlexample.order.domain.OrderStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class OrderRepositoryImplTest {

    @Autowired
    private OrderJpaRepository jpaRepository;

    @Autowired
    private EntityManager entityManager;

    private OrderRepository repository() {
        return new OrderRepositoryImpl(jpaRepository, new JPAQueryFactory(entityManager));
    }

    @Test
    @DisplayName("상태로 검색하면 해당 상태의 주문만 조회된다")
    void search_filtersByStatus() {
        Order pending = jpaRepository.save(Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000)))));

        Order paid = Order.create(List.of(OrderItem.of(2L, 1, BigDecimal.valueOf(1000))));
        paid.changeStatus(OrderStatus.PAID);
        jpaRepository.save(paid);

        var result = repository().search(OrderStatus.PENDING);

        assertThat(result).extracting(Order::getId).containsExactly(pending.getId());
    }

    @Test
    @DisplayName("상태 없이 검색하면 전체 주문이 조회된다")
    void search_withoutStatus_returnsAll() {
        jpaRepository.save(Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000)))));
        jpaRepository.save(Order.create(List.of(OrderItem.of(2L, 1, BigDecimal.valueOf(1000)))));

        assertThat(repository().search(null)).hasSize(2);
    }
}
