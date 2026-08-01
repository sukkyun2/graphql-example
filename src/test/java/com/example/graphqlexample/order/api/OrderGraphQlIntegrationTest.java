package com.example.graphqlexample.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.graphqlexample.product.domain.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@AutoConfigureGraphQlTester
class OrderGraphQlIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM order_item");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM product");
    }

    @Test
    @DisplayName("주문을 생성하면 접수 상태로 조회된다")
    void createOrder_thenQueryReturnsPendingStatus() {
        String productId = createProduct("keyboard", 1000, 10);

        OrderView created = createOrder(productId, 2);
        assertThat(created.status()).isEqualTo("PENDING");

        OrderView found = getOrder(created.id());
        assertThat(found.status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("주문을 생성하면 상품 재고가 차감된다")
    void createOrder_decreasesProductStock() {
        String productId = createProduct("mouse", 1000, 10);

        createOrder(productId, 3);

        assertThat(getProductStock(productId)).isEqualTo(7);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 생성되지 않고 재고 부족 안내를 받는다")
    void createOrder_withInsufficientStock_returnsStockInsufficientError() {
        String productId = createProduct("monitor", 1000, 1);

        graphQlTester.document("""
                mutation($input: OrderCreateInput!) {
                  createOrder(input: $input) { id }
                }
                """)
            .variable("input", Map.of("items", List.of(Map.of("productId", productId, "quantity", 5))))
            .execute()
            .errors()
            .expect(error -> "STOCK_INSUFFICIENT".equals(error.getExtensions().get("code")))
            .verify();

        assertThat(getProductStock(productId)).isEqualTo(1);
    }

    @Test
    @DisplayName("주문을 조회하면 담긴 상품 정보를 함께 확인할 수 있다")
    void getOrder_includesProductInfo() {
        String productId = createProduct("keyboard", 1000, 10);
        OrderView created = createOrder(productId, 2);

        OrderView found = getOrder(created.id());

        assertThat(found.items()).hasSize(1);
        assertThat(found.items().get(0).product().name()).isEqualTo("keyboard");
    }

    @Test
    @DisplayName("여러 주문의 상품 정보를 함께 조회해도 상품 조회는 배치로 한 번만 일어난다")
    void getOrders_batchesProductLookupAcrossOrders() {
        String productId1 = createProduct("keyboard", 1000, 10);
        String productId2 = createProduct("mouse", 2000, 10);
        createOrder(productId1, 1);
        createOrder(productId2, 1);

        clearInvocations(productRepository);

        graphQlTester.document("""
                query {
                  orders(status: PENDING) {
                    id
                    items { productId quantity unitPrice product { id name stock } }
                  }
                }
                """)
            .execute()
            .path("orders")
            .entityList(OrderView.class)
            .get();

        verify(productRepository, times(1)).findAllByIdInAndDeletedAtIsNull(anyList());
    }

    @Test
    @DisplayName("잘못된 순서로 주문 상태를 변경하면 거부된다")
    void updateOrderStatus_withInvalidTransition_returnsErrorCode() {
        String productId = createProduct("keyboard", 1000, 10);
        OrderView order = createOrder(productId, 1);

        graphQlTester.document("""
                mutation($id: ID!, $status: OrderStatus!) {
                  updateOrderStatus(id: $id, status: $status) { id }
                }
                """)
            .variable("id", order.id())
            .variable("status", "SHIPPING")
            .execute()
            .errors()
            .expect(error -> "INVALID_ORDER_STATUS_TRANSITION".equals(error.getExtensions().get("code")))
            .verify();
    }

    @Test
    @DisplayName("주문을 취소하면 상품 재고가 복원된다")
    void cancelOrder_restoresProductStock() {
        String productId = createProduct("keyboard", 1000, 10);
        OrderView order = createOrder(productId, 3);
        assertThat(getProductStock(productId)).isEqualTo(7);

        cancelOrder(order.id());

        assertThat(getProductStock(productId)).isEqualTo(10);
    }

    @Test
    @DisplayName("배송이 시작된 주문은 취소할 수 없다")
    void cancelOrder_whenShipping_returnsErrorCode() {
        String productId = createProduct("keyboard", 1000, 10);
        OrderView order = createOrder(productId, 1);
        updateOrderStatus(order.id(), "PAID");
        updateOrderStatus(order.id(), "SHIPPING");

        graphQlTester.document("""
                mutation($id: ID!) {
                  cancelOrder(id: $id) { id }
                }
                """)
            .variable("id", order.id())
            .execute()
            .errors()
            .expect(error -> "INVALID_ORDER_STATUS_TRANSITION".equals(error.getExtensions().get("code")))
            .verify();
    }

    private String createProduct(String name, int price, int stock) {
        graphQlTester.document("""
                mutation($name: String!, $price: BigDecimal!, $stock: Int!) {
                  createProduct(input: { name: $name, price: $price, stock: $stock })
                }
                """)
            .variable("name", name)
            .variable("price", price)
            .variable("stock", stock)
            .execute()
            .path("createProduct")
            .entity(Boolean.class)
            .get();

        return graphQlTester.document("""
                query($keyword: String!) {
                  products(nameKeyword: $keyword) { id }
                }
                """)
            .variable("keyword", name)
            .execute()
            .path("products[0].id")
            .entity(String.class)
            .get();
    }

    private OrderView createOrder(String productId, int quantity) {
        return graphQlTester.document("""
                mutation($input: OrderCreateInput!) {
                  createOrder(input: $input) {
                    id
                    status
                    items { productId quantity unitPrice }
                  }
                }
                """)
            .variable("input", Map.of("items", List.of(Map.of("productId", productId, "quantity", quantity))))
            .execute()
            .path("createOrder")
            .entity(OrderView.class)
            .get();
    }

    private OrderView getOrder(String id) {
        return graphQlTester.document("""
                query($id: ID!) {
                  order(id: $id) {
                    id
                    status
                    items { productId quantity unitPrice product { id name stock } }
                  }
                }
                """)
            .variable("id", id)
            .execute()
            .path("order")
            .entity(OrderView.class)
            .get();
    }

    private void updateOrderStatus(String id, String status) {
        graphQlTester.document("""
                mutation($id: ID!, $status: OrderStatus!) {
                  updateOrderStatus(id: $id, status: $status) { id }
                }
                """)
            .variable("id", id)
            .variable("status", status)
            .execute();
    }

    private void cancelOrder(String id) {
        graphQlTester.document("""
                mutation($id: ID!) {
                  cancelOrder(id: $id) { id }
                }
                """)
            .variable("id", id)
            .execute();
    }

    private int getProductStock(String id) {
        return graphQlTester.document("""
                query($id: ID!) {
                  product(id: $id) { stock }
                }
                """)
            .variable("id", id)
            .execute()
            .path("product.stock")
            .entity(Integer.class)
            .get();
    }

    private record ProductView(String id, String name, int stock) {}

    private record OrderItemView(String productId, int quantity, BigDecimal unitPrice, ProductView product) {}

    private record OrderView(String id, String status, List<OrderItemView> items) {}
}
