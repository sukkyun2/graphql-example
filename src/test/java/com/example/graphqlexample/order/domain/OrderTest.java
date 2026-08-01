package com.example.graphqlexample.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    @DisplayName("새 주문을 생성하면 접수 상태로 시작한다")
    void create_withValidItems_startsPending() {
        Order order = Order.create(List.of(OrderItem.of(1L, 2, BigDecimal.valueOf(1000))));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("주문 항목 없이 주문을 생성하면 실패한다")
    void create_withNoItems_throws() {
        assertThatThrownBy(() -> Order.create(List.of()))
            .isInstanceOf(InvalidOrderArgumentException.class);
    }

    @Test
    @DisplayName("접수부터 배송완료까지 순서대로는 상태 변경이 허용된다")
    void changeStatus_followsForwardSequence_succeeds() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));

        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.SHIPPING);
        order.changeStatus(OrderStatus.COMPLETED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("순서를 건너뛰어 상태를 변경하면 거부된다")
    void changeStatus_skippingSteps_throws() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.SHIPPING))
            .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    @DisplayName("배송완료된 주문은 더 이상 상태를 변경할 수 없다")
    void changeStatus_afterCompleted_throws() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.SHIPPING);
        order.changeStatus(OrderStatus.COMPLETED);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELLED))
            .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    @DisplayName("접수 상태의 주문은 취소할 수 있다")
    void changeStatus_fromPendingToCancelled_succeeds() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));

        order.changeStatus(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("결제완료 상태의 주문은 취소할 수 있다")
    void changeStatus_fromPaidToCancelled_succeeds() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        order.changeStatus(OrderStatus.PAID);

        order.changeStatus(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("배송이 시작된 주문은 취소할 수 없다")
    void changeStatus_fromShippingToCancelled_throws() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.SHIPPING);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELLED))
            .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    @DisplayName("상품 ID 없이 주문 항목을 만들면 실패한다")
    void orderItem_withoutProductId_throws() {
        assertThatThrownBy(() -> OrderItem.of(null, 1, BigDecimal.valueOf(1000)))
            .isInstanceOf(InvalidOrderArgumentException.class);
    }

    @Test
    @DisplayName("수량을 0 이하로 담으면 실패한다")
    void orderItem_withNonPositiveQuantity_throws() {
        assertThatThrownBy(() -> OrderItem.of(1L, 0, BigDecimal.valueOf(1000)))
            .isInstanceOf(InvalidOrderArgumentException.class);
    }

    @Test
    @DisplayName("단가를 음수로 담으면 실패한다")
    void orderItem_withNegativeUnitPrice_throws() {
        assertThatThrownBy(() -> OrderItem.of(1L, 1, BigDecimal.valueOf(-1)))
            .isInstanceOf(InvalidOrderArgumentException.class);
    }
}
