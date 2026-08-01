package com.example.graphqlexample.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.graphqlexample.order.domain.Order;
import com.example.graphqlexample.order.domain.OrderItem;
import com.example.graphqlexample.order.domain.OrderRepository;
import com.example.graphqlexample.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    @Test
    @DisplayName("존재하는 주문을 조회하면 해당 주문을 반환한다")
    void getOrder_whenFound_returnsOrder() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThat(orderQueryService.getOrder(1L)).isEqualTo(order);
    }

    @Test
    @DisplayName("존재하지 않는 주문을 조회하면 실패한다")
    void getOrder_whenNotFound_throws() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderQueryService.getOrder(1L))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("상태별 주문 목록 조회는 저장소 검색 결과를 그대로 반환한다")
    void getOrders_delegatesToRepositorySearch() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        when(orderRepository.search(OrderStatus.PENDING)).thenReturn(List.of(order));

        assertThat(orderQueryService.getOrders(OrderStatus.PENDING)).containsExactly(order);
    }
}
