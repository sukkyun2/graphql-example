package com.example.graphqlexample.product.infra;

import com.example.graphqlexample.product.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
}
