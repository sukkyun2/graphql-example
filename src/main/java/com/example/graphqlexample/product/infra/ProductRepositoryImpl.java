package com.example.graphqlexample.product.infra;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductRepository;
import com.example.graphqlexample.product.domain.ProductStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public Optional<Product> findByIdAndDeletedAtIsNull(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Product> search(ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return status != null
            ? jpaRepository.findByStatusAndDeletedAtIsNull(status, pageable)
            : jpaRepository.findByDeletedAtIsNull(pageable);
    }
}
