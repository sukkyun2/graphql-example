package com.example.graphqlexample.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void create_withValidInput_startsOnSale() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(10000), 5);

        assertThat(product.getName()).isEqualTo("keyboard");
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(product.getStock()).isEqualTo(5);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getDeletedAt()).isNull();
        assertThat(product.getCreatedAt()).isEqualTo(product.getUpdatedAt());
    }

    @Test
    void create_withBlankName_throws() {
        assertThatThrownBy(() -> Product.create("  ", BigDecimal.TEN, 1))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    void create_withNegativePrice_throws() {
        assertThatThrownBy(() -> Product.create("keyboard", BigDecimal.valueOf(-1), 1))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    void create_withNegativeStock_throws() {
        assertThatThrownBy(() -> Product.create("keyboard", BigDecimal.TEN, -1))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    void update_withPartialCommand_onlyChangesGivenFields() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(10000), 5);
        var createdAt = product.getUpdatedAt();

        product.update(new Product.UpdateCommand(null, BigDecimal.valueOf(20000), null, null));

        assertThat(product.getName()).isEqualTo("keyboard");
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(product.getStock()).isEqualTo(5);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void update_withNegativeStock_throws() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(10000), 5);

        assertThatThrownBy(() -> product.update(new Product.UpdateCommand(null, null, -1, null)))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    void delete_setsDeletedAtAndTouchesUpdatedAt() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(10000), 5);

        product.delete();

        assertThat(product.getDeletedAt()).isNotNull();
    }
}
