# Phase 1 - 청구 생성/조회 첫 슬라이스

## 1. 작업 개요
- **목표:** `POST /api/v1/claims`, `GET /api/v1/claims/{id}` 동작하는 첫 슬라이스 확보
- **기간:** 2025-12-28
- **산출물:** 청구 도메인 모델, JPA 어댑터, API + DTO + 예외 처리, Flyway 스키마, H2 테스트 환경

## 2. 주요 변경 사항
### 2.1 빌드 & 설정 (`build.gradle`, `src/main/resources/application.yml`)
- Spring Data JPA/Redis/Kafka, Flyway, PostgreSQL 드라이버, Jackson JSR-310 추가
- 관리 포인트: `jakarta.validation` 및 Kafka 테스트용 `spring-kafka-test`
- 운영 설정에 Redis/Kafka/Flyway/JPA 옵션 반영, Actuator 노출 범위 지정

### 2.2 DB 마이그레이션 (`src/main/resources/db/migration/V1__init.sql`)
- **기술 선택 이유**
  - *Flyway*: SQL 기반 버전 관리를 통해 배포·회귀 추적이 용이하고, Phase별 스키마 변화를 문서화 없이 되돌릴 수 있음
  - *PostgreSQL + UUID*: 사건 이력/알림 등 분산 환경에서 ID 충돌을 피하기 위해 UUID, Claim 본문은 정렬·조인 빈도를 고려해 BIGSERIAL
- **구조 요약**
  - Claim이 Aggregate Root → ClaimEvent/Review/Payment/Notification/Audit가 Claim FK를 참조
  - Kafka 이벤트 스키마와 동일한 `claim_events`로 재처리/관측 가능성 확보
  - policy+status, submitted_at 인덱스로 상담사/보고서 조회 성능 확보
- **운영 흐름**
  1. `docker compose up -d`로 Postgres를 올리면 애플리케이션 시작 시 Flyway가 V1 스크립트를 실행
  2. 개발 중 스키마 변경 발생 시 `V2__*.sql` 추가 → 자동 마이그레이션 → git 이력으로 추적

### 2.3 도메인 모델링 (`src/main/java/com/insurance/claim/domain/**`)
- `Claim` Aggregate: 상태 전환(`markInReview/approve/reject/markPaid`), ID/타임스탬프 관리
- Value Object: `ClaimNumber`(규칙 검증, 랜덤 생성), `Money`(스케일 통일, 음수 방지)
- `ClaimRepository` 인터페이스와 JPA 구현(엔티티 + 어댑터)로 Hexagonal 구조 유지

### 2.4 애플리케이션 & API (`src/main/java/com/insurance/claim/application/**`, `api/**`)
- `ClaimService`: Command 기반 생성, 조회, `ClaimMapper`로 응답 DTO 변환
- DTO: `ClaimCreateRequest`, `ClaimResponse`
- Controller: `/api/v1/claims` POST/GET 구현
- 예외 처리: `ClaimNotFoundException` + `ApiExceptionHandler`

#### 요청 흐름 (POST /api/v1/claims)
1. 클라이언트가 `ClaimCreateRequest` JSON 전송 → Bean Validation으로 기본 검증
2. `ClaimController`가 Request → `CreateClaimCommand` 변환 후 `ClaimService` 호출
3. `ClaimService`가 `Claim.create()`로 Aggregate 인스턴스를 생성하고 Repository 저장
4. JPA 어댑터가 Entity로 변환해 DB에 영속화 후 Domain 객체로 다시 변환
5. `ClaimMapper`가 API 응답 DTO 빌드 → HTTP 201 Created 반환

#### 요청 흐름 (GET /api/v1/claims/{id})
1. Controller가 `claimId` PathVariable을 받아 서비스 호출
2. Repository에서 Claim 조회, 없으면 `ClaimNotFoundException` 던짐 → 404 변환
3. DTO로 매핑하여 200 OK 응답

### 2.5 테스트 & 프로필
- `src/test/resources/application.yml` 에 H2(in-memory, PostgreSQL 모드) 구성 → CI에서 외부 DB 없이 테스트
- `testImplementation 'com.h2database:h2'` 추가
- `./gradlew clean test` 성공 (2025-12-28 16:48 KST)

### 2.6 운영 로깅 & AOP (`build.gradle`, `RequestResponseLoggingAspect`)
- **전략:** Controller/Service 진입·종료 시 공통 로그를 남겨 추적성을 확보하고, PII는 Aspect에서 마스킹 처리
- **구현:** `spring-boot-starter-aop` 추가 → `RequestResponseLoggingAspect`가 `@Around`로 API/Service 레이어 감싸기
  - 진입 로그: 메서드 시그니처 + 요약된 인자 (이메일 마스킹, DTO만 상세 표시)
  - 종료 로그: 요약된 결과 + 실행 시간(ms)
  - 예외 시 ERROR 로그로 메시지·스택 기록
- **효과:** 코드 곳곳에 `log.info`를 흩뿌리지 않고도 일관된 로깅을 유지, 추후 Audit Aspect 추가도 동일 패턴으로 확장 가능

## 3. 남은 TODO / Next Step
1. Phase 2 준비: 도메인 이벤트 표현 및 Spring Event 발행 로직 추가
2. Claim 단위/통합 테스트 보강 (Testcontainers + Repository/Service)
3. Redis/Kafka 설정 세분화 (생성 토픽, DLQ, 멱등 키 저장소 등)
4. API 문서 (`docs/API.md`) 업데이트: 신규 엔드포인트 스펙 반영
5. 운영/테스트 프로필 분리(`application-local.yml`, `application-test.yml`)로 설정 충돌 방지

## 4. 참고 명령어
```bash
./gradlew clean test
docker compose up -d   # 인프라 환경 확인용
```
