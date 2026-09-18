# Troubleshooting Log

개발 중 실제로 겪은 문제와 해결 과정을 기록한다. 같은 실수를 반복하지 않기 위한 팀(=미래의 나) 참고용 문서다.
새 항목은 아래 형식을 따르고, 최신 항목이 위로 오도록 추가한다.

```
## <제목>

- 날짜: YYYY-MM-DD
- 증상:
- 원인:
- 해결:
- 참고:
```

---

## `src/test/resources/application.yml`이 main의 `application.yml`을 통째로 덮어써서 테스트 컨텍스트 로딩 실패

- 날짜: 2026-09-18
- 증상: 통합 테스트 도입 후 `AbstractIntegrationTest`를 상속한 모든 테스트가
  `ApplicationContext` 로딩 단계에서 예외 없이(?) 실패. 실제 원인은
  `PlaceholderResolutionException: Could not resolve placeholder
  'lifehub.security.seed-user-id'` — `SecurityConfig`가 `@Value`로 주입받는
  프로퍼티를 찾지 못해 `securityConfig` 빈 생성 자체가 실패했다.
- 원인: SQL 로그를 조용히 하려고 `src/test/resources/application.yml`을 새로 만들었는데,
  Maven은 `src/main/resources`와 `src/test/resources`에 **같은 파일명**이 있으면
  병합하지 않는다. 테스트 실행 시 클래스패스에서 `target/test-classes`가
  `target/classes`보다 먼저 오기 때문에, `logging.level`만 담긴 테스트용
  `application.yml`이 `lifehub.security.seed-user-id` 등 앱 전체 설정이 들어있는
  main `application.yml`을 완전히 가려버렸다(병합이 아니라 대체).
- 해결: 테스트 전용 설정 파일을 `src/test/resources/application-test.yml`로
  이름을 바꾸고(`lifehub-api/src/test/resources/application-test.yml`),
  `AbstractIntegrationTest`에 `@ActiveProfiles("test")`를 추가했다
  (`lifehub-api/src/test/java/com/lifehub/AbstractIntegrationTest.java`).
  이렇게 하면 Spring Boot가 main `application.yml`(항상 로드) 위에
  `application-test.yml`(프로필별 오버레이)을 "병합"해서 얹으므로, main 설정을
  가리지 않으면서 로깅 레벨만 오버라이드된다.
  - 부수적으로 한 번 더 걸렸던 함정: 파일명을 바꾼 뒤 처음 재실행했을 때도 같은
    에러가 재현됐는데, 원인은 `mvn test`가 `target/test-classes/`에 이미 복사되어
    있던 **이전(대체용) `application.yml`을 지우지 않고 그대로 둔 것**이었다(Maven
    리소스 복사는 갱신/추가만 하고 사라진 소스 파일에 대응하는 산출물은 청소하지
    않음). `mvn clean test`로 다시 빌드하고 나서야 해결됨을 확인했다.
- 참고: 테스트 전용 설정은 항상 `application-<profile>.yml` 형태로 만들고
  `@ActiveProfiles`로 활성화해야 main 설정과 "병합"된다. 같은 파일명
  (`application.yml`)을 test resources에 두면 무조건 main을 완전히 대체(가림)한다 —
  이건 Spring Boot의 프로필 오버레이 메커니즘이 아니라 Maven/클래스패스 단계에서
  일어나는 일이라, "일부 키만 더 넣고 싶었다"는 의도와 무관하게 항상 전체가 가려진다.

---

## PostgreSQL에서 `(:param IS NULL OR ...)` 패턴이 파라미터 타입을 추론하지 못해 500 발생

- 날짜: 2026-09-18
- 증상: `GET /api/finance/transactions?from=2026-01-01&to=2026-01-31`처럼 날짜 필터를 넘기면
  `500 Internal Server Error`. 필터 없이 호출하면 정상 동작해서 처음엔 원인을 특정하기 어려웠음.
- 원인: `TransactionRepository.search()`를 아래와 같은 단일 JPQL로 구현했었다.

  ```java
  @Query("""
      SELECT t FROM Transaction t
      WHERE t.userId = :userId
        AND (:accountId IS NULL OR t.accountId = :accountId)
        AND (:categoryId IS NULL OR t.categoryId = :categoryId)
        AND (:type IS NULL OR t.type = :type)
        AND (:from IS NULL OR t.occurredAt >= :from)
        AND (:to IS NULL OR t.occurredAt <= :to)
      """)
  ```

  PostgreSQL의 extended query protocol은 Parse 단계에서 각 `?` 바인드 위치의 타입을 SQL 문맥만으로
  추론한다. `x IS NULL` 비교는 `x`의 타입에 대해 아무 단서도 주지 않기 때문에, 같은 named parameter가
  다른 자리에서 타입이 분명한 비교(`t.occurred_at >= ?`)로도 쓰이더라도 `IS NULL` 자리의 플레이스홀더
  자체는 타입을 추론하지 못해 `ERROR: could not determine data type of parameter $8`로 실패한다.
  실제 서버 로그(`o.h.engine.jdbc.spi.SqlExceptionHelper`)에서 이 메시지를 확인해 원인을 특정했다.
- 해결: 해당 `@Query` 메서드를 제거하고 `JpaSpecificationExecutor` 기반으로 전환했다.
  - 신규: `TransactionSpecifications`
    (`lifehub-api/src/main/java/com/lifehub/finance/repository/TransactionSpecifications.java`) —
    값이 `null`이 아닌 필터만 `Specification`으로 만들어 반환(`null`이면 그 조건 자체를 만들지 않음).
  - `TransactionRepository`가 `JpaSpecificationExecutor<Transaction>`를 추가로 extends.
  - `TransactionService.search()`가 `Specification.where(...).and(...)`로 조건을 조합해
    `transactionRepository.findAll(spec, pageable)` 호출.
- 참고: 동적/선택적 필터가 있는 쿼리를 JPQL 문자열에 `IS NULL OR` 패턴으로 짜 넣는 건 PostgreSQL +
  Hibernate 조합에서 자주 걸리는 함정이다. 조건이 있을 때만 프레디케이트를 추가하는 Specification/
  Criteria API를 쓰면 이 클래스의 버그 자체가 발생하지 않는다(각 조건이 실제로 필요할 때만 SQL에
  등장하므로 타입 추론이 실패할 자리가 아예 없어짐).

---

## `update()` 응답의 `updatedAt`이 flush 이전 값이라 실제 DB 값과 불일치

- 날짜: 2026-09-18
- 증상: `PUT /api/finance/accounts/{id}` 등으로 리소스를 수정하면 응답 바디의 `updatedAt`이
  `createdAt`과 동일한(즉, 전혀 갱신되지 않은) 값으로 내려옴. 반면 `psql`로 DB를 직접 조회하면
  `updated_at`은 정상적으로 최신 시각으로 바뀌어 있었음 — API 응답과 DB 상태가 불일치.
- 원인: `BaseEntity`의 `updatedAt`은 `@LastModifiedDate` + `AuditingEntityListener`로 관리되는데,
  이 값은 JPA `@PreUpdate` 콜백 시점, 즉 **flush(보통 트랜잭션 커밋 시 자동 발생) 시점**에만 채워진다.
  서비스 메서드에서 `entity.update(...)`로 필드를 바꾼 직후 바로 그 엔티티로 응답 DTO를 만들면,
  아직 flush되지 않아 `@PreUpdate`가 실행되기 전(메모리상 예전 값)을 읽게 된다.
- 해결: 엔티티를 수정한 직후, 응답 DTO를 만들기 전에 명시적으로 `repository.flush()`를 호출해
  `@PreUpdate`(→ `updatedAt` 갱신)를 강제로 먼저 실행시켰다.
  - `AccountService.update()` (`lifehub-api/src/main/java/com/lifehub/finance/service/AccountService.java`)
  - `TransactionCategoryService.update()`
    (`lifehub-api/src/main/java/com/lifehub/finance/service/TransactionCategoryService.java`)
  - `TransactionService.update()`
    (`lifehub-api/src/main/java/com/lifehub/finance/service/TransactionService.java`)
- 참고: JPA의 변경 감지(dirty checking)는 필드를 바꾼 즉시가 아니라 flush 시점에만 실제 UPDATE SQL을
  만든다. 같은 트랜잭션 안에서 방금 바꾼 값을 곧바로 응답에 담아야 할 때는 이 flush 타이밍을 항상
  의식해야 하고, 특히 `@CreatedDate`/`@LastModifiedDate`처럼 애플리케이션 코드가 직접 설정하지 않는
  필드일수록(리스너가 대신 채워주므로) 이 함정에 빠지기 쉽다.

---

## `/error` 재디스패치 시 인증 컨텍스트가 유실되어 403 발생

- 날짜: 2026-09-18
- 증상: 매핑되지 않은 경로(예: `GET /api/does-not-exist`)를 요청하면 기대한 `404 Not Found` 대신
  `403 Forbidden`(빈 바디)이 내려옴.
- 원인: `SeedUserAuthenticationFilter`는 `OncePerRequestFilter`라서 하나의 요청당 정확히 한 번만
  실행된다. 매핑되는 컨트롤러가 없으면 Spring Boot는 내부적으로 `/error`로 **새로운 ERROR 디스패치**를
  수행하는데, 이 디스패치도 `FilterChainProxy`를 다시 통과한다. 이때 `SecurityContextHolderFilter`가
  컨텍스트를 다시 비우지만, `SeedUserAuthenticationFilter`는 (같은 요청으로 인식되어) "이미 실행됨"
  처리되어 재실행되지 않는다. 그 결과 `AnonymousAuthenticationFilter`가 익명 컨텍스트로 채우고,
  `SecurityConfig`의 `anyRequest().authenticated()` 조건에 걸려 `Http403ForbiddenEntryPoint`가 403을
  반환한다.
  `logging.level.org.springframework.security=DEBUG`로 로그를 켜서
  `Securing GET /error` → `Set SecurityContextHolder to anonymous SecurityContext` →
  `Http403ForbiddenEntryPoint : ... Rejecting access` 순서를 직접 확인해 원인을 특정했다.
- 해결: `SecurityConfig`의 `PUBLIC_PATHS`에 `"/error"`를 추가해 이 내부 디스패치를 인증 없이 통과시켰다.
  (`lifehub-api/src/main/java/com/lifehub/common/config/SecurityConfig.java`)
- 참고: 커스텀 인증 필터를 `OncePerRequestFilter`로 만들 때, Spring Boot의 에러 페이지 처리가
  내부적으로 별도의 ERROR 디스패치를 발생시킨다는 점을 놓치기 쉽다. Spring Security + Spring Boot
  조합에서 잘 알려진 이슈이며, 일반적인 해결책은 `/error`를 공개 경로로 열어주는 것이다.

---

## `/actuator/health`가 404를 반환 (의존성 누락)

- 날짜: 2026-09-18
- 증상: `SecurityConfig`의 `PUBLIC_PATHS`에 `/actuator/health`를 이미 공개 경로로 등록해뒀는데도
  실제로 호출하면 `404 Not Found`.
- 원인: `pom.xml`에 `spring-boot-starter-actuator` 의존성 자체가 빠져 있었다. 즉 액추에이터 오토컨피그가
  전혀 동작하지 않아서 `/actuator/health`에 매핑된 핸들러가 애초에 존재하지 않았던, 그냥 "없는 경로"
  상태였다.
- 해결: `pom.xml`에 `spring-boot-starter-actuator`를 추가. (`lifehub-api/pom.xml`)
- 참고: Spring Security의 `permitAll()`은 "그 경로에 대한 인가(authorization)를 통과시킨다"는 의미일 뿐,
  그 경로에 실제로 매핑된 핸들러가 존재하는지는 보장하지 않는다. 시큐리티 설정과 엔드포인트 존재 여부는
  서로 독립적으로 검증해야 하는 별개의 문제다.
