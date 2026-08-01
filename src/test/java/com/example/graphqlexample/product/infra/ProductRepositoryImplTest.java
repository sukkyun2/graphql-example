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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
class ProductRepositoryImplTest {

    @Autowired
    private ProductJpaRepository jpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private ProductRepository repository() {
        return new ProductRepositoryImpl(jpaRepository, new JPAQueryFactory(entityManager), entityManager);
    }

    @Test
    @DisplayName("상품명 키워드로 검색하면 대소문자를 구분하지 않고 조회된다")
    void search_filtersByNameKeywordCaseInsensitive() {
        jpaRepository.save(Product.create("Mechanical Keyboard", BigDecimal.valueOf(1000), 1));
        jpaRepository.save(Product.create("mouse", BigDecimal.valueOf(2000), 1));

        var condition = new ProductSearchCondition(null, "keyboard", null, null, null, null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("Mechanical Keyboard");
    }

    @Test
    @DisplayName("가격 범위로 검색하면 해당 범위의 상품만 조회된다")
    void search_filtersByPriceRange() {
        jpaRepository.save(Product.create("cheap", BigDecimal.valueOf(1000), 1));
        jpaRepository.save(Product.create("mid", BigDecimal.valueOf(5000), 1));
        jpaRepository.save(Product.create("expensive", BigDecimal.valueOf(10000), 1));

        var condition = new ProductSearchCondition(null, null, BigDecimal.valueOf(2000), BigDecimal.valueOf(9000), null, null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("mid");
    }

    @Test
    @DisplayName("등록일 범위로 검색하면 해당 기간에 등록된 상품만 조회된다")
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
    @DisplayName("판매 상태와 다른 조건을 함께 검색하면 모든 조건을 만족하는 상품만 조회된다")
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
    @DisplayName("조건 없이 검색해도 삭제된 상품은 제외된다")
    void search_withNoConditions_excludesSoftDeleted() {
        jpaRepository.save(Product.create("visible", BigDecimal.valueOf(1000), 1));

        Product deleted = jpaRepository.save(Product.create("deleted", BigDecimal.valueOf(1000), 1));
        deleted.delete();
        jpaRepository.save(deleted);

        var condition = new ProductSearchCondition(null, null, null, null, null, null);
        var result = repository().search(condition, 0, 20);

        assertThat(result).extracting(Product::getName).containsExactly("visible");
    }

    @Test
    @DisplayName("재고가 충분하면 조건부 차감이 성공하고 재고가 줄어든다")
    void decreaseStockIfSufficient_withEnoughStock_succeeds() {
        Product product = jpaRepository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 5));

        long updated = repository().decreaseStockIfSufficient(product.getId(), 3);

        assertThat(updated).isEqualTo(1);
        assertThat(jpaRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("재고가 부족하면 조건부 차감이 적용되지 않고 재고가 그대로 유지된다")
    void decreaseStockIfSufficient_withInsufficientStock_doesNotApply() {
        Product product = jpaRepository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 2));

        long updated = repository().decreaseStockIfSufficient(product.getId(), 3);

        assertThat(updated).isEqualTo(0);
        assertThat(jpaRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("재고를 복원하면 지정한 수량만큼 늘어난다")
    void increaseStock_addsQuantity() {
        Product product = jpaRepository.save(Product.create("keyboard", BigDecimal.valueOf(1000), 2));

        repository().increaseStock(product.getId(), 3);

        assertThat(jpaRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("삭제되지 않은 상품만 ID 목록으로 배치 조회된다")
    void findAllByIdInAndDeletedAtIsNull_excludesSoftDeleted() {
        Product visible = jpaRepository.save(Product.create("visible", BigDecimal.valueOf(1000), 1));
        Product deleted = jpaRepository.save(Product.create("deleted", BigDecimal.valueOf(1000), 1));
        deleted.delete();
        jpaRepository.save(deleted);

        var result = repository().findAllByIdInAndDeletedAtIsNull(List.of(visible.getId(), deleted.getId()));

        assertThat(result).extracting(Product::getId).containsExactly(visible.getId());
    }

    @Test
    @DisplayName("동시에 여러 요청이 같은 상품의 재고를 차감해도 재고가 음수로 내려가지 않는다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void decreaseStockIfSufficient_underConcurrency_neverOversells() throws InterruptedException {
        Product product = jpaRepository.save(Product.create("limited", BigDecimal.valueOf(1000), 5));
        Long productId = product.getId();

        int threadCount = 10;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Boolean succeeded = transactionTemplate.execute(status ->
                        repository().decreaseStockIfSufficient(productId, 1) > 0);
                    if (Boolean.TRUE.equals(succeeded)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        try {
            assertThat(successCount.get()).isEqualTo(5);
            assertThat(jpaRepository.findById(productId).orElseThrow().getStock()).isZero();
        } finally {
            // NOT_SUPPORTED means this test's writes commit for real and won't be rolled
            // back like every other @DataJpaTest method, so clean up explicitly to avoid
            // leaking "limited" into sibling tests sharing this class's H2 context.
            transactionTemplate.executeWithoutResult(status -> jpaRepository.deleteById(productId));
        }
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
