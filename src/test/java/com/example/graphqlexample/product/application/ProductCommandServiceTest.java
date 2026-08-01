package com.example.graphqlexample.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductCommandServiceTest {

    private ProductRepository productRepository;
    private ProductCommandService productCommandService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productCommandService = new ProductCommandService(productRepository);
    }

    @Test
    @DisplayName("상품을 등록하면 저장소에 저장된다")
    void createProduct_savesNewProduct() {
        productCommandService.createProduct("keyboard", BigDecimal.valueOf(1000), 1);

        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("존재하는 상품을 수정하면 변경한 값만 반영된다")
    void updateProduct_whenFound_appliesPartialUpdate() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        productCommandService.updateProduct(
            1L, new Product.UpdateCommand(null, BigDecimal.valueOf(2000), null, null));

        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 수정하려 하면 실패한다")
    void updateProduct_whenNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.updateProduct(
            1L, new Product.UpdateCommand(null, null, null, null)))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 상품을 삭제하면 삭제 처리된다")
    void deleteProduct_whenFound_marksDeleted() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        productCommandService.deleteProduct(1L);

        assertThat(product.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 상품을 삭제하려 하면 실패한다")
    void deleteProduct_whenNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.deleteProduct(1L))
            .isInstanceOf(ProductNotFoundException.class);
    }
}
