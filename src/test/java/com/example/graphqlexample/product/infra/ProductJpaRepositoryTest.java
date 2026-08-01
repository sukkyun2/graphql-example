package com.example.graphqlexample.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.graphqlexample.product.domain.Product;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ProductJpaRepositoryTest {

    @Autowired
    private ProductJpaRepository repository;

    @Test
    @DisplayName("삭제되지 않은 상품을 조회하면 정상적으로 조회된다")
    void findByIdAndDeletedAtIsNull_returnsExisting() {
        Product product = repository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 1));

        Optional<Product> found = repository.findByIdAndDeletedAtIsNull(product.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("keyboard");
    }

    @Test
    @DisplayName("삭제된 상품은 조회 결과에서 제외된다")
    void findByIdAndDeletedAtIsNull_excludesSoftDeleted() {
        Product product = repository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 1));
        product.delete();
        repository.save(product);

        assertThat(repository.findByIdAndDeletedAtIsNull(product.getId())).isEmpty();
    }
}
