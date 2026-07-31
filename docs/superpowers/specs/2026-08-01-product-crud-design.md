# Product CRUD 설계

> 범위: `product` 모듈만 (api/application/domain/infra 4계층). `order`는 아직 코드가 없으므로 이번 설계에서 제외.
> 상위 문서: [USERSTORY.md](../../../USERSTORY.md) Epic 1, [CLAUDE.md](../../../CLAUDE.md) 1·2번 섹션

## 1. 패키지 구조

```
com.example.graphqlexample.product
├─ api
│   ├─ GetProduct                 (@QueryMapping)
│   ├─ GetProducts                (@QueryMapping)
│   ├─ CreateProduct               (@MutationMapping)
│   ├─ UpdateProduct               (@MutationMapping)
│   ├─ DeleteProduct               (@MutationMapping)
│   ├─ ProductStatusMapper         (GraphQL enum(String) ↔ domain enum 변환)
│   └─ dto: ProductCreateInput, ProductUpdateInput
├─ application
│   ├─ ProductQueryService
│   ├─ ProductCommandService
│   └─ dto: GetProductsCriteria
├─ domain
│   ├─ Product                     (rich domain 객체 + JPA @Entity)
│   ├─ ProductStatus               (enum: ON_SALE / SOLD_OUT / DISCONTINUED)
│   ├─ ProductRepository           (Port 인터페이스)
│   └─ exceptions: ProductNotFoundException, InvalidProductArgumentException
└─ infra
    ├─ ProductJpaRepository        (Spring Data JPA, 파생 쿼리 메서드)
    └─ ProductRepositoryImpl       (Port 구현체)

com.example.graphqlexample.common.graphql
└─ GraphQLExceptionResolver        (전역 예외 → GraphQL error 매핑)
```

- `domain.Product`가 JPA 엔티티를 겸함 — 별도 Entity/EntityMapper 없음
- `api`는 use case 1개당 클래스 1개. 조회는 `Get`, 예외 가능(`getProduct`), 저장소 조회는 `find`(nullable/Optional)로 구분
- Command(`create/update/delete`)는 전부 `void`. GraphQL Mutation도 생성/수정된 리소스를 돌려주지 않고 `Boolean!`(성공 여부)만 반환

## 2. 도메인 모델 — `Product`

```java
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    private int stock;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Product(String name, BigDecimal price, int stock) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.status = ProductStatus.ON_SALE;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Product create(String name, BigDecimal price, int stock) {
        return new Product(name, price, stock);
    }

    public void update(UpdateCommand command) {
        if (command.name() != null) {
            validateName(command.name());
            this.name = command.name();
        }
        if (command.price() != null) {
            validatePrice(command.price());
            this.price = command.price();
        }
        if (command.stock() != null) {
            validateStock(command.stock());
            this.stock = command.stock();
        }
        if (command.status() != null) {
            this.status = command.status();
        }
        touch();
    }

    public void delete() { this.deletedAt = LocalDateTime.now(); touch(); }

    private void touch() { this.updatedAt = LocalDateTime.now(); }

    private static void validateName(String name) {
        if (name == null || name.isBlank())
            throw new InvalidProductArgumentException("상품명은 비어있을 수 없습니다");
    }
    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new InvalidProductArgumentException("가격은 0 이상이어야 합니다");
    }
    private static void validateStock(Integer stock) {
        if (stock < 0)
            throw new InvalidProductArgumentException("재고는 0 이상이어야 합니다");
    }

    public record UpdateCommand(String name, BigDecimal price, Integer stock, ProductStatus status) {}
}
```

- 불변식은 도메인 메서드 내부에서 직접 검증 (Bean Validation 미사용)
- `createdAt`/`updatedAt`은 생성자·`touch()`에서 수동 관리 (JPA Auditing 미사용)
- `update()`는 `UpdateCommand`의 null이 아닌 필드만 반영하는 부분 업데이트

## 3. GraphQL 스키마 (`product.graphqls`)

```graphql
type Product {
  id: ID!
  name: String!
  price: BigDecimal!
  stock: Int!
  status: ProductStatus!
  createdAt: LocalDateTime!
  updatedAt: LocalDateTime!
}

enum ProductStatus {
  ON_SALE
  SOLD_OUT
  DISCONTINUED
}

input ProductCreateInput {
  name: String!
  price: BigDecimal!
  stock: Int!
}

input ProductUpdateInput {
  name: String
  price: BigDecimal
  stock: Int
  status: ProductStatus
}

type Query {
  product(id: ID!): Product
  products(status: ProductStatus, page: Int = 0, size: Int = 20): [Product!]!
}

type Mutation {
  createProduct(input: ProductCreateInput!): Boolean!
  updateProduct(id: ID!, input: ProductUpdateInput!): Boolean!
  deleteProduct(id: ID!): Boolean!
}
```

`BigDecimal`/`LocalDateTime` 스칼라는 `graphql-java-extended-scalars` 의존성 추가 후 `RuntimeWiringConfigurer` 빈으로 등록한다.

## 4. api 계층

```java
@Controller
class GetProduct {
    @QueryMapping
    Product product(@Argument Long id) { return productQueryService.getProduct(id); }
}

@Controller
class GetProducts {
    @QueryMapping
    List<Product> products(@Argument String status, @Argument int page, @Argument int size) {
        var criteria = new GetProductsCriteria(productStatusMapper.toDomain(status), page, size);
        return productQueryService.getProducts(criteria);
    }
}

@Controller
class CreateProduct {
    @MutationMapping
    boolean createProduct(@Argument ProductCreateInput input) {
        productCommandService.createProduct(input.name(), input.price(), input.stock());
        return true;
    }
}

@Controller
class UpdateProduct {
    @MutationMapping
    boolean updateProduct(@Argument Long id, @Argument ProductUpdateInput input) {
        var command = new Product.UpdateCommand(
            input.name(), input.price(), input.stock(),
            productStatusMapper.toDomain(input.status()));
        productCommandService.updateProduct(id, command);
        return true;
    }
}

@Controller
class DeleteProduct {
    @MutationMapping
    boolean deleteProduct(@Argument Long id) {
        productCommandService.deleteProduct(id);
        return true;
    }
}
```

```java
public record ProductCreateInput(String name, BigDecimal price, int stock) {}
public record ProductUpdateInput(String name, BigDecimal price, Integer stock, String status) {}
```

- `status`는 String으로 받아 `ProductStatusMapper.toDomain(String)`이 domain enum으로 변환 (null 입력 시 null 반환, null-safe)
- domain을 GraphQL 스키마 표현으로부터 명시적으로 분리하기 위해 mapper를 항상 경유

## 5. application 계층

```java
@Service
@Transactional(readOnly = true)
class ProductQueryService {
    Product getProduct(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
    List<Product> getProducts(GetProductsCriteria criteria) {
        return productRepository.search(criteria.status(), criteria.page(), criteria.size());
    }
}

@Service
@Transactional
class ProductCommandService {
    void createProduct(String name, BigDecimal price, int stock) {
        productRepository.save(Product.create(name, price, stock));
    }
    void updateProduct(Long id, Product.UpdateCommand command) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        product.update(command);
        // save() 불필요: 트랜잭션 내 영속 엔티티는 dirty checking으로 자동 반영
    }
    void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        product.delete();
    }
}

record GetProductsCriteria(ProductStatus status, int page, int size) {}
```

## 6. infra 계층

```java
interface ProductJpaRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
    List<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);
    List<Product> findByDeletedAtIsNull(Pageable pageable);
}

@Repository
@RequiredArgsConstructor
class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository jpaRepository;

    public Product save(Product product) { return jpaRepository.save(product); }

    public Optional<Product> findByIdAndDeletedAtIsNull(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    public List<Product> search(ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return status != null
            ? jpaRepository.findByStatusAndDeletedAtIsNull(status, pageable)
            : jpaRepository.findByDeletedAtIsNull(pageable);
    }
}
```

```java
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
    List<Product> search(ProductStatus status, int page, int size);
}
```

## 7. 에러 핸들링

```java
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) { super("상품을 찾을 수 없습니다: id=" + id); }
}
public class InvalidProductArgumentException extends RuntimeException {
    public InvalidProductArgumentException(String message) { super(message); }
}
```

```java
@Component
class GraphQLExceptionResolver implements DataFetcherExceptionResolver {
    public Mono<List<GraphQLError>> resolveException(Throwable ex, DataFetchingEnvironment env) {
        GraphQLError error = switch (ex) {
            case ProductNotFoundException e -> build(e, env, ErrorType.NOT_FOUND, "PRODUCT_NOT_FOUND");
            case InvalidProductArgumentException e -> build(e, env, ErrorType.BAD_REQUEST, "INVALID_PRODUCT_INPUT");
            default -> null; // 나머지는 Spring GraphQL 기본 INTERNAL_ERROR 처리
        };
        return Mono.justOrEmpty(error).map(List::of);
    }
    private GraphQLError build(Exception e, DataFetchingEnvironment env, ErrorType type, String code) {
        return GraphqlErrorBuilder.newError(env).errorType(type).message(e.getMessage())
            .extensions(Map.of("code", code)).build();
    }
}
```

`common/graphql/` 패키지에 위치 (Product 전용이 아닌 전역 컴포넌트). REST의 `@ControllerAdvice`+`@ExceptionHandler`에 대응하는 GraphQL 쪽 메커니즘 — GraphQL 응답은 HTTP status 대신 body의 `errors` 배열로 에러를 전달하기 때문에 별도 인터페이스(`DataFetcherExceptionResolver`)를 사용한다.

## 8. 테스트 전략

- **domain**: `Product.create()` 검증 실패 케이스(공백 이름 / 음수 가격 / 음수 재고), `update()` 부분 업데이트 동작, `delete()`의 `deletedAt`/`updatedAt` 반영
- **application**: `ProductRepository`를 mock으로 대체해 `ProductQueryService`/`ProductCommandService` 단위 테스트 (not-found 시 예외, 정상 흐름의 repository 호출 검증)
- **infra**: `@DataJpaTest` — soft delete된 상품이 각 조회 메서드에서 제외되는지, 페이지네이션 동작
- **GraphQL 통합**: `GraphQlTester` + `@SpringBootTest` — CUD → `true` 응답 후 `product`/`products`로 재조회해 반영 확인, 없는 id 조회 시 `PRODUCT_NOT_FOUND`, 잘못된 입력 시 `INVALID_PRODUCT_INPUT` 코드 검증

## 9. 추가 의존성 (`build.gradle.kts`)

- `org.springframework.boot:spring-boot-starter-graphql`
- `com.graphql-java:graphql-java-extended-scalars` (BigDecimal/LocalDateTime 스칼라)
- `org.springframework.graphql:spring-graphql-test` (테스트)
