package com.example.graphqlexample.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

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

    @Test
    void findByStatusAndDeletedAtIsNull_filtersByStatusAndExcludesSoftDeleted() {
        repository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 1));

        Product soldOut = Product.create("mouse", BigDecimal.valueOf(2000), 1);
        soldOut.update(new Product.UpdateCommand(null, null, null, ProductStatus.SOLD_OUT));
        repository.save(soldOut);

        Product deleted = repository.save(Product.create("monitor", BigDecimal.valueOf(3000), 1));
        deleted.delete();
        repository.save(deleted);

        var result = repository.findByStatusAndDeletedAtIsNull(ProductStatus.ON_SALE, PageRequest.of(0, 20));

        assertThat(result).extracting(Product::getName).containsExactly("keyboard");
    }

    @Test
    void findByDeletedAtIsNull_paginates() {
        for (int i = 0; i < 3; i++) {
            repository.save(Product.create("product-" + i, BigDecimal.valueOf(1000), 1));
        }

        var firstPage = repository.findByDeletedAtIsNull(PageRequest.of(0, 2));

        assertThat(firstPage).hasSize(2);
    }
}
