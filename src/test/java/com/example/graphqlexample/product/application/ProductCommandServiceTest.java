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
    void createProduct_savesNewProduct() {
        productCommandService.createProduct("keyboard", BigDecimal.valueOf(1000), 1);

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_whenFound_appliesPartialUpdate() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        productCommandService.updateProduct(
            1L, new Product.UpdateCommand(null, BigDecimal.valueOf(2000), null, null));

        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    void updateProduct_whenNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.updateProduct(
            1L, new Product.UpdateCommand(null, null, null, null)))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteProduct_whenFound_marksDeleted() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        productCommandService.deleteProduct(1L);

        assertThat(product.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteProduct_whenNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.deleteProduct(1L))
            .isInstanceOf(ProductNotFoundException.class);
    }
}
