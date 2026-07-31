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
