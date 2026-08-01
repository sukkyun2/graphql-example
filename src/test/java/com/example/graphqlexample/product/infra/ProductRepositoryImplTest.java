package com.example.graphqlexample.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductRepository;
import com.example.graphqlexample.product.domain.ProductSearchCondition;
import com.example.graphqlexample.product.domain.ProductStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ProductRepositoryImplTest {

    @Autowired
    private ProductJpaRepository jpaRepository;

    @Autowired
    private EntityManager entityManager;

    private ProductRepository repository() {
        return new ProductRepositoryImpl(jpaRepository, new JPAQueryFactory(entityManager));
    }

    @Test
    void search_filtersByNameKeywordCaseInsensitive() {
        jpaRepository.save(Product.create("Mechanical Keyboard", BigDecimal.valueOf(1000), 1));
        jpaRepository.save(Product.create("mouse", BigDecimal.valueOf(2000), 1));

        var condition = new ProductSearchCondition(null, "keyboard", null, null, null, null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("Mechanical Keyboard");
    }

    @Test
    void search_filtersByPriceRange() {
        jpaRepository.save(Product.create("cheap", BigDecimal.valueOf(1000), 1));
        jpaRepository.save(Product.create("mid", BigDecimal.valueOf(5000), 1));
        jpaRepository.save(Product.create("expensive", BigDecimal.valueOf(10000), 1));

        var condition = new ProductSearchCondition(null, null, BigDecimal.valueOf(2000), BigDecimal.valueOf(9000), null, null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("mid");
    }

    @Test
    void search_filtersByCreatedAtRange() {
        Product old = jpaRepository.save(Product.create("old", BigDecimal.valueOf(1000), 1));
        setCreatedAt(old, LocalDateTime.now().minusDays(10));
        jpaRepository.save(old);

        Product recent = jpaRepository.save(Product.create("recent", BigDecimal.valueOf(1000), 1));

        var condition = new ProductSearchCondition(null, null, null, null, LocalDateTime.now().minusDays(1), null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("recent");
    }

    @Test
    void search_combinesStatusWithOtherFilters() {
        Product onSale = Product.create("keyboard-onsale", BigDecimal.valueOf(1000), 1);
        jpaRepository.save(onSale);

        Product soldOut = Product.create("keyboard-soldout", BigDecimal.valueOf(1000), 1);
        soldOut.update(new Product.UpdateCommand(null, null, null, ProductStatus.SOLD_OUT));
        jpaRepository.save(soldOut);

        var condition = new ProductSearchCondition(ProductStatus.ON_SALE, "keyboard", null, null, null, null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("keyboard-onsale");
    }

    @Test
    void search_withNoConditions_excludesSoftDeleted() {
        jpaRepository.save(Product.create("visible", BigDecimal.valueOf(1000), 1));

        Product deleted = jpaRepository.save(Product.create("deleted", BigDecimal.valueOf(1000), 1));
        deleted.delete();
        jpaRepository.save(deleted);

        var condition = new ProductSearchCondition(null, null, null, null, null, null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("visible");
    }

    private static void setCreatedAt(Product product, LocalDateTime createdAt) {
        try {
            var field = Product.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(product, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
