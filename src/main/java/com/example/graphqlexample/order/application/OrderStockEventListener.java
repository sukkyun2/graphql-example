package com.example.graphqlexample.order.application;

import com.example.graphqlexample.order.domain.OrderCancelledEvent;
import com.example.graphqlexample.order.domain.OrderCreatedEvent;
import com.example.graphqlexample.product.application.InsufficientStockException;
import com.example.graphqlexample.product.application.ProductCommandService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
class OrderStockEventListener {

    private final ProductCommandService productCommandService;
    private final OrderCommandService orderCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderCreated(OrderCreatedEvent event) {
        try {
            productCommandService.decreaseStock(toStockChanges(event.items()));
        } catch (InsufficientStockException e) {
            // ponytail: single compensating cancel, no retry/outbox — CLAUDE.md allows
            // skipping that at this monolith's early stage. Upgrade path: retry-with-backoff,
            // then a buyer notification, if this path starts firing often enough to matter.
            orderCommandService.cancelDueToStockShortage(event.orderId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderCancelled(OrderCancelledEvent event) {
        productCommandService.restoreStock(toCancelledStockChanges(event.items()));
    }

    private List<ProductCommandService.StockChange> toStockChanges(List<OrderCreatedEvent.Item> items) {
        return items.stream()
            .map(item -> new ProductCommandService.StockChange(item.productId(), item.quantity()))
            .toList();
    }

    private List<ProductCommandService.StockChange> toCancelledStockChanges(List<OrderCancelledEvent.Item> items) {
        return items.stream()
            .map(item -> new ProductCommandService.StockChange(item.productId(), item.quantity()))
            .toList();
    }
}
