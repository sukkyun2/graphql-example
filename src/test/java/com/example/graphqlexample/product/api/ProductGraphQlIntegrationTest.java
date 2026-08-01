package com.example.graphqlexample.product.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureGraphQlTester
@Transactional
class ProductGraphQlIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    @DisplayName("상품을 등록하면 상품 목록 조회에 반영된다")
    void createProduct_thenProductsQueryReflectsIt() {
        createProduct("keyboard", 10000, 5);

        List<ProductView> products = fetchProductsOnSale();

        assertThat(products).hasSize(1);
        ProductView product = products.get(0);
        assertThat(product.name()).isEqualTo("keyboard");
        assertThat(product.price()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(product.stock()).isEqualTo(5);
        assertThat(product.status()).isEqualTo("ON_SALE");
    }

    @Test
    @DisplayName("상품 정보를 일부만 수정하면 나머지는 그대로 유지된다")
    void updateProduct_partiallyUpdatesOnlyGivenFields() {
        createProduct("mouse", 5000, 3);
        String id = fetchProductsOnSale().get(0).id();

        Boolean updated = graphQlTester.document("""
                mutation($id: ID!) {
                  updateProduct(id: $id, input: { price: 6000 })
                }
                """)
            .variable("id", id)
            .execute()
            .path("updateProduct")
            .entity(Boolean.class)
            .get();

        assertThat(updated).isTrue();

        ProductView product = fetchProductsOnSale().get(0);
        assertThat(product.name()).isEqualTo("mouse");
        assertThat(product.price()).isEqualByComparingTo(BigDecimal.valueOf(6000));
        assertThat(product.stock()).isEqualTo(3);
    }

    @Test
    @DisplayName("상품을 삭제하면 이후 조회 시 찾을 수 없다")
    void deleteProduct_thenSubsequentQueryReturnsNotFound() {
        createProduct("monitor", 20000, 2);
        String id = fetchProductsOnSale().get(0).id();

        Boolean deleted = graphQlTester.document("""
                mutation($id: ID!) {
                  deleteProduct(id: $id)
                }
                """)
            .variable("id", id)
            .execute()
            .path("deleteProduct")
            .entity(Boolean.class)
            .get();

        assertThat(deleted).isTrue();

        graphQlTester.document("""
                query($id: ID!) {
                  product(id: $id) { id }
                }
                """)
            .variable("id", id)
            .execute()
            .errors()
            .expect(error -> "PRODUCT_NOT_FOUND".equals(error.getExtensions().get("code")))
            .verify();
    }

    @Test
    @DisplayName("상품명 없이 등록을 시도하면 검증 오류가 발생한다")
    void createProduct_withBlankName_returnsValidationErrorCode() {
        graphQlTester.document("""
                mutation {
                  createProduct(input: { name: "", price: 1000, stock: 1 })
                }
                """)
            .execute()
            .errors()
            .expect(error -> "INVALID_PRODUCT_INPUT".equals(error.getExtensions().get("code")))
            .verify();
    }

    private void createProduct(String name, int price, int stock) {
        Boolean created = graphQlTester.document("""
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

        assertThat(created).isTrue();
    }

    private List<ProductView> fetchProductsOnSale() {
        return graphQlTester.document("""
                query {
                  products(status: ON_SALE) {
                    id
                    name
                    price
                    stock
                    status
                  }
                }
                """)
            .execute()
            .path("products")
            .entityList(ProductView.class)
            .get();
    }

    private record ProductView(String id, String name, BigDecimal price, int stock, String status) {}
}
