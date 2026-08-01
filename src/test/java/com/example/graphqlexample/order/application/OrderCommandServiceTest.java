package com.example.graphqlexample.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.graphqlexample.order.domain.Order;
import com.example.graphqlexample.order.domain.OrderCancelledEvent;
import com.example.graphqlexample.order.domain.OrderCreatedEvent;
import com.example.graphqlexample.order.domain.OrderItem;
import com.example.graphqlexample.order.domain.OrderRepository;
import com.example.graphqlexample.order.domain.OrderStatus;
import com.example.graphqlexample.product.application.InsufficientStockException;
import com.example.graphqlexample.product.application.ProductNotFoundException;
import com.example.graphqlexample.product.application.ProductQueryService;
import com.example.graphqlexample.product.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductQueryService productQueryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderCommandService orderCommandService;

    @Test
    @DisplayName("재고가 충분하면 주문이 생성되고 주문 생성 이벤트가 정확히 한 번 발행된다")
    void createOrder_withSufficientStock_savesOrderAndPublishesEventOnce() {
        Product product = withId(Product.create("keyboard", BigDecimal.valueOf(1000), 5), 1L);
        when(productQueryService.findAllByIds(anyList())).thenReturn(List.of(product));
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class)))
            .thenAnswer(invocation -> withOrderId(invocation.getArgument(0), 100L));

        Order order = orderCommandService.createOrder(
            List.of(new OrderCommandService.CreateOrderItemCommand(1L, 2)));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(100L);
        assertThat(captor.getValue().items()).containsExactly(new OrderCreatedEvent.Item(1L, 2));
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 저장되지 않고 재고 부족 예외가 발생한다")
    void createOrder_withInsufficientStock_throwsAndDoesNotSave() {
        Product product = withId(Product.create("keyboard", BigDecimal.valueOf(1000), 1), 1L);
        when(productQueryService.findAllByIds(anyList())).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderCommandService.createOrder(
            List.of(new OrderCommandService.CreateOrderItemCommand(1L, 2))))
            .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 주문하면 실패한다")
    void createOrder_withUnknownProduct_throws() {
        when(productQueryService.findAllByIds(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> orderCommandService.createOrder(
            List.of(new OrderCommandService.CreateOrderItemCommand(1L, 1))))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("정상 순서로 상태를 변경하면 반영된다")
    void updateOrderStatus_withValidTransition_succeeds() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderCommandService.updateOrderStatus(1L, OrderStatus.PAID);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("잘못된 순서로 상태를 변경하면 실패한다")
    void updateOrderStatus_withInvalidTransition_throws() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderCommandService.updateOrderStatus(1L, OrderStatus.SHIPPING))
            .isInstanceOf(com.example.graphqlexample.order.domain.InvalidOrderStatusTransitionException.class);
    }

    @Test
    @DisplayName("주문을 취소하면 주문 취소 이벤트가 발행된다")
    void cancelOrder_publishesOrderCancelledEvent() {
        Order order = Order.create(List.of(OrderItem.of(1L, 3, BigDecimal.valueOf(1000))));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderCommandService.cancelOrder(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().items()).containsExactly(new OrderCancelledEvent.Item(1L, 3));
    }

    @Test
    @DisplayName("배송중인 주문을 취소하려 하면 실패하고 이벤트도 발행되지 않는다")
    void cancelOrder_whenShipping_throwsAndDoesNotPublish() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.SHIPPING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderCommandService.cancelOrder(1L))
            .isInstanceOf(com.example.graphqlexample.order.domain.InvalidOrderStatusTransitionException.class);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("재고 차감 실패로 인한 자동 취소는 재고 복원 이벤트를 발행하지 않는다")
    void cancelDueToStockShortage_doesNotPublishCancelledEvent() {
        Order order = Order.create(List.of(OrderItem.of(1L, 1, BigDecimal.valueOf(1000))));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderCommandService.cancelDueToStockShortage(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private static Product withId(Product product, Long id) {
        try {
            var field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(product, id);
            return product;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Order withOrderId(Order order, Long id) {
        try {
            var field = Order.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
            return order;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
