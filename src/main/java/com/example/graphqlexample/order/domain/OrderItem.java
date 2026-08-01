package com.example.graphqlexample.order.domain;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    private Long productId;

    private int quantity;

    private BigDecimal unitPrice;

    private OrderItem(Long productId, int quantity, BigDecimal unitPrice) {
        if (productId == null) {
            throw new InvalidOrderArgumentException("상품 ID는 필수입니다");
        }
        if (quantity <= 0) {
            throw new InvalidOrderArgumentException("주문 수량은 1개 이상이어야 합니다");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderArgumentException("주문 단가는 0 이상이어야 합니다");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static OrderItem of(Long productId, int quantity, BigDecimal unitPrice) {
        return new OrderItem(productId, quantity, unitPrice);
    }
}
