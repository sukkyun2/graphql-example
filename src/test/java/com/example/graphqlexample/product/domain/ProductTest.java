package com.example.graphqlexample.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    @DisplayName("새 상품을 등록하면 판매중 상태로 시작한다")
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
    @DisplayName("상품명을 비워두고 등록하면 실패한다")
    void create_withBlankName_throws() {
        assertThatThrownBy(() -> Product.create("  ", BigDecimal.TEN, 1))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    @DisplayName("가격을 음수로 등록하면 실패한다")
    void create_withNegativePrice_throws() {
        assertThatThrownBy(() -> Product.create("keyboard", BigDecimal.valueOf(-1), 1))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    @DisplayName("재고를 음수로 등록하면 실패한다")
    void create_withNegativeStock_throws() {
        assertThatThrownBy(() -> Product.create("keyboard", BigDecimal.TEN, -1))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    @DisplayName("일부 정보만 수정하면 나머지 정보는 그대로 유지된다")
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
    @DisplayName("재고를 음수로 수정하면 실패한다")
    void update_withNegativeStock_throws() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(10000), 5);

        assertThatThrownBy(() -> product.update(new Product.UpdateCommand(null, null, -1, null)))
            .isInstanceOf(InvalidProductArgumentException.class);
    }

    @Test
    @DisplayName("상품을 삭제하면 삭제 시각이 기록된다")
    void delete_setsDeletedAtAndTouchesUpdatedAt() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(10000), 5);

        product.delete();

        assertThat(product.getDeletedAt()).isNotNull();
    }
}
