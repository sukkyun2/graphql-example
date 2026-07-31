package com.example.graphqlexample.product.infra;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    List<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);

    List<Product> findByDeletedAtIsNull(Pageable pageable);
}
