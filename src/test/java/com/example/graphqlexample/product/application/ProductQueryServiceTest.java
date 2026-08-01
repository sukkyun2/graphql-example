package com.example.graphqlexample.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductRepository;
import com.example.graphqlexample.product.domain.ProductSearchCondition;
import com.example.graphqlexample.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("존재하는 상품을 조회하면 해당 상품을 반환한다")
    void getProduct_whenFound_returnsProduct() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        assertThat(productQueryService.getProduct(1L)).isEqualTo(product);
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 실패한다")
    void getProduct_whenNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productQueryService.getProduct(1L))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("조건에 맞는 상품 목록을 조회한다")
    void getProducts_delegatesToRepositorySearch() {
        Product product = Product.create("keyboard", BigDecimal.valueOf(1000), 1);
        var condition = new ProductSearchCondition(ProductStatus.ON_SALE, null, null, null, null, null);
        when(productRepository.search(condition, 0, 20)).thenReturn(List.of(product));

        var criteria = new GetProductsCriteria(ProductStatus.ON_SALE, null, null, null, null, null, 0, 20);
        var result = productQueryService.getProducts(criteria);

        assertThat(result).containsExactly(product);
    }
}
