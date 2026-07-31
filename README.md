# graphql-example

주문(Order)-상품(Product) 도메인 상호작용을 통해 Spring Boot 기반 GraphQL API와 모듈러 모놀리스 설계를 연습하는 프로젝트입니다.

- 무엇을 만드는지: [USERSTORY.md](./USERSTORY.md)
- 어떻게 만드는지: [CLAUDE.md](./CLAUDE.md)

## 기술 스택

| 기술 스택 | 이 프로젝트에서 얻는 하드 스킬 |
|---|---|
| Spring GraphQL | GraphQL 스키마(SDL) 설계, Query/Mutation Resolver 구현 |
| DataLoader | N+1 쿼리 문제 인지 및 배치 로딩으로 해결하는 패턴 |
| Spring Modulith | 모듈러 모놀리스 구조 설계, 모듈 간 의존 방향 통제 |
| GraphQlTester + Testcontainers | GraphQL API 엔드투엔드 통합 테스트 |
| ArchUnit | 아키텍처 의존 규칙을 코드로 강제하는 방법 |

## 실행

```bash
./gradlew bootRun
```

## GraphQL 스키마

스키마 SDL은 [`src/main/resources/graphql/`](./src/main/resources/graphql)에 커밋되어 있습니다. 프론트엔드 개발자는 이 경로의 `.graphqls` 파일을 그대로 참고하면 됩니다 (코드 변경 시 항상 최신 상태 유지).

서버를 띄운 상태에서 병합된 스키마를 텍스트로 받고 싶다면:

```bash
curl http://localhost:8080/graphql/schema
```

> 위 엔드포인트는 `application.yaml`에 `spring.graphql.schema.printer.enabled: true` 설정이 필요합니다 (기본 비활성화).

## 테스트

```bash
# 전체 테스트
./gradlew test

# 모듈 경계 검증만 실행
./gradlew test --tests "*ArchitectureTest"
```
