package com.example.graphqlexample.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.graphqlexample.product.domain.Product;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ProductJpaRepositoryTest {

    @Autowired
    private ProductJpaRepository repository;

    @Test
    void findByIdAndDeletedAtIsNull_returnsExisting() {
        Product product = repository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 1));

        Optional<Product> found = repository.findByIdAndDeletedAtIsNull(product.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("keyboard");
    }

    @Test
    void findByIdAndDeletedAtIsNull_excludesSoftDeleted() {
        Product product = repository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 1));
        product.delete();
        repository.save(product);

        assertThat(repository.findByIdAndDeletedAtIsNull(product.getId())).isEmpty();
    }
}
