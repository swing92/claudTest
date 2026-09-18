# 기능 정의서

lifehub-api에 실제로 구현되어 있는 기능의 API 명세와 비즈니스 규칙입니다. 컨트롤러/서비스/DTO
코드(`src/main/java/com/lifehub/**`)를 기준으로 작성되었습니다.

> 새 기능이 추가되거나 기존 API가 바뀌면 이 문서도 함께 갱신합니다.

## 목차

1. [공통 사항](#공통-사항)
2. [가계부/재정 관리 (Finance)](#1-가계부재정-관리-finance)
   1. [계좌 (Account)](#11-계좌-account)
   2. [카테고리 (TransactionCategory)](#12-카테고리-transactioncategory)
   3. [거래 및 통계 (Transaction)](#13-거래-및-통계-transaction)
3. [시간/할 일 관리 (Task)](#2-시간할-일-관리-task)
4. [아직 구현되지 않은 기능 / 알려진 제약사항](#3-아직-구현되지-않은-기능--알려진-제약사항)

---

## 공통 사항

### 인증

모든 API는 `SeedUserAuthenticationFilter`에 의해 항상 고정된 시드 사용자로 인증된
상태로 처리됩니다. 별도의 로그인/토큰이 필요 없고, 클라이언트가 사용자를 지정할 방법도
없습니다 — 모든 데이터는 시드 사용자(`user_id = 1`) 소유로 생성/조회됩니다.

### 공통 에러 응답

`GlobalExceptionHandler`가 아래와 같이 처리합니다.

| 예외 | HTTP 상태 | 바디 |
|---|---|---|
| `ResourceNotFoundException` | 404 | `{timestamp, status, error: "NOT_FOUND", message}` |
| `IllegalArgumentException` | 400 | `{timestamp, status, error: "BAD_REQUEST", message}` |
| `ConflictException` | 409 | `{timestamp, status, error: "CONFLICT", message}` |
| `MethodArgumentNotValidException` (Bean Validation 실패) | 400 | `{timestamp, status, error: "VALIDATION_FAILED", messages: ["필드: 메시지", ...]}` |

### 목록/페이징 응답

`Transaction`, `Task` 검색 API는 Spring Data `Page<T>`를 그대로 JSON으로 직렬화해서
반환합니다. 주요 필드: `content`(데이터 배열), `totalElements`, `totalPages`, `number`
(0-base 페이지 번호), `size`, `sort`, `first`, `last`, `empty`.

---

## 1. 가계부/재정 관리 (Finance)

### 1.1 계좌 (Account)

**개요**: 현금/은행/카드 등 거래가 귀속되는 계좌를 관리합니다.

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/finance/accounts` | 계좌 생성 |
| GET | `/api/finance/accounts` | 계좌 목록 조회 (생성일 최신순) |
| GET | `/api/finance/accounts/{accountId}` | 단건 조회 |
| PUT | `/api/finance/accounts/{accountId}` | 전체 수정 |
| DELETE | `/api/finance/accounts/{accountId}` | 삭제 |

**요청 바디** (`AccountCreateRequest` / `AccountUpdateRequest` — 필드 동일)

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `name` | string | Y | 공백 불가, 최대 100자 |
| `type` | `CASH`\|`BANK`\|`CARD` | Y | |
| `currency` | string | N | 3자여야 함(주어질 경우). 없거나 공백이면 `"KRW"`로 대체 |

**응답** (`AccountResponse`): `id, name, type, currency, createdAt, updatedAt`

**상태 코드**

| 코드 | 조건 |
|---|---|
| 201 | 생성 성공 |
| 200 | 조회/수정 성공 |
| 204 | 삭제 성공 |
| 400 | 요청 검증 실패 (`name` 공백, `currency` 길이 등) |
| 404 | 다른 사용자의 계좌 id이거나 존재하지 않는 id |
| 409 | 이 계좌를 참조하는 거래(`transaction`)가 하나라도 있는 상태에서 삭제 시도 |

**비즈니스 규칙**

- `currency`를 비워서 보내면 서버가 `"KRW"`로 채웁니다(생성/수정 동일).
- 삭제 시 `transaction.existsByAccountId`로 먼저 참조 여부를 확인하고, 있으면 409로
  거부합니다(DB FK 제약으로도 어차피 막히지만, 더 명확한 에러 메시지를 주기 위해
  애플리케이션 레벨에서 먼저 체크).

### 1.2 카테고리 (TransactionCategory)

**개요**: 거래의 수입/지출 카테고리를 관리합니다.

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/finance/categories` | 카테고리 생성 |
| GET | `/api/finance/categories?type=` | 목록 조회. `type`(`INCOME`\|`EXPENSE`) 생략 시 전체 |
| GET | `/api/finance/categories/{categoryId}` | 단건 조회 |
| PUT | `/api/finance/categories/{categoryId}` | 수정 (`type` 제외) |
| DELETE | `/api/finance/categories/{categoryId}` | 삭제 |

**요청 바디 — 생성** (`CategoryCreateRequest`)

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `name` | string | Y | 공백 불가, 최대 50자 |
| `type` | `INCOME`\|`EXPENSE` | Y | |
| `colorHex` | string | N | `#RRGGBB` 형식 정규식 검증 |
| `isDefault` | boolean | N | 생략 시 `false` |

**요청 바디 — 수정** (`CategoryUpdateRequest`): `name`, `colorHex`, `isDefault`만 받습니다.
**`type`은 수정 API 자체에 필드가 없어 절대 바뀌지 않습니다.**

**응답** (`CategoryResponse`): `id, name, type, colorHex, isDefault, createdAt, updatedAt`

**상태 코드**

| 코드 | 조건 |
|---|---|
| 201 / 200 / 204 | 생성 / 조회·수정 / 삭제 성공 |
| 400 | `name` 공백, `colorHex` 형식 오류 등 |
| 404 | 다른 사용자의 카테고리이거나 존재하지 않는 id |
| 409 | 이 카테고리를 참조하는 거래가 있는 상태에서 삭제 시도 |

**비즈니스 규칙**

- **카테고리 `type`은 생성 후 불변**입니다. 수정 API의 요청 DTO 자체에 `type` 필드가
  없어서 클라이언트가 아예 바꿀 방법이 없습니다. 이유: `transaction.type`이 카테고리의
  `type`을 그대로 복제해서 저장하고 있는데(아래 1.3 참고), 카테고리의 타입이 사후에
  바뀌면 이미 저장된 거래들과 정합성이 깨지기 때문입니다.
- 삭제 시 `transaction.existsByCategoryId`로 참조 여부를 먼저 확인해 409로 거부합니다.

### 1.3 거래 및 통계 (Transaction)

**개요**: 개별 수입/지출 거래를 기록하고, 기간/계좌/카테고리로 검색하거나 월별·카테고리별
통계를 조회합니다.

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/finance/transactions` | 거래 생성 |
| GET | `/api/finance/transactions` | 검색(필터+페이징) |
| GET | `/api/finance/transactions/summary/monthly?year=&month=` | 월별 수입/지출/순액 요약 |
| GET | `/api/finance/transactions/summary/by-category?year=&month=` | 월별 카테고리별 합계 |
| GET | `/api/finance/transactions/{transactionId}` | 단건 조회 |
| PUT | `/api/finance/transactions/{transactionId}` | 수정 |
| DELETE | `/api/finance/transactions/{transactionId}` | 삭제 |

**요청 바디 — 생성/수정** (`TransactionCreateRequest` / `TransactionUpdateRequest` — 필드 동일)

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `accountId` | number | Y | 요청자 소유의 존재하는 계좌여야 함 |
| `categoryId` | number | Y | 요청자 소유의 존재하는 카테고리여야 함 |
| `amount` | number(소수 2자리) | Y | 양수(`0` 이하 불가) |
| `type` | `INCOME`\|`EXPENSE` | Y | `categoryId`가 가리키는 카테고리의 `type`과 일치해야 함 |
| `memo` | string | N | 최대 500자 |
| `occurredAt` | date(`YYYY-MM-DD`) | Y | |

응답의 `source`는 요청으로 받지 않으며(아래 비즈니스 규칙 참고), 생성 시 서버가 항상
`MANUAL`로 채웁니다.

**검색 쿼리 파라미터** (`GET /api/finance/transactions`, 전부 선택)

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `accountId` | number | 계좌로 필터 |
| `categoryId` | number | 카테고리로 필터 |
| `type` | `INCOME`\|`EXPENSE` | 타입으로 필터 |
| `from`, `to` | date | `occurredAt` 범위 필터(둘 다 포함, inclusive) |
| `page`, `size`, `sort` | Spring 표준 페이징 파라미터 | 기본값: `page=0`, `size=20`, `sort=occurredAt,DESC` |

**응답** (`TransactionResponse`): `id, accountId, categoryId, amount, type, memo, occurredAt,
source, createdAt, updatedAt`

**월별 요약 응답** (`MonthlySummaryResponse`): `year, month, totalIncome, totalExpense, netAmount`
(`netAmount = totalIncome - totalExpense`. 해당 월에 데이터가 없는 타입은 `0`으로 채워짐)

**카테고리별 통계 응답** (`CategorySummaryResponse`): `year, month, items[]`, 각 item은
`categoryId, categoryName, type, totalAmount` (`CategorySummaryItem`). 집계 이후 카테고리가
삭제된 경우 `categoryName`은 `"(deleted category)"`로 표시됩니다.

**상태 코드**

| 코드 | 조건 |
|---|---|
| 201 / 200 / 204 | 생성 / 조회·수정·통계 / 삭제 성공 |
| 400 | `amount`가 양수가 아님, 필수값 누락 등 Bean Validation 실패, 또는 `type`이 카테고리의 `type`과 다름 |
| 404 | 거래 자체가 없거나 다른 사용자 소유 / `accountId`가 없거나 다른 사용자 소유 / `categoryId`가 없거나 다른 사용자 소유 |

**비즈니스 규칙**

- **`source`는 API로 `SYNCED`를 만들 수 없습니다.** 은행/카드 자동 연동(오픈뱅킹,
  마이데이터 등) 기능이 아직 구현되지 않았기 때문에, `Transaction` 생성자 자체가
  `source = MANUAL`, `external_id = null`만 만들도록 되어 있습니다. `source`/`external_id`
  컬럼은 스키마상으로만 존재하고, 나중에 자동 연동 기능이 별도 생성 경로로 추가될 때
  쓰일 예정입니다.
- **`type`은 반드시 카테고리의 `type`과 일치해야 합니다.** 생성/수정 시
  `TransactionService`가 `categoryId`로 카테고리를 조회해 `category.type != request.type`이면
  `IllegalArgumentException`(→ 400)을 던집니다.
- **계좌/카테고리는 요청한 사용자 소유여야 합니다.** 존재하지 않거나 다른 사용자의
  `accountId`/`categoryId`를 넘기면 404를 반환합니다(권한 없음을 403이 아니라 404로
  표현 — 다른 사용자의 리소스 존재 여부 자체를 노출하지 않기 위함).
- 월별/카테고리별 통계는 `occurred_at`이 해당 연/월(`YearMonth`의 1일~말일) 범위인
  거래만 집계합니다.

---

## 2. 시간/할 일 관리 (Task)

**개요**: 할 일(`TODO`)과 캘린더 일정(`EVENT`)을 하나의 `Task` 모델로 통합 관리합니다.

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/tasks` | 생성 |
| GET | `/api/tasks` | 검색(필터+페이징) |
| GET | `/api/tasks/{taskId}` | 단건 조회 |
| PUT | `/api/tasks/{taskId}` | 내용 수정 (완료 상태 제외) |
| PATCH | `/api/tasks/{taskId}/complete` | 완료 처리 |
| PATCH | `/api/tasks/{taskId}/incomplete` | 완료 취소(재오픈) |
| DELETE | `/api/tasks/{taskId}` | 삭제 |

**요청 바디 — 생성/수정** (`TaskCreateRequest` / `TaskUpdateRequest` — 필드 동일)

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `title` | string | Y | 공백 불가, 최대 200자 |
| `description` | string | N | 최대 2000자 |
| `type` | `TODO`\|`EVENT` | Y | |
| `dueAt` | datetime | N | |
| `startAt` | datetime | N | `endAt`과 함께 주어질 경우 `endAt >= startAt` |
| `endAt` | datetime | N | 위와 동일 |
| `isAllDay` | boolean | N | 생략 시 `false` |
| `priority` | `LOW`\|`MEDIUM`\|`HIGH` | N | 생략 시 `MEDIUM` |

수정 요청(`TaskUpdateRequest`)에는 `isCompleted`/`completedAt` 필드가 **아예 없습니다** —
완료 상태는 오직 `complete`/`incomplete` 액션으로만 바뀝니다.

**검색 쿼리 파라미터** (`GET /api/tasks`, 전부 선택)

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `type` | `TODO`\|`EVENT` | 타입으로 필터 |
| `isCompleted` | boolean | 완료 여부로 필터 |
| `dueFrom`, `dueTo` | datetime | `dueAt` 범위 필터(inclusive) |
| `page`, `size`, `sort` | Spring 표준 페이징 파라미터 | 기본값: `page=0`, `size=20`, `sort=dueAt,ASC` |

**응답** (`TaskResponse`): `id, title, description, type, dueAt, startAt, endAt, isAllDay,
isCompleted, completedAt, priority, createdAt, updatedAt`

**상태 코드**

| 코드 | 조건 |
|---|---|
| 201 | 생성 성공 |
| 200 | 조회/수정/완료/재오픈 성공 |
| 204 | 삭제 성공 |
| 400 | `title` 공백 등 Bean Validation 실패, 또는 `endAt`이 `startAt`보다 빠름 |
| 404 | 다른 사용자의 task이거나 존재하지 않는 id |

**비즈니스 규칙**

- `startAt`과 `endAt`이 둘 다 주어졌는데 `endAt`이 `startAt`보다 빠르면
  `IllegalArgumentException`(→ 400)을 던집니다. 둘 중 하나만 주어지거나 둘 다 없으면
  검증하지 않습니다.
- `isAllDay`를 생략하면 `false`, `priority`를 생략하면 `MEDIUM`으로 채워집니다(생성/수정 동일).
- `complete()`는 `isCompleted=true`, `completedAt=now()`로 설정합니다. 이미 완료된
  task에 다시 호출해도 에러 없이 `completedAt`만 현재 시각으로 갱신됩니다(멱등적으로
  동작하되 시각은 매번 갱신됨).
- `incomplete()`는 `isCompleted=false`, `completedAt=null`로 되돌립니다.
- `type`(`TODO`/`EVENT`)에 따라 `dueAt` 또는 `startAt`/`endAt` 중 무엇을 채워야 하는지에
  대한 강제 규칙은 없습니다 — 둘 다 선택 필드이며 자유롭게 조합할 수 있습니다.

---

## 3. 아직 구현되지 않은 기능 / 알려진 제약사항

- **취업준비 현황 관리(Job Applications)**: DB 스키마(`company`, `job_application`,
  `job_application_event`)만 만들어져 있고, Entity/Repository/Service/Controller는 아직
  없습니다. 구현되면 `job_application_event` 생성/수정 시 요청의 `user_id`가 부모
  `job_application.user_id`와 일치하는지 서비스 레이어에서 검증하는 로직이 반드시
  포함되어야 합니다(테이블 정의서의 "job_application_event" 항목 참고).
- **실제 인증/다중 사용자 미지원**: 모든 요청이 고정된 시드 사용자로 처리됩니다.
  회원가입/로그인, 사용자별 데이터 격리(다른 사용자 계정으로 로그인해서 확인하는 시나리오)는
  아직 테스트/구현되지 않았습니다.
- **가계부 자동 연동 미구현**: `source=SYNCED` 거래를 만드는 오픈뱅킹/마이데이터 연동은
  구현되어 있지 않습니다(스키마만 준비됨).
- **파일 업로드/스토리지 없음**: 근로계약서 등 문서를 첨부하는 기능은 아직 없습니다.
- **큐/워커 없음**: 무거운 비동기 작업(식단 추천 계산 등)을 처리할 BullMQ/Redis 같은
  큐 인프라는 아직 붙어 있지 않습니다.
- **`company`/`job_application` 삭제 가드 없음**: `account`/`transaction_category`와 달리
  아직 API 자체가 없어서, 참조 중인 하위 레코드가 있을 때 애플리케이션 레벨에서 409로
  막는 로직도 아직 없습니다(DB FK 제약으로는 여전히 막힙니다).
