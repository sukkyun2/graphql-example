package com.example.graphqlexample.order.application;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.graphqlexample.order.domain.OrderCancelledEvent;
import com.example.graphqlexample.order.domain.OrderCreatedEvent;
import com.example.graphqlexample.product.application.InsufficientStockException;
import com.example.graphqlexample.product.application.ProductCommandService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderStockEventListenerTest {

    @Mock
    private ProductCommandService productCommandService;

    @Mock
    private OrderCommandService orderCommandService;

    @InjectMocks
    private OrderStockEventListener orderStockEventListener;

    @Test
    @DisplayName("주문 생성 이벤트를 받으면 상품 재고를 차감한다")
    void onOrderCreated_decreasesStock() {
        var event = new OrderCreatedEvent(1L, List.of(new OrderCreatedEvent.Item(10L, 2)));

        orderStockEventListener.onOrderCreated(event);

        verify(productCommandService).decreaseStock(anyList());
    }

    @Test
    @DisplayName("재고 차감이 실패하면 해당 주문을 자동으로 취소 처리한다")
    void onOrderCreated_whenStockInsufficient_cancelsOrder() {
        var event = new OrderCreatedEvent(1L, List.of(new OrderCreatedEvent.Item(10L, 2)));
        doThrow(new InsufficientStockException(10L)).when(productCommandService).decreaseStock(anyList());

        orderStockEventListener.onOrderCreated(event);

        verify(orderCommandService).cancelDueToStockShortage(1L);
    }

    @Test
    @DisplayName("주문 취소 이벤트를 받으면 상품 재고를 복원한다")
    void onOrderCancelled_restoresStock() {
        var event = new OrderCancelledEvent(1L, List.of(new OrderCancelledEvent.Item(10L, 2)));

        orderStockEventListener.onOrderCancelled(event);

        verify(productCommandService).restoreStock(anyList());
    }
}
