package com.example.graphqlexample.product.infra;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductRepository;
import com.example.graphqlexample.product.domain.ProductSearchCondition;
import com.example.graphqlexample.product.domain.ProductStatus;
import com.example.graphqlexample.product.domain.QProduct;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductRepositoryImpl implements ProductRepository {

    private static final QProduct product = QProduct.product;

    private final ProductJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public Optional<Product> findByIdAndDeletedAtIsNull(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Product> search(ProductSearchCondition condition, int page, int size) {
        return queryFactory
            .selectFrom(product)
            .where(
                product.deletedAt.isNull(),
                statusEq(condition.status()),
                nameContains(condition.nameKeyword()),
                priceGoe(condition.minPrice()),
                priceLoe(condition.maxPrice()),
                createdAtBetween(condition.createdFrom(), condition.createdTo())
            )
            .offset((long) page * size)
            .limit(size)
            .fetch();
    }

    private BooleanExpression statusEq(ProductStatus status) {
        return status != null ? product.status.eq(status) : null;
    }

    private BooleanExpression nameContains(String keyword) {
        return (keyword != null && !keyword.isBlank()) ? product.name.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression priceGoe(BigDecimal minPrice) {
        return minPrice != null ? product.price.goe(minPrice) : null;
    }

    private BooleanExpression priceLoe(BigDecimal maxPrice) {
        return maxPrice != null ? product.price.loe(maxPrice) : null;
    }

    private BooleanExpression createdAtBetween(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return product.createdAt.between(from, to);
        }
        if (from != null) {
            return product.createdAt.goe(from);
        }
        if (to != null) {
            return product.createdAt.loe(to);
        }
        return null;
    }
}
