# 기술 설계: 주문(Order) - 상품(Product) 도메인 상호작용

> 유저스토리는 [USERSTORY.md](./USERSTORY.md) 참고.

## 0. 전체 모듈 구조

```
order/
├─ api           (GraphQL Resolver, DataLoader)
├─ application   (OrderQueryService, OrderCommandService, OrderCommand/Query, DomainEvent Listener)
├─ domain        (Order, OrderStatus enum, OrderItem, OrderRepository 인터페이스, OrderEvent)
└─ infra         (OrderRepositoryImpl(JPA), OrderEntity)

product/
├─ api           (GraphQL Resolver, DataLoader)
├─ application   (ProductQueryService, ProductCommandService, ProductCommand/Query)
├─ domain        (Product, ProductStatus enum, ProductRepository 인터페이스, ProductEvent)
└─ infra         (ProductRepositoryImpl(JPA), ProductEntity)
```

- **의존 규칙**: `order:domain`은 `product:domain`의 **인터페이스(Port)**만 참조 가능, infra끼리는 서로 참조 금지
- **GraphQL 계층 위치**: 별도 모듈로 분리하지 않고, 각 도메인 모듈 내부에 `api` 패키지를 둔다 (즉 하나의 도메인 모듈은 `api`/`application`/`domain`/`infra` 4계층으로 구성). `api`는 해당 도메인의 `application` 계층만 의존
- **서비스 구분**: 서비스는 Query와 Command로 구분한다 (ex: `ProductQueryService`, `ProductCommandService`)
- **삭제 정책**: 모든 삭제는 soft delete로 처리한다. 엔티티에 `deleted_at`(nullable) 컬럼을 두고, 삭제 시 실제 row는 유지한 채 삭제 시각만 기록한다. 조회 시 `deleted_at IS NULL` 조건 기본 적용

## 1. CRUD 요구사항

### Product

| 구분 | Query/Mutation | 설명 |
|---|---|---|
| Create | `createProduct(input: ProductCreateInput): Product` | 상품 등록, 초기 `ProductStatus = ON_SALE` |
| Read | `product(id: ID!): Product`, `products(status: ProductStatus, page): [Product]` | 단건/목록 조회, enum 필터링 |
| Update | `updateProduct(id: ID!, input: ProductUpdateInput): Product` | 가격/재고/상태 변경 |
| Delete | `deleteProduct(id: ID!): Boolean` | soft delete (`deleted_at` 컬럼에 삭제 시각 기록, 실제 row는 유지) |

### Order

| 구분 | Query/Mutation | 설명 |
|---|---|---|
| Create | `createOrder(input: OrderCreateInput): Order` | 주문 생성 → `OrderCreatedEvent` 발행 → Product 재고 차감 트리거 |
| Read | `order(id: ID!): Order`, `orders(status: OrderStatus): [Order]` | `Order.items[].product` 필드에서 Product 참조 (N+1 발생 지점) |
| Update | `updateOrderStatus(id: ID!, status: OrderStatus): Order` | 상태 전이 검증 필요 (enum 기반 상태 머신) |
| Delete | `cancelOrder(id: ID!): Order` | 취소 시 `OrderCancelledEvent` 발행 → 재고 복원 |

## 2. Enum 설계 요구사항

```graphql
enum ProductStatus {
  ON_SALE
  SOLD_OUT
  DISCONTINUED
}

enum OrderStatus {
  PENDING
  PAID
  SHIPPING
  COMPLETED
  CANCELLED
}
```

- **상태 전이 규칙 명시 필요**: 예) `PENDING → PAID → SHIPPING → COMPLETED`, `PENDING/PAID → CANCELLED`만 허용, 나머지는 예외 처리
- **DB 저장 방식**: `@Enumerated(EnumType.STRING)`으로 고정한다 (`ORDINAL` 사용 금지)

## 3. 도메인 이벤트 요구사항

### 이벤트 흐름

```
[Order 도메인]                          [Product 도메인]
createOrder()
└─ Order.create()
   └─ ApplicationEventPublisher.publish(OrderCreatedEvent)
      └─ @TransactionalEventListener(phase = AFTER_COMMIT)
         └─ ProductCommandService.decreaseStock(items)
```

- **이벤트 정의 위치**: 각 도메인 domain 모듈에 `OrderCreatedEvent`, `OrderCancelledEvent` 정의 (POJO, Spring 의존 없이)
- **발행 방식**: Spring의 `ApplicationEventPublisher` 사용 (모놀리스 내부이므로 메시지 브로커 불필요, 추후 확장 대비 인터페이스 추상화 고려)
- **트랜잭션 경계**: `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 비동기 성격으로 처리한다. 주문은 먼저 커밋되고, 재고 차감은 별도 트랜잭션에서 이후 처리된다 (eventual consistency)
  - 재고 차감 실패 시 주문 자체는 이미 커밋된 상태이므로, 보상 트랜잭션 또는 재시도로 복구해야 한다
  - 재고 부족처럼 사용자에게 즉시 알려야 하는 케이스는 가능하면 `createOrder` 처리 중 사전 검증(재고 확인)으로 걸러내고, 이벤트 리스너 단계의 실패는 별도 복구 플로우로 다룬다
- **재고 부족 시 예외 처리**: `InsufficientStockException` → GraphQL error extension code(`STOCK_INSUFFICIENT`)로 매핑
- **이벤트 재발행/실패 복구**: 이벤트 리스너 실패 시 재시도 정책(또는 outbox 패턴 도입 여부, 모놀리스 초기 단계면 생략 가능)

## 4. N+1 문제 해결 (DataLoader)

### 문제 지점

```graphql
query {
  orders {
    id
    items {
      product { id name price status }   # Order 목록 N개 × 조회 = N+1
    }
  }
}
```

### 요구사항

- **DataLoader 등록**: `ProductDataLoader` (`batchLoadFn: List<ProductId> → List<Product>`)를 GraphQL context에 등록
- **배치 조회 메서드**: `ProductQueryService.findAllByIds(List<Long> ids): List<Product>` 형태의 IN절 배치 조회 인터페이스를 `product:application`에 미리 구현해둬야 함
- **Resolver 구현 위치**: `Order.items[].product` 필드 resolver에서 개별 `productQueryService.findById()` 호출 금지 → 반드시 `dataLoader.load(productId)` 사용
- **DataLoader 생명주기**: 요청(Request)당 1개 인스턴스 (`DataLoaderRegistry`를 GraphQL Context에 request-scope로 주입)
- **캐싱 여부**: DataLoader 자체의 요청 내 캐싱(default true)만 쓸지, 요청 간 캐시(Redis 등)까지 갈지 결정

## 5. 단위 테스트 전략

### 계층별 테스트 대상

- **domain**: `Order`, `Product` 등 도메인 객체의 상태 전이/불변식 검증 (예: `OrderStatus` 전이 규칙 위반 시 예외 발생, 재고 음수 방지 등). 순수 Java 객체 테스트로 Spring 컨텍스트 불필요
- **application (QueryService/CommandService)**: Repository·이벤트 발행자를 mock으로 대체하고 서비스 로직만 검증
  - `ProductCommandService.decreaseStock()` 재고 부족 시 `InsufficientStockException` 발생 검증
  - `OrderCommandService.createOrder()` 호출 시 `OrderCreatedEvent`가 정확히 1회 발행되는지 검증
  - `ProductQueryService.findAllByIds()`가 Repository의 배치 조회 메서드를 1회만 호출하는지 검증
- **infra (Repository/Mapper)**: `@DataJpaTest` + H2로 JPA 매핑 및 커스텀 쿼리 검증, enum ↔ SDL 변환 mapper 단위 테스트

### 원칙

- **Mock 대상**: Repository 인터페이스, `ApplicationEventPublisher` — 실제 DB/트랜잭션 없이 순수 로직만 검증
- **Query/Command 분리 검증**: CommandService 테스트는 상태 변경과 이벤트 발행에, QueryService 테스트는 조회 결과와 N+1 방지(배치 호출 횟수)에 집중
- **`@DisplayName` 작성 규칙**: 모든 테스트 메서드에 `@DisplayName`을 한글로 작성한다. 변수명·엔티티명을 그대로 노출하지 않고, 유저스토리처럼 실제 도메인 상황을 서술한다
  - 나쁜 예: `상품 status가 SOLD_OUT이면 예외를 던진다`
  - 좋은 예: `품절된 상품을 주문하면 주문이 거절된다`
- **도구**: JUnit 5 + Mockito(`@ExtendWith(MockitoExtension.class)`), Spring 컨텍스트 로딩 없이 실행하여 속도 확보
- GraphQL 통합 테스트([6번](#6-graphql-통합-테스트-요구사항))는 전체 흐름의 최종 검증 용도이며, 세부 분기/예외 케이스는 최대한 단위 테스트에서 커버한다

## 6. GraphQL 통합 테스트 요구사항

### 테스트 대상 시나리오

- `createOrder` → `OrderCreatedEvent` 발행 확인 → Product 재고 감소 검증 (같은 트랜잭션 내 반영 여부 확인)
- `orders { items { product } }` 조회 시 DataLoader가 배치로 몇 번 호출됐는지 검증 (N+1 미발생 검증 — 예: `ProductRepository.findAllByIds` 호출 1회인지 assert)
- 재고 부족 시 `createOrder` 실패 → GraphQL error response의 extension code 검증
- `updateOrderStatus`의 잘못된 상태 전이(enum) 시도 시 예외 검증
- `cancelOrder` → 재고 복원(`OrderCancelledEvent`) 검증

### 테스트 도구 및 환경

- **테스트 도구**: `graphql-java-test`, `GraphQlTester`(Spring GraphQL 제공) + `@SpringBootTest` 조합 권장
- **DB**: Testcontainers 또는 H2 중 택일, 이벤트 리스너가 AFTER_COMMIT이면 H2로는 트랜잭션 커밋 시점 검증이 까다로울 수 있어 Testcontainers 권장
- **모듈 경계 검증**: ArchUnit으로 `order:domain`이 `product:infra`를 참조하지 않는지 규칙 테스트 추가
