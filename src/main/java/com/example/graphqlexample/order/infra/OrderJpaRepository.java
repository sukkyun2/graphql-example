package com.example.graphqlexample.order.infra;

import com.example.graphqlexample.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<Order, Long> {
}
