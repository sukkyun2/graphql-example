package com.example.graphqlexample.product.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    private int stock;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Product(String name, BigDecimal price, int stock) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.status = ProductStatus.ON_SALE;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Product create(String name, BigDecimal price, int stock) {
        return new Product(name, price, stock);
    }

    public void update(UpdateCommand command) {
        if (command.name() != null) {
            validateName(command.name());
            this.name = command.name();
        }
        if (command.price() != null) {
            validatePrice(command.price());
            this.price = command.price();
        }
        if (command.stock() != null) {
            validateStock(command.stock());
            this.stock = command.stock();
        }
        if (command.status() != null) {
            this.status = command.status();
        }
        touch();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidProductArgumentException("상품명은 비어있을 수 없습니다");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductArgumentException("가격은 0 이상이어야 합니다");
        }
    }

    private static void validateStock(Integer stock) {
        if (stock < 0) {
            throw new InvalidProductArgumentException("재고는 0 이상이어야 합니다");
        }
    }

    public record UpdateCommand(String name, BigDecimal price, Integer stock, ProductStatus status) {}
}
