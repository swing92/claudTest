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
4. [취업준비 현황 관리 (Job Applications)](#3-취업준비-현황-관리-job-applications)
   1. [지원 회사 (Company)](#31-지원-회사-company)
   2. [지원 현황 (JobApplication)](#32-지원-현황-jobapplication)
   3. [전형 진행 이력 (JobApplicationEvent)](#33-전형-진행-이력-jobapplicationevent)
5. [아직 구현되지 않은 기능 / 알려진 제약사항](#4-아직-구현되지-않은-기능--알려진-제약사항)

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

## 3. 취업준비 현황 관리 (Job Applications)

### 3.1 지원 회사 (Company)

**개요**: 지원을 고려/진행하는 회사를 관리합니다.

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/companies` | 회사 생성 |
| GET | `/api/companies` | 목록 조회 (생성일 최신순) |
| GET | `/api/companies/{companyId}` | 단건 조회 |
| PUT | `/api/companies/{companyId}` | 수정 |
| DELETE | `/api/companies/{companyId}` | 삭제 |

**요청 바디** (`CompanyCreateRequest` / `CompanyUpdateRequest` — 필드 동일)

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `name` | string | Y | 공백 불가, 최대 200자 |
| `industry` | string | N | 최대 100자 |
| `url` | string | N | 최대 500자 |
| `notes` | string | N | 최대 2000자 |

**응답** (`CompanyResponse`): `id, name, industry, url, notes, createdAt, updatedAt`

**상태 코드**

| 코드 | 조건 |
|---|---|
| 201 / 200 / 204 | 생성 / 조회·수정 / 삭제 성공 |
| 400 | `name` 공백 등 검증 실패 |
| 404 | 다른 사용자의 회사이거나 존재하지 않는 id |
| 409 | 이 회사를 참조하는 지원 현황(`job_application`)이 있는 상태에서 삭제 시도 |

**비즈니스 규칙**

- 삭제 시 `jobApplicationRepository.existsByCompanyId`로 먼저 참조 여부를 확인해 409로 거부합니다.

### 3.2 지원 현황 (JobApplication)

**개요**: 특정 회사·포지션에 대한 지원 현황(전형 단계 요약)을 관리합니다.

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/job-applications` | 생성 |
| GET | `/api/job-applications?companyId=&status=` | 검색(필터+페이징) |
| GET | `/api/job-applications/{jobApplicationId}` | 단건 조회 |
| PUT | `/api/job-applications/{jobApplicationId}` | 수정 |
| DELETE | `/api/job-applications/{jobApplicationId}` | 삭제 |

**요청 바디 — 생성** (`JobApplicationCreateRequest`)

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `companyId` | number | Y | 요청자 소유의 존재하는 회사여야 함 |
| `positionTitle` | string | Y | 공백 불가, 최대 200자 |
| `applyUrl` | string | N | 최대 500자 |
| `status` | 상태 enum(아래) | N | 생략 시 `PREPARING` |
| `appliedAt` | date | N | |
| `notes` | string | N | 최대 2000자 |

**요청 바디 — 수정** (`JobApplicationUpdateRequest`): 위와 동일하나 `status`가 **필수**입니다
(현재 상태를 항상 명시적으로 지정).

`status` 값: `PREPARING`\|`APPLIED`\|`DOCUMENT_PASSED`\|`INTERVIEW`\|`FINAL_PASSED`\|`REJECTED`

**검색 쿼리 파라미터** (전부 선택)

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `companyId` | number | 회사로 필터 |
| `status` | 상태 enum | 상태로 필터 |
| `page`, `size`, `sort` | Spring 표준 페이징 파라미터 | 기본값: `page=0`, `size=20`, `sort=createdAt,DESC` |

**응답** (`JobApplicationResponse`): `id, companyId, positionTitle, applyUrl, status, appliedAt,
notes, createdAt, updatedAt`

**상태 코드**

| 코드 | 조건 |
|---|---|
| 201 / 200 / 204 | 생성 / 조회·검색·수정 / 삭제 성공 |
| 400 | `positionTitle` 공백, `status` 누락(수정 시) 등 검증 실패 |
| 404 | 지원 현황 자체가 없거나 다른 사용자 소유 / `companyId`가 없거나 다른 사용자 소유 |
| 409 | 이 지원 현황을 참조하는 전형 이력(`job_application_event`)이 있는 상태에서 삭제 시도 |

**비즈니스 규칙**

- `status`는 생성 시 생략하면 `PREPARING`으로 채워집니다.
- 상태 전이에 대한 별도 검증(예: `REJECTED`에서 다시 `INTERVIEW`로 되돌리는 것을 막는 등)은
  없습니다 — 어떤 상태로든 자유롭게 바꿀 수 있습니다.
- `companyId`는 수정 시에도 다시 요청자 소유인지 검증됩니다(계좌/카테고리를 바꿀 수 있는
  거래(Transaction)와 동일한 패턴).
- 삭제 시 `jobApplicationEventRepository.existsByJobApplicationId`로 먼저 참조 여부를
  확인해 409로 거부합니다.

### 3.3 전형 진행 이력 (JobApplicationEvent)

**개요**: 하나의 지원 현황에 대한 전형 진행 이력(서류 제출/결과, 면접, 최종 결과 등)을
시간순으로 기록합니다. 항상 특정 `jobApplicationId`에 종속된 하위 리소스로 다뤄집니다
(URL 경로에 `jobApplicationId`가 포함됨).

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/job-applications/{jobApplicationId}/events` | 이벤트 생성 |
| GET | `/api/job-applications/{jobApplicationId}/events` | 목록 조회 (이벤트 날짜 오름차순) |
| GET | `/api/job-applications/{jobApplicationId}/events/{eventId}` | 단건 조회 |
| PUT | `/api/job-applications/{jobApplicationId}/events/{eventId}` | 수정 |
| DELETE | `/api/job-applications/{jobApplicationId}/events/{eventId}` | 삭제 |

**요청 바디 — 생성** (`JobApplicationEventCreateRequest`, `jobApplicationId`는 URL 경로에서
받으므로 바디에 없음)

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `eventType` | 이벤트 타입 enum(아래) | Y | |
| `eventDate` | date | Y | |
| `result` | 결과 enum(아래) | N | 생략 시 `PENDING` |
| `memo` | string | N | 최대 2000자 |

**요청 바디 — 수정** (`JobApplicationEventUpdateRequest`): 위와 동일하나 `result`가
**필수**입니다(현재 결과를 항상 명시적으로 지정, 생략에 의한 의도치 않은 초기화 방지).

`eventType` 값: `DOCUMENT_SUBMITTED`\|`DOCUMENT_RESULT`\|`INTERVIEW`\|`FINAL_RESULT`
`result` 값: `PENDING`\|`PASS`\|`FAIL`

**응답** (`JobApplicationEventResponse`): `id, jobApplicationId, eventType, eventDate, result,
memo, createdAt, updatedAt`

**상태 코드**

| 코드 | 조건 |
|---|---|
| 201 / 200 / 204 | 생성 / 조회·목록·수정 / 삭제 성공 |
| 400 | `eventType`/`eventDate` 누락, `result` 누락(수정 시) 등 검증 실패 |
| 404 | 경로의 `jobApplicationId`가 없거나 다른 사용자 소유 / 이벤트 자체가 없거나 다른 `jobApplicationId`·사용자 소유 |

**비즈니스 규칙 — `user_id` 정합성 검증 (설계 단계에서 요구된 규칙)**

`job_application_event.user_id`는 부모 `job_application.user_id`를 그대로 복제한
비정규화 컬럼입니다(이유는 [테이블정의서](./table-definition.md)의 "job_application_event"
항목 참고). `JobApplicationEventService`는 이 정합성을 다음과 같이 보장합니다.

1. `requireOwnedJobApplication(userId, jobApplicationId)`가 **`userId`로 필터링하지 않고**
   `jobApplicationId`만으로 부모를 조회한 뒤, `parentJobApplication.getUserId().equals(userId)`를
   **명시적으로 비교**합니다. 일치하지 않으면(다른 사용자의 지원 현황이면) 404를 반환합니다
   — 조회 쿼리의 필터 조건에 묻어서 암묵적으로 걸러지는 방식이 아니라, 실제로 실행되는
   비교 코드입니다.
2. 이 검증을 통과한 **부모 엔티티의 `userId`를 그대로 복사**해서 `JobApplicationEvent`를
   생성합니다(`create()`에서 별도의 `userId` 파라미터를 새로 넣는 게 아니라
   `jobApplication.getUserId()`를 사용) — 따라서 이벤트의 `user_id`가 부모와 어긋나는 것이
   구조적으로 불가능합니다.
3. 이벤트 자체의 조회/수정/삭제는 `findByIdAndJobApplicationIdAndUserId`로 `id` +
   `jobApplicationId` + `userId` 세 조건을 모두 만족해야 하므로, URL의 `jobApplicationId`와
   실제 이벤트가 속한 `jobApplicationId`가 다르면(예: 다른 지원 건의 이벤트 id를 넣는 경우)
   404가 됩니다.

기타 규칙:

- `jobApplicationId`는 생성 이후 변경할 수 없습니다(수정 API 자체가 URL 경로로 고정되고,
  요청 바디에 필드가 없음) — 이벤트는 한 번 속한 지원 현황에 영구히 종속됩니다.
- `result`는 생성 시 생략하면 `PENDING`으로 채워집니다.

---

## 4. 아직 구현되지 않은 기능 / 알려진 제약사항

- **실제 인증/다중 사용자 미지원**: 모든 요청이 고정된 시드 사용자로 처리됩니다.
  회원가입/로그인, 사용자별 데이터 격리(다른 사용자 계정으로 로그인해서 확인하는 시나리오)는
  아직 테스트/구현되지 않았습니다. (Job Applications의 `user_id` 정합성 검증 로직 자체는
  구현되어 있지만, 실제로 서로 다른 사용자 2명이 동시에 사용하는 시나리오로는 아직
  검증되지 않았습니다.)
- **가계부 자동 연동 미구현**: `source=SYNCED` 거래를 만드는 오픈뱅킹/마이데이터 연동은
  구현되어 있지 않습니다(스키마만 준비됨).
- **파일 업로드/스토리지 없음**: 근로계약서 등 문서를 첨부하는 기능은 아직 없습니다.
- **큐/워커 없음**: 무거운 비동기 작업(식단 추천 계산 등)을 처리할 BullMQ/Redis 같은
  큐 인프라는 아직 붙어 있지 않습니다.
