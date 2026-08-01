package com.example.graphqlexample.order.application;

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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final ProductQueryService productQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public Order createOrder(List<CreateOrderItemCommand> commands) {
        List<Long> productIds = commands.stream().map(CreateOrderItemCommand::productId).toList();
        Map<Long, Product> products = productQueryService.findAllByIds(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<OrderItem> items = commands.stream()
            .map(command -> toOrderItem(command, products))
            .toList();

        Order order = orderRepository.save(Order.create(items));

        eventPublisher.publishEvent(new OrderCreatedEvent(
            order.getId(),
            items.stream()
                .map(item -> new OrderCreatedEvent.Item(item.getProductId(), item.getQuantity()))
                .toList()
        ));

        return order;
    }

    public Order updateOrderStatus(Long id, OrderStatus status) {
        Order order = getOrderInternal(id);
        applyTransition(order, status);
        return order;
    }

    public Order cancelOrder(Long id) {
        return updateOrderStatus(id, OrderStatus.CANCELLED);
    }

    // REQUIRES_NEW: called from OrderStockEventListener within an AFTER_COMMIT callback,
    // where the original transaction has already closed (see ProductCommandService
    // .decreaseStock for the same reasoning).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void cancelDueToStockShortage(Long id) {
        getOrderInternal(id).changeStatus(OrderStatus.CANCELLED);
    }

    private OrderItem toOrderItem(CreateOrderItemCommand command, Map<Long, Product> products) {
        Product product = products.get(command.productId());
        if (product == null) {
            throw new ProductNotFoundException(command.productId());
        }
        if (product.getStock() < command.quantity()) {
            throw new InsufficientStockException(command.productId());
        }
        return OrderItem.of(command.productId(), command.quantity(), product.getPrice());
    }

    private void applyTransition(Order order, OrderStatus status) {
        order.changeStatus(status);
        if (status == OrderStatus.CANCELLED) {
            eventPublisher.publishEvent(new OrderCancelledEvent(
                order.getId(),
                order.getItems().stream()
                    .map(item -> new OrderCancelledEvent.Item(item.getProductId(), item.getQuantity()))
                    .toList()
            ));
        }
    }

    private Order getOrderInternal(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public record CreateOrderItemCommand(Long productId, int quantity) {}
}
