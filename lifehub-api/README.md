# lifehub-api

개인용 종합 관리 웹앱의 백엔드(API 서버)입니다.

## 1. 프로젝트 개요

지금은 로그인 없이 혼자 쓰는 개인용 웹앱으로 시작하지만, 이후 회원가입/로그인 기반의
다중 사용자 서비스로 확장하는 것을 전제로 설계되어 있습니다. 모든 테이블에 `user_id`를
두고, 인증 방식은 나중에 통째로 교체 가능한 구조로 분리해뒀습니다 (자세한 내용은
[3. 아키텍처/설계 원칙](#3-아키텍처설계-원칙) 참고).

1차로 구현 중인 핵심 기능은 다음 세 가지입니다.

1. **가계부/재정 관리** — 계좌, 카테고리별 수입·지출 기록, 월별/카테고리별 통계
2. **시간/할 일 관리** — 할 일과 캘린더 일정을 하나의 모델로 통합 관리
3. **취업준비 현황 관리** — 지원 회사/포지션, 전형 진행상황(서류·면접·결과) 기록

이후 확장 예정 영역(아직 미구현):

- 근로계약/급여 관리 — 계약서 문서 업로드, 급여일자/계약내용 관리
- 식단 계획/추천 — 식사 기록 기반 추천 (별도 워커/추천 로직이 붙을 수 있음을 염두에 두고 설계)
- 사용자가 직접 추가하는 커스텀 관리 영역

프론트엔드는 **별도 저장소**(Next.js, TypeScript)로 분리되어 있습니다. 이 저장소는 백엔드
API 서버 전용입니다.

## 2. 기술 스택

| 영역 | 선택 |
|---|---|
| 언어/런타임 | Java 21 |
| 프레임워크 | Spring Boot 3.4.1 (Maven) |
| 인증/인가 | Spring Security |
| ORM | Spring Data JPA (Hibernate) |
| DB | PostgreSQL |
| 스키마 마이그레이션 | Flyway |
| API 문서 | springdoc-openapi (OpenAPI 3 / Swagger UI) |
| 기타 | Lombok, Bean Validation (`spring-boot-starter-validation`), Spring Boot Actuator |
| 프론트엔드 | Next.js + TypeScript (별도 레포) |

## 3. 아키텍처/설계 원칙

### 도메인별 패키지 분리, 서로 직접 참조하지 않음

`finance`(가계부) / `tasks`(할일·캘린더) / `jobapplications`(취업준비, 예정)는 각자
독립된 최상위 패키지이며, 서로 다른 도메인 패키지를 import하지 않습니다. 공통으로 쓰는
것은 `common`(설정/보안/예외/베이스 엔티티)과 `user` 뿐입니다. 새 관리 영역(건강, 습관 등)이
추가될 때도 새 패키지 하나를 더하는 것으로 끝나고, 기존 도메인 코드는 건드리지 않는 것이
목표입니다.

### `user_id`는 순수 컬럼으로만 존재 (JPA 연관관계 아님)

모든 도메인 엔티티(`BaseUserOwnedEntity`를 상속)는 `User`를 `@ManyToOne`으로 연관관계
맺지 않고, `userId`(`Long`) 컬럼만 가집니다. 이렇게 해두면:

- 도메인 패키지가 `user` 패키지에 대한 JPA 연관관계 의존 없이 완전히 독립적으로 남습니다.
- 나중에 도메인을 별도 서비스로 분리하더라도 연관관계를 풀 필요가 없습니다.
- 조회는 항상 `WHERE user_id = :userId` 조건 하나로 끝나, 다중 사용자 전환 시 로직이
  단순합니다.

### 시드 유저 인증 (지금은 로그인 없음, 나중에 실제 인증으로 교체 가능)

`SeedUserAuthenticationFilter`가 모든 요청에 대해 항상 고정된 시드 사용자
(`lifehub.security.seed-user-id`, 기본값 `1`)를 인증된 사용자로 SecurityContext에
주입합니다. 컨트롤러는 `@CurrentUserId Long userId` 파라미터로 현재 사용자 id를
받는데, 이 값은 `CurrentUserIdArgumentResolver`가 SecurityContext에서 꺼내줍니다.

나중에 실제 회원가입/로그인을 붙일 때는 이 필터 하나만 JWT 검증 필터 등으로 교체하면
됩니다. `@CurrentUserId`를 쓰는 컨트롤러/서비스 코드는 전혀 수정할 필요가 없습니다.

### 응답은 Entity가 아니라 DTO로 노출

모든 API는 JPA 엔티티를 직접 반환하지 않고 별도의 응답 DTO(`record`)로 변환해서
내려줍니다. 이유:

- 엔티티 필드가 바뀌어도 API 스펙(그리고 이를 기반으로 프론트가 생성하는 타입)이
  의도치 않게 깨지지 않습니다.
- 지연 로딩 프록시나 연관관계를 실수로 직렬화해버리는 문제를 원천 차단합니다.
- springdoc이 생성하는 OpenAPI 스펙이 실제 계약(contract) 역할을 하도록 명확히
  분리됩니다.

## 4. 실행 방법

### 사전 요구사항

- JDK 21
- Docker Desktop (PostgreSQL 로컬 실행용)
- (선택) Maven — 프로젝트에 [Maven Wrapper](https://maven.apache.org/wrapper/)가
  포함되어 있어 별도 설치 없이 `./mvnw`로 바로 빌드/실행할 수 있습니다.

### 1) PostgreSQL 실행 (Docker Compose)

```bash
cd lifehub-api
docker compose up -d postgres
```

기본값은 DB `lifehub`, 사용자/비밀번호 `lifehub`/`lifehub`, 포트 `5432`입니다.
필요하면 `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_PORT` 환경변수로 바꿀 수 있습니다.

### 2) 애플리케이션 빌드/실행 (Maven Wrapper)

```bash
# 빌드만
./mvnw clean compile

# 테스트 포함 빌드
./mvnw clean verify

# 실행 (기본 포트 8080)
./mvnw spring-boot:run
```

Docker Compose 기본값과 다른 DB 접속 정보를 쓰려면 환경변수로 넘기면 됩니다.

```bash
DB_HOST=localhost DB_PORT=5432 DB_NAME=lifehub DB_USERNAME=lifehub DB_PASSWORD=lifehub \
  ./mvnw spring-boot:run
```

애플리케이션이 기동될 때 **Flyway 마이그레이션이 자동으로 실행**됩니다
(`src/main/resources/db/migration/V*.sql`). 별도로 마이그레이션 명령을 따로 실행할
필요가 없습니다. `spring.jpa.hibernate.ddl-auto=validate`로 설정되어 있어, Hibernate는
스키마를 직접 바꾸지 않고 엔티티와 실제 DB 스키마가 일치하는지 검증만 합니다 —
즉 스키마 변경은 항상 새 Flyway 마이그레이션 파일을 추가하는 방식으로만 이뤄집니다.

### 3) API 문서 확인

애플리케이션 기동 후:

- OpenAPI 스펙(JSON): `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 헬스체크: `http://localhost:8080/actuator/health`

## 5. 현재 구현 상태

### 완료된 기능

- [x] 프로젝트 초기 세팅 (Spring Boot, PostgreSQL, Flyway, Docker Compose, 시드 유저 인증, springdoc)
- [x] 가계부/재정 관리
  - [x] 계좌(Account) CRUD
  - [x] 카테고리(TransactionCategory) CRUD
  - [x] 거래(Transaction) CRUD, 검색(계좌/카테고리/타입/기간 필터 + 페이징)
  - [x] 월별 요약, 카테고리별 통계
- [x] 시간/할 일 관리
  - [x] 할일·캘린더 일정(Task) CRUD, 검색(타입/완료여부/마감일 범위 필터 + 페이징)
  - [x] 완료/재오픈 처리
- [ ] 취업준비 현황 관리 (DB 스키마는 존재, API/서비스 레이어 미구현)
  - [ ] 지원 회사(Company) CRUD
  - [ ] 지원 현황(JobApplication) CRUD
  - [ ] 전형 진행 이력(JobApplicationEvent) CRUD + user_id 정합성 검증

### 아직 시작하지 않은 것

- [ ] 근로계약/급여 관리 (문서 업로드 포함) — 파일 스토리지 연동 필요
- [ ] 식단 계획/추천 — 큐/워커(BullMQ 등) 연동 필요
- [ ] 실제 회원가입/로그인 (지금은 시드 유저 고정)
- [ ] 프론트엔드 연동 (별도 Next.js 레포)

개발 중 겪은 트러블슈팅(에러 원인/해결 과정)은 [`TROUBLESHOOTING.md`](./TROUBLESHOOTING.md)에
모아두고 있습니다. 새 이슈가 생길 때마다 계속 추가됩니다.

## 6. 문서

- [`TROUBLESHOOTING.md`](./TROUBLESHOOTING.md) — 개발 중 겪은 이슈와 해결 과정
- [`docs/table-definition.md`](./docs/table-definition.md) — 테이블 정의서 (전체 스키마)
- [`docs/feature-specification.md`](./docs/feature-specification.md) — 기능 정의서 (API 명세, 비즈니스 규칙)

## 7. 프로젝트 구조

```
lifehub-api/
├── docker-compose.yml          # 로컬 PostgreSQL
├── pom.xml
├── mvnw, mvnw.cmd               # Maven Wrapper
├── TROUBLESHOOTING.md
├── docs/
│   ├── table-definition.md
│   └── feature-specification.md
└── src/main/
    ├── java/com/lifehub/
    │   ├── LifehubApplication.java
    │   ├── common/                        # 도메인 무관 공통 코드
    │   │   ├── config/                    # SecurityConfig, OpenApiConfig, JpaAuditingConfig, WebConfig
    │   │   ├── security/                  # SeedUserAuthenticationFilter, @CurrentUserId, AppUserPrincipal
    │   │   ├── entity/                    # BaseEntity, BaseUserOwnedEntity
    │   │   └── exception/                 # GlobalExceptionHandler, ResourceNotFoundException, ConflictException
    │   ├── user/                          # User (시드 유저), UserRepository
    │   ├── finance/                       # 가계부/재정 관리
    │   │   ├── entity/                    # Account, TransactionCategory, Transaction (+ enum)
    │   │   ├── repository/                # *Repository, TransactionSpecifications, *Aggregate
    │   │   ├── dto/{request,response}/
    │   │   ├── service/                   # AccountService, TransactionCategoryService, TransactionService
    │   │   └── controller/                # AccountController, TransactionCategoryController, TransactionController
    │   └── tasks/                         # 시간/할 일 관리
    │       ├── entity/                    # Task (+ TaskType, TaskPriority)
    │       ├── repository/                # TaskRepository, TaskSpecifications
    │       ├── dto/{request,response}/
    │       ├── service/                   # TaskService
    │       └── controller/                # TaskController
    └── resources/
        ├── application.yml
        └── db/migration/                  # Flyway: V1_user, V2_finance, V3_task, V4_job_application
```

> `jobapplications` 패키지는 아직 코드가 없고, DB 스키마(V4 마이그레이션)만 먼저
> 만들어져 있습니다. 구현되면 이 트리에 `finance`/`tasks`와 같은 구조로 추가됩니다.

---

이 README는 기능이 추가되거나 구조가 바뀔 때마다 "5. 현재 구현 상태"와 "7. 프로젝트 구조"를
최신 상태로 갱신합니다.
