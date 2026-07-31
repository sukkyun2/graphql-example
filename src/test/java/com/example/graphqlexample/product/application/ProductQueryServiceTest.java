package com.example.graphqlexample.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductNotFoundException;
import com.example.graphqlexample.product.domain.ProductRepository;
import com.example.graphqlexample.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductQueryServiceTest {

    private ProductRepository productRepository;
    private ProductQueryService productQueryService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productQueryService = new ProductQueryService(productRepository);
    }

    @Test
    void getProduct_whenFound_returnsProduct() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        assertThat(productQueryService.getProduct(1L)).isEqualTo(product);
    }

    @Test
    void getProduct_whenNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productQueryService.getProduct(1L))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProducts_delegatesToRepositorySearch() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        when(productRepository.search(ProductStatus.ON_SALE, 0, 20)).thenReturn(List.of(product));

        var result = productQueryService.getProducts(new GetProductsCriteria(ProductStatus.ON_SALE, 0, 20));

        assertThat(result).containsExactly(product);
    }
}
