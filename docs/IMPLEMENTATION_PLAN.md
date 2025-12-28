# 보험금 청구 자동화 시스템 구현 계획서

## 📋 목차
- [개요](#개요)
- [구현 전략](#구현-전략)
- [Phase별 상세 계획](#phase별-상세-계획)
- [기술적 의사결정](#기술적-의사결정)
- [리스크 관리](#리스크-관리)
- [품질 보증 전략](#품질-보증-전략)
- [성공 지표](#성공-지표)

---

## 개요

### 문서 목적
본 문서는 보험금 청구 자동화 시스템의 **구현 전략과 실행 계획**을 정의합니다. 단순한 작업 나열이 아닌, **왜 이 순서로 개발하는가**에 대한 기술적 근거와 비즈니스 가치를 중심으로 작성되었습니다.

### 프로젝트 목표 (SMART 기준)
| 지표 | 현재 | 목표 | 측정 방법 |
|------|------|------|-----------|
| 처리 시간 | 평균 10분 | 7분 이하 | `ClaimEvent` 타임스탬프 차이 |
| 재오픈율 | 5% | 2% 이하 | 14일 내 고객 재문의 비율 |
| 자동화율 | 수동 심사 | 80% 자동 승인 | Rule Engine 처리 비율 |

### 구현 기간
- **MVP 구현**: 4주 (Phase 1~3)
- **전체 구현**: 6주 (Phase 1~5)

---

## 구현 전략

### 1. 수직적 슬라이싱 (Vertical Slicing)

**선택 이유:**
- 빠른 피드백 루프: 각 Phase마다 End-to-End 테스트 가능
- 리스크 조기 발견: 통합 문제를 초기에 발견
- 점진적 가치 제공: 각 Phase가 완성되면 바로 데모 가능

**대안 (수평적 레이어링)을 선택하지 않은 이유:**
- 모든 레이어 완성 후에야 통합 테스트 가능
- 비즈니스 가치 확인이 늦어짐
- 요구사항 변경 시 전체 레이어 수정 필요

### 2. Domain-Driven Design 적용

**핵심 원칙:**
```
api → application → domain ← infrastructure
```

**장점:**
- 비즈니스 로직과 기술 구현 분리
- 테스트 용이성 (Domain은 순수 Java 객체)
- 영속성 기술 교체 시 Domain 레이어 무변경

**구현 순서:**
1. **Domain부터 시작** (비즈니스 규칙 먼저 정의)
2. Infrastructure 구현 (Domain 인터페이스 구현)
3. Application 조율 (트랜잭션, DTO 변환)
4. API 노출 (HTTP 엔드포인트)

### 3. 이벤트 기반 아키텍처

**이벤트 흐름:**
```
claim.requested → claim.reviewed → claim.approved → claim.disbursed
```

**구현 우선순위:**
1. Phase 2: 동기식 구현 (Spring Event)
2. Phase 3: 비동기 전환 (Kafka)
3. Phase 4: 재처리 메커니즘 (Dead Letter Queue)

**단계적 전환 이유:**
- 초기 복잡도 감소 (Kafka 인프라 없이도 개발 가능)
- 이벤트 스키마 검증 후 비동기 전환
- 장애 시나리오 점진적 보강

---

## Phase별 상세 계획

### Phase 0: 기반 설정 (1일)

**목표:** 개발 환경 완성 및 핵심 의존성 설정

**작업 항목:**
1. `build.gradle` 의존성 추가
   - Spring Data JPA + PostgreSQL
   - Spring Kafka
   - Spring Data Redis
   - Validation, Lombok

2. `application.yml` 설정
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/claim
     jpa:
       hibernate:
         ddl-auto: validate
     kafka:
       bootstrap-servers: localhost:9092
   ```

3. Docker Compose 인프라 실행
   ```bash
   docker compose up -d
   ```

4. Flyway/Liquibase 마이그레이션 설정
   - DDL 스크립트 관리
   - 버전 관리

**완료 기준 (Definition of Done):**
- [ ] `./gradlew build` 성공
- [ ] `./gradlew bootRun` 실행 시 DB 연결 성공
- [ ] Health Check API 응답 확인
- [ ] Kafka 토픽 생성 확인

**산출물:**
- 설정 파일 (`application.yml`, `build.gradle`)
- DB 마이그레이션 스크립트 (V1__init.sql)

---

### Phase 1: 청구 생성 + 조회 (3일)

**목표:** 핵심 도메인 모델 완성 및 첫 번째 수직 슬라이스 구현

**비즈니스 가치:**
- 고객이 보험금 청구를 등록하고 상태를 조회할 수 있음
- 데모 가능한 첫 번째 기능

#### 1.1 Domain 레이어

**파일 구조:**
```
domain/
├── model/
│   ├── Claim.java              # Aggregate Root
│   └── ClaimStatus.java        # Enum
├── vo/
│   ├── ClaimNumber.java        # Value Object
│   └── Money.java              # Value Object
└── repository/
    └── ClaimRepository.java    # 인터페이스
```

**핵심 설계 결정:**

**1) Claim을 Aggregate Root로 선정**
- 이유: 모든 이벤트의 시작점
- 책임: 상태 전환 로직 (`approve()`, `reject()`)
- 불변성: 생성 후 ID 변경 불가

**2) ClaimNumber를 Value Object로 분리**
- 이유: 비즈니스 규칙 캡슐화 (형식: `CLM-20250115-A3F9E`)
- 장점: 재사용 가능, 단위 테스트 용이

**3) Money Value Object**
- 이유: BigDecimal 연산 실수 방지
- 기능: 금액 비교, 덧셈/뺄셈, 음수 방지

**구현 예시:**
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Claim {
    private Long id;
    private ClaimNumber claimNumber;
    private Money claimAmount;
    private ClaimStatus status;
    private LocalDateTime createdAt;

    @Builder
    public Claim(BigDecimal claimAmount, ...) {
        this.claimNumber = ClaimNumber.generate();
        this.claimAmount = new Money(claimAmount);
        this.status = ClaimStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void approve() {
        if (this.status != ClaimStatus.PENDING) {
            throw new IllegalStateException("대기 중인 청구만 승인 가능");
        }
        this.status = ClaimStatus.APPROVED;
    }
}
```

#### 1.2 Infrastructure 레이어

**파일 구조:**
```
infrastructure/persistence/
├── ClaimEntity.java            # JPA Entity
├── ClaimJpaRepository.java     # Spring Data JPA
├── ClaimRepositoryImpl.java    # Repository 구현체
└── ClaimMapper.java            # Domain ↔ Entity 변환
```

**핵심 설계 결정:**

**1) Domain과 JPA Entity 분리**
- 이유: 영속성 관심사 격리, JPA 애노테이션이 Domain을 오염시키지 않음
- 단점: 변환 비용 존재
- 판단: 장기적 유지보수성 > 단기 성능

**2) Mapper 패턴 사용**
```java
@Component
public class ClaimMapper {
    public ClaimEntity toEntity(Claim domain) { ... }
    public Claim toDomain(ClaimEntity entity) { ... }
}
```

**3) 복합 인덱스 설계**
```sql
CREATE INDEX idx_policy_status ON claims(policy_number, status);
CREATE INDEX idx_accident_date ON claims(accident_date);
```
- 이유: `findByPolicyNumberAndStatus` 쿼리 최적화

#### 1.3 Application 레이어

**파일:**
```
application/service/
└── ClaimService.java
```

**책임:**
- 트랜잭션 경계 정의 (`@Transactional`)
- DTO 변환 (API DTO ↔ Domain)
- Domain Service 조율

**구현 예시:**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimService {
    private final ClaimRepository claimRepository;

    @Transactional
    public ClaimResponse createClaim(ClaimCreateRequest request) {
        Claim claim = Claim.builder()
            .claimAmount(request.getClaimedAmount())
            .build();

        Claim saved = claimRepository.save(claim);
        return toResponse(saved);
    }

    public ClaimResponse getClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotFoundException(claimId));
        return toResponse(claim);
    }
}
```

#### 1.4 API 레이어

**파일 구조:**
```
api/
├── controller/
│   └── ClaimController.java
├── dto/
│   ├── request/
│   │   └── ClaimCreateRequest.java
│   └── response/
│       └── ClaimResponse.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ClaimNotFoundException.java
    └── ErrorResponse.java
```

**Validation 전략:**
```java
@Getter
@NoArgsConstructor
public class ClaimCreateRequest {
    @NotBlank(message = "보험 증권 번호는 필수입니다")
    private String policyNumber;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal claimedAmount;

    @PastOrPresent
    private LocalDate accidentDate;
}
```

**예외 처리:**
- `ClaimNotFoundException` → 404 NOT_FOUND
- `IllegalStateException` → 400 BAD_REQUEST
- `MethodArgumentNotValidException` → 400 VALIDATION_ERROR

**완료 기준:**
- [ ] `POST /claims` API 성공
- [ ] `GET /claims/{id}` API 성공
- [ ] 통합 테스트 작성 완료
- [ ] Postman/curl로 End-to-End 테스트 성공

**산출물:**
- Domain 모델 (Claim, Value Objects)
- REST API 2개 (생성, 조회)
- 통합 테스트 코드

---

### Phase 2: 이벤트 발행 - 동기식 (2일)

**목표:** 이벤트 기반 아키텍처 기반 마련 (Kafka 없이 Spring Event 사용)

**비즈니스 가치:**
- 청구 생성 시 알림 발송 준비
- 감사 로그 자동 기록

#### 2.1 Domain Event 정의

**파일:**
```
domain/event/
├── ClaimCreatedEvent.java
└── ClaimApprovedEvent.java
```

**설계:**
```java
@Getter
@Builder
public class ClaimCreatedEvent {
    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private BigDecimal claimAmount;
    private LocalDateTime createdAt;
}
```

#### 2.2 Event Publisher (Spring ApplicationEventPublisher)

**구현:**
```java
@Service
@RequiredArgsConstructor
public class ClaimService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ClaimResponse createClaim(ClaimCreateRequest request) {
        Claim saved = claimRepository.save(claim);

        // 이벤트 발행
        ClaimCreatedEvent event = ClaimCreatedEvent.from(saved);
        eventPublisher.publishEvent(event);

        return toResponse(saved);
    }
}
```

#### 2.3 Event Listener (알림 서비스 시뮬레이션)

**구현:**
```java
@Component
@Slf4j
public class ClaimEventListener {
    @EventListener
    public void handleClaimCreated(ClaimCreatedEvent event) {
        log.info("청구 생성 이벤트 수신: {}", event.getClaimNumber());
        // TODO: Phase 4에서 실제 알림 발송 구현
    }
}
```

**완료 기준:**
- [ ] 이벤트 발행/구독 동작 확인
- [ ] 로그에 이벤트 수신 메시지 출력
- [ ] 트랜잭션 롤백 시 이벤트 미발행 확인

**산출물:**
- Domain Event 클래스
- Event Listener (로그만 출력)

---

### Phase 3: Kafka 전환 - 비동기 이벤트 (3일)

**목표:** Spring Event → Kafka 전환, 진짜 이벤트 기반 아키텍처 구현

**비즈니스 가치:**
- 서비스 간 느슨한 결합
- 장애 격리 (알림 실패해도 청구 생성은 성공)
- 확장 가능성 (Consumer 추가로 수평 확장)

#### 3.1 Kafka 토픽 설계

**토픽 목록:**
| 토픽명 | 파티션 | 용도 |
|--------|--------|------|
| `claim.requested` | 3 | 청구 생성 |
| `claim.reviewed` | 3 | 심사 완료 |
| `claim.approved` | 3 | 승인 확정 |
| `claim.disbursed` | 3 | 지급 완료 |

**파티션 키 전략:**
- `claimId`를 파티션 키로 사용
- 이유: 같은 청구의 이벤트는 순서 보장 필요

#### 3.2 Event Schema 정의

**JSON Schema (예시):**
```json
{
  "eventId": "uuid",
  "eventType": "claim.requested",
  "claimId": "uuid",
  "payload": {
    "claimNumber": "CLM-20250115-A3F9E",
    "policyNumber": "POL-123456",
    "claimAmount": 250000
  },
  "occurredAt": "2025-01-15T10:13:00Z"
}
```

**버전 관리:**
- Header에 `schema-version: v1` 포함
- 향후 스키마 변경 시 v2로 마이그레이션

#### 3.3 Producer 구현

**파일:**
```
infrastructure/messaging/
├── KafkaProducerConfig.java
└── ClaimEventPublisher.java
```

**구현:**
```java
@Component
@RequiredArgsConstructor
public class ClaimEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishClaimCreated(Claim claim) {
        ClaimCreatedEvent event = ClaimCreatedEvent.from(claim);

        kafkaTemplate.send(
            "claim.requested",
            claim.getId().toString(), // 파티션 키
            event
        );
    }
}
```

#### 3.4 Consumer 구현

**구현:**
```java
@Component
@Slf4j
public class ClaimEventConsumer {
    @KafkaListener(topics = "claim.requested", groupId = "notification-service")
    public void handleClaimRequested(ClaimCreatedEvent event) {
        log.info("알림 발송 준비: {}", event.getClaimNumber());
        // TODO: Phase 4에서 실제 알림 구현
    }
}
```

#### 3.5 멱등성 보장

**문제:** Kafka 메시지 재전송 시 중복 처리 위험

**해결책:**
```java
@Service
public class IdempotentEventProcessor {
    private final RedisTemplate<String, String> redisTemplate;

    public boolean isProcessed(String eventId) {
        return redisTemplate.opsForValue()
            .setIfAbsent("event:" + eventId, "1", 1, TimeUnit.HOURS);
    }
}
```

**완료 기준:**
- [ ] Kafka Producer/Consumer 동작 확인
- [ ] Kafka UI에서 메시지 확인
- [ ] 메시지 재전송 시 중복 처리 방지 검증
- [ ] Consumer 장애 시 재처리 확인

**산출물:**
- Kafka Producer/Consumer
- 멱등성 처리 로직
- 통합 테스트

---

### Phase 4: 심사 + 지급 (4일)

**목표:** Rule Engine 기반 자동 심사 및 지급 프로세스 구현

**비즈니스 가치:**
- 자동화율 80% 달성
- 처리 시간 단축 (10분 → 7분)

#### 4.1 Review 도메인

**파일:**
```
domain/model/
├── Review.java
└── ReviewDecision.java (enum)
```

**설계:**
```java
@Getter
public class Review {
    private Long claimId;
    private ReviewDecision decision; // APPROVED, REJECTED, MANUAL
    private String reason;
    private String reviewer; // "rule-engine" or "상담사 이름"
    private LocalDateTime decidedAt;

    public static Review autoApprove(Long claimId, String reason) {
        Review review = new Review();
        review.claimId = claimId;
        review.decision = ReviewDecision.APPROVED;
        review.reason = reason;
        review.reviewer = "rule-engine";
        review.decidedAt = LocalDateTime.now();
        return review;
    }
}
```

#### 4.2 Rule Engine Service

**간단한 규칙 예시:**
```java
@Service
@RequiredArgsConstructor
public class RuleEngineService {
    private final ClaimRepository claimRepository;

    public ReviewDecision evaluate(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow();

        // 규칙 1: 100만원 이하 자동 승인
        if (claim.getClaimAmount().isLessThanOrEqual(new Money(1_000_000))) {
            return ReviewDecision.APPROVED;
        }

        // 규칙 2: 500만원 이상 수동 심사
        if (claim.getClaimAmount().isGreaterThan(new Money(5_000_000))) {
            return ReviewDecision.MANUAL;
        }

        return ReviewDecision.APPROVED;
    }
}
```

**향후 확장:**
- Drools 같은 Rule Engine 도입
- 외부 규칙 파일로 관리 (YAML, JSON)

#### 4.3 이벤트 흐름 완성

**시퀀스:**
```
1. claim.requested 발행 (청구 생성)
   ↓
2. RuleEngineConsumer가 구독
   ↓
3. 자동 심사 실행
   ↓
4. claim.reviewed 발행 (심사 완료)
   ↓
5. APPROVED인 경우 claim.approved 발행
   ↓
6. PaymentService가 claim.approved 구독
   ↓
7. 지급 처리 후 claim.disbursed 발행
```

#### 4.4 Payment 도메인

**파일:**
```
domain/model/
├── Payment.java
└── PayoutStatus.java (enum)
```

**간단한 지급 시뮬레이션:**
```java
@Service
@Slf4j
public class PaymentService {
    @KafkaListener(topics = "claim.approved", groupId = "payment-service")
    public void processPayment(ClaimApprovedEvent event) {
        log.info("지급 처리 시작: {}", event.getClaimNumber());

        // 실제로는 외부 은행 API 호출
        // 여기서는 시뮬레이션
        Payment payment = Payment.builder()
            .claimId(event.getClaimId())
            .amount(event.getClaimAmount())
            .method(PaymentMethod.ACCOUNT_TRANSFER)
            .status(PayoutStatus.SUCCESS)
            .build();

        paymentRepository.save(payment);

        // claim.disbursed 이벤트 발행
        eventPublisher.publishClaimDisbursed(event.getClaimId());
    }
}
```

**완료 기준:**
- [ ] Rule Engine 자동 승인 동작 확인
- [ ] 심사 → 지급 이벤트 흐름 End-to-End 테스트
- [ ] 수동 심사 케이스 처리 확인
- [ ] Payment 생성 및 상태 조회 API 완성

**산출물:**
- Review, Payment 도메인
- Rule Engine Service
- 이벤트 기반 자동 처리 로직

---

### Phase 5: 알림 + 관측성 (3일)

**목표:** 알림 발송 및 Observability 구축

#### 5.1 Notification Service

**파일:**
```
application/service/
└── NotificationService.java
```

**구현 (이메일 시뮬레이션):**
```java
@Service
@Slf4j
public class NotificationService {
    @KafkaListener(topics = "claim.requested", groupId = "notification-service")
    public void sendClaimReceivedNotification(ClaimCreatedEvent event) {
        log.info("[이메일] 청구 접수 완료: {}", event.getClaimNumber());
        // 실제로는 이메일 전송 로직
    }

    @KafkaListener(topics = "claim.disbursed", groupId = "notification-service")
    public void sendPaymentCompletedNotification(ClaimDisbursedEvent event) {
        log.info("[이메일] 지급 완료: {}", event.getClaimNumber());
    }
}
```

#### 5.2 Audit Log

**자동 감사 로그:**
```java
@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        AuditLog log = AuditLog.builder()
            .actor(SecurityContextHolder.getContext().getAuthentication().getName())
            .action(joinPoint.getSignature().getName())
            .timestamp(LocalDateTime.now())
            .build();

        auditLogRepository.save(log);
        return joinPoint.proceed();
    }
}
```

#### 5.3 Observability

**1) Metrics (Micrometer + Prometheus)**
```java
@Service
public class ClaimMetrics {
    private final MeterRegistry meterRegistry;

    public void recordClaimCreated() {
        meterRegistry.counter("claim.created.total").increment();
    }

    public void recordProcessingTime(Duration duration) {
        meterRegistry.timer("claim.processing.time").record(duration);
    }
}
```

**2) Distributed Tracing**
- `traceId` 헤더를 모든 API 응답에 포함
- Kafka 메시지에도 `traceId` 전달

**3) Logging**
- 구조화된 JSON 로그
```json
{
  "timestamp": "2025-01-15T10:13:00Z",
  "level": "INFO",
  "traceId": "abc123",
  "message": "청구 생성 완료",
  "claimId": "uuid"
}
```

**완료 기준:**
- [ ] 알림 발송 로그 확인
- [ ] Audit Log DB 저장 확인
- [ ] Prometheus 메트릭 수집 확인
- [ ] 처리 시간 7분 이하 달성 검증

**산출물:**
- Notification Service
- Audit Log 자동 기록
- Metrics/Logging 설정

---

## 기술적 의사결정

### 1. 왜 JPA를 선택했는가?

**선택:** Spring Data JPA + QueryDSL

**이유:**
- **생산성**: CRUD 보일러플레이트 제거
- **ORM 장점**: 객체 중심 개발, 영속성 컨텍스트
- **팀 익숙도**: Spring 생태계 표준

**고려한 대안:**
- **MyBatis**: SQL 직접 제어 가능하지만 생산성 낮음
- **JDBC Template**: 너무 Low-level

**Trade-off:**
- N+1 문제 → Fetch Join, @EntityGraph로 해결
- 복잡한 쿼리 → QueryDSL로 타입 안전하게 작성

### 2. 왜 Kafka를 선택했는가?

**선택:** Apache Kafka

**이유:**
- **대용량 처리**: 수평 확장 가능
- **이벤트 소싱**: 이벤트 히스토리 영구 저장
- **업계 표준**: 토스 등 금융권 표준

**고려한 대안:**
- **RabbitMQ**: 메시지 큐 중심, 이벤트 보관 약함
- **AWS SQS**: 벤더 락인 우려

### 3. 왜 Redis를 선택했는가?

**선택:** Redis

**이유:**
- **분산 락**: 중복 지급 방지
- **캐싱**: 청구 상태 조회 성능 향상
- **멱등성**: 이벤트 중복 처리 방지

**사용 사례:**
```java
// 분산 락
RedisLockRegistry lockRegistry;
Lock lock = lockRegistry.obtain("claim:" + claimId);
if (lock.tryLock()) {
    try {
        // 중복 지급 방지 로직
    } finally {
        lock.unlock();
    }
}
```

### 4. 왜 Domain과 JPA Entity를 분리했는가?

**선택:** Domain 모델과 JPA Entity 분리

**이유:**
- **순수성**: Domain에 JPA 애노테이션 오염 방지
- **테스트**: Domain 단위 테스트 시 DB 불필요
- **기술 독립성**: ORM 교체 시 Domain 무변경

**Trade-off:**
- 변환 비용 존재 (Mapper 필요)
- 코드 증가

**판단:** 장기적 유지보수성 > 단기 생산성

---

## 리스크 관리

### 리스크 1: Kafka 메시지 유실

**영향도:** 높음
**발생 가능성:** 중간

**완화 전략:**
1. **Producer Acks 설정**: `acks=all` (모든 ISR 응답 대기)
2. **Retry 정책**: 재전송 최대 3회
3. **Dead Letter Queue**: 3회 실패 시 별도 토픽 저장
4. **모니터링**: Kafka Lag 알림 설정

### 리스크 2: 중복 지급

**영향도:** 매우 높음
**발생 가능성:** 낮음

**완화 전략:**
1. **분산 락**: Redis로 동일 claimId 동시 처리 방지
2. **멱등성 키**: Payment에 Unique 제약 조건
   ```sql
   UNIQUE (claim_id, idempotency_key)
   ```
3. **트랜잭션**: `@Transactional`로 원자성 보장

### 리스크 3: DB 성능 저하

**영향도:** 중간
**발생 가능성:** 중간

**완화 전략:**
1. **인덱스 최적화**: 자주 조회되는 컬럼 인덱스
2. **Redis 캐싱**: 청구 상태 조회 캐싱 (TTL 5분)
3. **Read Replica**: 조회 쿼리 분산
4. **N+1 방지**: Fetch Join 적극 활용

### 리스크 4: 외부 API 장애 (지급 시스템)

**영향도:** 높음
**발생 가능성:** 낮음

**완화 전략:**
1. **Circuit Breaker**: Resilience4j 적용
   ```java
   @CircuitBreaker(name = "paymentApi")
   public void callPaymentApi() { ... }
   ```
2. **Retry + Backoff**: 지수 백오프 재시도
3. **Fallback**: 수동 처리 큐로 전환
4. **타임아웃**: 10초 제한

---

## 품질 보증 전략

### 1. 테스트 전략

**테스트 피라미드:**
```
       /\
      /E2E\       (10%) - 통합 테스트
     /------\
    /  API  \     (20%) - Controller 테스트
   /----------\
  / Unit Tests \ (70%) - Domain, Service 테스트
 /--------------\
```

**커버리지 목표:**
- Domain 레이어: 90% 이상
- Application 레이어: 80% 이상
- 전체: 70% 이상

### 2. 테스트 종류

**Unit Test:**
```java
@Test
void approve_shouldChangeStatusToApproved_whenPending() {
    Claim claim = Claim.builder().build();

    claim.approve();

    assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
}
```

**Integration Test:**
```java
@SpringBootTest
@AutoConfigureTestDatabase
class ClaimServiceIntegrationTest {
    @Test
    void createClaim_shouldSaveAndReturnClaim() {
        ClaimCreateRequest request = new ClaimCreateRequest(...);

        ClaimResponse response = claimService.createClaim(request);

        assertThat(response.getClaimId()).isNotNull();
    }
}
```

**E2E Test:**
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class ClaimE2ETest {
    @Container
    static PostgreSQLContainer<?> postgres = ...;

    @Test
    void fullClaimFlow_shouldCompleteSuccessfully() {
        // 청구 생성 → 심사 → 지급 전체 흐름 테스트
    }
}
```

### 3. 코드 품질

**정적 분석:**
- Checkstyle: 코딩 컨벤션
- SonarQube: 코드 품질 메트릭
- SpotBugs: 잠재적 버그 탐지

**Code Review 체크리스트:**
- [ ] 비즈니스 로직이 Domain에 있는가?
- [ ] 트랜잭션 경계가 적절한가?
- [ ] 예외 처리가 명확한가?
- [ ] 테스트 커버리지 충족하는가?

---

## 성공 지표

### 1. SMART 목표 달성 여부

| 지표 | 측정 방법 | 목표 | 검증 시점 |
|------|-----------|------|-----------|
| 처리 시간 | `claim.requested` → `claim.disbursed` 타임스탬프 차이 | 7분 이하 | Phase 4 완료 시 |
| 자동화율 | Rule Engine 처리 건수 / 전체 청구 건수 | 80% | Phase 4 완료 시 |
| 재오픈율 | 14일 내 재문의 건수 / 전체 청구 건수 | 2% 이하 | Phase 5 완료 후 |

### 2. 기술 지표

| 지표 | 목표 |
|------|------|
| API 응답 시간 | P95 < 200ms |
| 테스트 커버리지 | 70% 이상 |
| Kafka Lag | 1분 이내 |
| DB Connection Pool | 80% 이하 |

### 3. 데모 시나리오

**시나리오 1: 자동 승인 케이스**
1. 청구 생성 (100만원)
2. 자동 심사 통과
3. 지급 완료
4. 알림 발송
5. **전체 처리 시간: 5분**

**시나리오 2: 수동 심사 케이스**
1. 청구 생성 (600만원)
2. 수동 심사 대기
3. 상담사 승인
4. 지급 완료

---

## 부록

### A. 개발 환경 설정

**필수 도구:**
- JDK 21
- Docker Desktop
- IntelliJ IDEA
- Postman

**로컬 실행:**
```bash
# 1. 인프라 실행
docker compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun

# 3. Health Check
curl http://localhost:8080/health
```

### B. 참고 문서

- [API 명세](API.md)
- [ERD 설계](ERD.md)
- [패키지 구조](PACKAGE_STRUCTURE.md)
- [WBS](WBS.md)

### C. 용어집

| 용어 | 설명 |
|------|------|
| Aggregate Root | DDD에서 엔티티 집합의 진입점 |
| Value Object | 불변 값 객체 |
| Idempotency | 동일 요청 여러 번 실행해도 결과 동일 |
| Circuit Breaker | 장애 전파 방지 패턴 |
| Dead Letter Queue | 처리 실패 메시지 저장소 |

---

**작성일:** 2025-01-15
**버전:** 1.0
**작성자:** Backend Developer