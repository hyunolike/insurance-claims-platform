# 04. 이벤트 · Outbox · 멱등성

> v1의 가장 큰 기술 부채였던 "트랜잭션 커밋 전 이벤트 발행"을 구조적으로 해결한다.

---

## 1. 이벤트 설계 원칙

| 원칙 | 내용 |
|---|---|
| **애그리거트가 기록한다** | 서비스가 만들지 않는다. 상태 변경 경로마다 이벤트가 붙어 있어 누락이 불가능 |
| **Outbox로 발행한다** | 상태 변경과 이벤트 저장이 같은 트랜잭션. 커밋되면 이벤트도 있다 |
| **과거형 사실이다** | `ClaimApproved`(O) / `ApproveClaim`(X). 이벤트는 명령이 아니다 |
| **소비자를 모른다** | 발행 측은 누가 듣는지 신경 쓰지 않는다 |
| **민감정보를 싣지 않는다** | 이름·계좌·진단명을 페이로드에 넣지 않는다. 필요하면 수신 측이 인가받아 조회 |
| **스키마는 하위호환** | 필드 추가는 자유, 삭제·의미변경은 새 버전 |

---

## 2. 이벤트 카탈로그

### 2.1 claims-platform 발행 (내부 + 외부)

| 이벤트 | 발행 시점 | 주 소비자 | 외부 공개 |
|---|---|---|---|
| `claim.received` | 청구 접수 완료 | 알림, 지급기한 스케줄러 | ✅ |
| `claim.documents_requested` | 서류 보완 요청 | 알림 | ✅ |
| `claim.documents_supplemented` | 서류 보완 완료 | 심사 트리거 | 내부 |
| `claim.screening_started` | 자동심사 시작 | 관측 | 내부 |
| `claim.adjudicated` | 심사 종결(모든 판정) | 알림, 통계 | ✅ |
| `claim.referred` | 수동심사 회부 | 심사자 큐, 알림 | 내부 |
| `claim.approved` | 승인 확정 | 지급 프로세스 | ✅ |
| `claim.denied` | 부지급 확정 | 알림(사유 포함) | ✅ |
| `claim.payment_instructed` | 지급지시 생성 | 이체 어댑터 | 내부 |
| `claim.paid` | 이체 성공 | 알림, **business-support**, 통계 | ✅ |
| `claim.payment_failed` | 이체 실패 | 운영 알림 | 내부 |
| `claim.withdrawn` | 청구 철회 | 원장 예약 해제 | 내부 |
| `claim.reopened` | 재심사 접수 | 심사자 큐 | ✅ |
| `claim.reclaimed` | 환수 확정 | **business-support**, 알림 | ✅ |
| `claim.due_date_approaching` | 지급기한 임박 | 운영 알림 | 내부 |
| `claim.due_date_exceeded` | 지급기한 초과 | 운영 알림 + 지연이자 산출 | 내부 |

### 2.2 claims-platform 구독 (business-support 발행)

| 이벤트 | claims의 반응 |
|---|---|
| `policy.issued` | `PolicyReplica` 생성 |
| `policy.endorsed` | `PolicyReplica` 갱신 |
| `policy.lapsed` / `reinstated` / `terminated` | `PolicyReplica` 상태 갱신 (진행 중 심사에 개입하지 않음) |
| `policy.corrected` | **영향받는 미지급 청구를 재심사 대상으로 표시 → 심사자 큐** |

> 상세 근거는 [`01-context-map.md`](01-context-map.md) §5.

---

## 3. 이벤트 스키마

### 3.1 공통 봉투 (Envelope)

```jsonc
{
  "eventId":     "01JBX7K3QM8W2ZP4NRTV9C6DYE",  // ULID. 중복제거 키
  "eventType":   "claim.paid",
  "eventVersion": 1,
  "occurredAt":  "2026-04-05T14:22:31.482+09:00",
  "producer":    "claims-platform",

  "aggregateType": "Claim",
  "aggregateId":   "CLM-20260402-000123",

  "traceId":     "4bf92f3577b34da6a3ce929d0e0e4736",  // 분산추적 연결
  "correlationId": "CLM-20260402-000123",             // 업무 흐름 묶음
  "causationId": "01JBX7K1...",                        // 이 이벤트를 유발한 이벤트

  "payload":     { /* 이벤트별 */ }
}
```

**`eventId`에 ULID를 쓰는 이유**: UUIDv4와 달리 **시간순 정렬**이 가능해 DB 인덱스 지역성이 좋고,
Outbox 재발행 시 순서 복원이 쉽다.

### 3.2 페이로드 예시

```jsonc
// claim.received
{
  "claimNo": "CLM-20260402-000123",
  "policyNo": "P2026-0001234",
  "insuredRef": "CI-xxxx",              // 이름 아님
  "accidentDate": "2026-03-14",
  "claimedAmount": 300000,
  "treatmentCount": 1,
  "receivedAt": "2026-04-02T10:15:00+09:00",
  "dueDate": "2026-04-07",              // 지급기한
  "dueDateReason": "STANDARD_3BD"
}

// claim.adjudicated
{
  "claimNo": "CLM-20260402-000123",
  "adjudicationId": "ADJ-20260402-000456",
  "mode": "AUTO",
  "decision": "PARTIALLY_APPROVED",
  "claimedAmount": 300000,
  "payableAmount": 124000,
  "rulesetVersion": "2026.01",
  "denialReasons": [],
  "benefitSummary": [                    // 통지문 생성용. 금액만, 진단명 없음
    { "coverageCode": "COV-OUTP-COVERED",   "base": 60000,  "deductible": 20000, "payable": 40000 },
    { "coverageCode": "COV-OUTP-UNCOVERED", "base": 120000, "deductible": 36000, "payable": 84000 }
  ]
}

// claim.denied
{
  "claimNo": "CLM-20260402-000123",
  "decision": "DENIED",
  "denialReasons": [
    { "code": "D-POL-004", "label": "부담보 조건에 해당", "ruleId": "R-POL-050" }
  ],
  "appealable": true,
  "appealDeadline": "2026-07-02"
}

// claim.paid  → business-support도 구독
{
  "claimNo": "CLM-20260402-000123",
  "policyNo": "P2026-0001234",
  "insuredRef": "CI-xxxx",
  "paidAmount": 124000,
  "paidAt": "2026-04-05T14:22:31+09:00",
  "benefitYear": "2026",
  "treatmentTypes": ["OUTPATIENT"],
  "majorUncoveredUsed": false            // 4세대 비급여 이용량 연동 보험료 산정용
}
```

### 3.3 페이로드에 넣지 않는 것

| 금지 | 이유 |
|---|---|
| 피보험자 성명·연락처 | 개인정보. Kafka는 암호화 저장 보장이 약함 |
| 계좌번호 | 금융정보 |
| KCD 코드·진단명 | **민감정보(건강정보)**. 이벤트로 흘리면 통제 불가 |
| 주민등록번호 | 애초에 저장하지 않음 |

알림 서비스가 이름이 필요하면 `insuredRef`로 인가받아 조회한다. 이벤트는 **"무슨 일이 있었다"만 전달**한다.

---

## 4. Transactional Outbox

### 4.1 왜 필요한가

```java
// ❌ v1 방식 — 커밋 전 발행
@Transactional
public void approve(Long id) {
    claim.approve();
    repository.save(claim);
    eventPublisher.publishEvent(new ClaimApprovedEvent(...));  // 리스너가 지금 실행됨
    // ↓ 여기서 예외 발생하면?
    // DB는 롤백. 그런데 리스너는 이미 실행됐다. Kafka였다면 이미 발행됐다.
}
```

Phase 3에서 리스너를 Kafka 발행으로 바꾸는 순간, **롤백된 트랜잭션의 이벤트가 외부로 나간다.**
"지급 승인 알림을 받았는데 조회하면 심사중" 같은 사고가 된다.

### 4.2 Outbox 구조

```sql
CREATE TABLE outbox_event (
    id             BIGSERIAL PRIMARY KEY,
    event_id       VARCHAR(26)  NOT NULL UNIQUE,     -- ULID
    event_type     VARCHAR(64)  NOT NULL,
    event_version  INT          NOT NULL DEFAULT 1,
    aggregate_type VARCHAR(32)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    partition_key  VARCHAR(64)  NOT NULL,            -- 순서 보장 단위 = claimNo
    envelope       JSONB        NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING|PUBLISHED|FAILED
    attempts       INT          NOT NULL DEFAULT 0,
    last_error     TEXT         NULL,
    published_at   TIMESTAMPTZ  NULL
);

CREATE INDEX idx_outbox_pending ON outbox_event (status, id) WHERE status = 'PENDING';
```

### 4.3 쓰기 경로

```java
@Transactional
public AdjudicationResult adjudicate(AdjudicateCommand cmd) {
    Claim claim = claimRepository.findById(cmd.claimId()).orElseThrow();
    // ... 심사 실행 ...
    claim.applyAdjudication(result);        // 애그리거트가 이벤트 record()
    claimRepository.save(claim);
    adjudicationRepository.save(adjudication);
    outbox.append(claim.pullEvents());      // ★ 같은 트랜잭션의 INSERT
}   // ← 여기서 커밋. 상태와 이벤트가 원자적으로 함께 저장됨
```

### 4.4 발행 경로 (Relay)

```
Phase 4 (초기):  폴링 릴레이
    @Scheduled(fixedDelay = 500ms)
    SELECT ... WHERE status='PENDING' ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED
    → Kafka 발행 → status='PUBLISHED'

Phase 6 (확장):  Debezium CDC
    WAL을 읽어 발행. 폴링 부하 0, 지연 낮음
```

**`FOR UPDATE SKIP LOCKED`**가 핵심이다. 릴레이 인스턴스를 여러 개 띄워도 같은 행을 두 번 집지 않는다.

### 4.5 최소 1회 전달 (At-least-once)

Outbox는 **최소 1회**를 보장한다. 정확히 1회는 보장하지 않는다.
발행 후 `status` 업데이트 전에 죽으면 재발행된다.

→ **소비자가 멱등해야 한다.** (다음 절)

### 4.6 순서 보장

- Kafka 파티션 키 = `claimNo`
- 같은 청구의 이벤트는 같은 파티션 → 순서 보장
- 다른 청구 간 순서는 보장하지 않음 (필요 없음)

---

## 5. 멱등성

### 5.1 3개 층위

| 층 | 대상 | 수단 |
|---|---|---|
| **API** | 클라이언트 재시도 | `Idempotency-Key` 헤더 |
| **이벤트 소비** | Outbox 재발행, Kafka 재전달 | `processed_event` 테이블 |
| **외부 이체** | 우리의 재시도 | 결정적 멱등키를 은행 API에 전달 |

### 5.2 API 멱등성

```http
POST /api/v1/claims
Idempotency-Key: 7f3a9c21-...
```

```sql
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(64)  PRIMARY KEY,
    endpoint        VARCHAR(128) NOT NULL,
    request_hash    CHAR(64)     NOT NULL,   -- sha256(정규화된 요청 본문)
    status          VARCHAR(16)  NOT NULL,   -- IN_PROGRESS | COMPLETED
    response_status INT          NULL,
    response_body   JSONB        NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL    -- 24시간
);
```

**처리 규칙**

| 상황 | 응답 |
|---|---|
| 키 없음 | 정상 처리 후 기록 |
| 키 존재 + `COMPLETED` + 요청 해시 동일 | **저장된 응답 그대로 반환** (재처리 안 함) |
| 키 존재 + 요청 해시 **다름** | `422 Unprocessable Entity` — 같은 키로 다른 요청은 오류 |
| 키 존재 + `IN_PROGRESS` | `409 Conflict` + `Retry-After` |

**어디에 적용하나**: 상태를 바꾸는 모든 `POST`/`PATCH`. 특히 청구 접수, 지급지시, 심사 확정.
조회(`GET`)는 불필요.

### 5.3 이벤트 소비 멱등성

```sql
CREATE TABLE processed_event (
    consumer_group VARCHAR(64) NOT NULL,
    event_id       VARCHAR(26) NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_group, event_id)
);
```

```java
@KafkaListener(topics = "policy-events", groupId = "claims-policy-replica")
@Transactional
public void on(EventEnvelope envelope) {
    if (!processedEvents.markProcessed("claims-policy-replica", envelope.eventId())) {
        return;   // 이미 처리함. 조용히 무시
    }
    handler.handle(envelope);
}
```

`markProcessed`는 `INSERT ... ON CONFLICT DO NOTHING`의 성공 여부를 반환한다.
**처리와 마킹이 같은 트랜잭션**이어야 한다.

### 5.4 지급 멱등성 (가장 중요)

```java
IdempotencyKey key = IdempotencyKey.of(
    sha256(claimId + ":" + adjudicationId + ":" + amount.value())
);
```

**결정적(deterministic)이어야 한다.** 난수로 만들면 재시도마다 새 키가 되어 멱등성이 사라진다.

```
1차 시도 → 타임아웃 (성공/실패 불명)
  ❌ 재시도 → 중복지급 위험
  ✅ inquire(key)로 결과 조회 → 확인될 때까지 PAID로 전환 안 함
```

DB 제약으로도 막는다:
```sql
ALTER TABLE payment_instruction
    ADD CONSTRAINT uq_payment_per_adjudication UNIQUE (claim_id, adjudication_id);
```

---

## 6. 토픽 구성

| 토픽 | 파티션 키 | 보존 | 소비자 |
|---|---|---|---|
| `claims.claim-events.v1` | `claimNo` | 30일 | 알림, 통계, business-support |
| `claims.payment-events.v1` | `claimNo` | 90일 | 정산, 감사 |
| `policy.policy-events.v1` | `policyNo` | 30일 | claims(읽기모델) |
| `claims.dlq.v1` | 원본 키 | 180일 | 운영자 |

### 6.1 DLQ 정책

```
처리 실패 → 지수 백오프 재시도 (1s, 4s, 16s, 64s)
  → 4회 실패 시 DLQ 이동 + 운영 알림
  → DLQ 메시지는 수동 확인 후 재처리 API로 복구
```

**DLQ를 소리 없이 쌓아두지 않는다.** DLQ 적재 시 알림이 필수다 — 조용한 DLQ는 없는 것과 같다.

---

## 7. 외부 시스템 어댑터

모두 **포트를 도메인이 정의하고, 인프라가 구현**한다. 실연동은 범위 밖이며 스텁으로 구현한다.

| 포트 | 실제 대상 | 이번 범위 |
|---|---|---|
| `PolicySnapshotPort` | business-support | **실제 구현** (레포 간 REST) |
| `FundTransferPort` | 펌뱅킹 / 지급대행 | 스텁 (지연·실패·타임아웃 시뮬레이션) |
| `AccountVerificationPort` | 예금주 실명조회 | 스텁 |
| `DuplicateInsuranceQueryPort` | 보험개발원 등 | 스텁 |
| `NotificationPort` | 알림톡 / SMS / 이메일 | 스텁 (발송 로그만) |
| `MedicalDataPort` | EMR / 전송대행기관 | 인터페이스만 정의 |
| `FraudScreeningPort` | FDS 엔진 | 룰 기반 간이 구현 |

### 7.1 스텁의 품질

스텁이라고 `return true`로 두지 않는다. **실패 경로를 테스트할 수 있어야** 스텁의 값어치가 있다.

```java
public class StubFundTransferAdapter implements FundTransferPort {
    // 설정으로 주입: 성공률, 지연 분포, 타임아웃 발생률
    // 같은 멱등키 재요청 시 이전 결과 반환 (실제 은행 동작 모사)
}
```

---

## 8. 관측성

### 8.1 추적

- 모든 API 응답에 `traceId` 헤더
- 이벤트 봉투에 `traceId` 전파 → Kafka 건너서도 하나의 흐름으로 연결
- `correlationId = claimNo` → 청구 1건의 전 생애를 한 번에 조회

### 8.2 핵심 지표

| 지표 | 타입 | 용도 |
|---|---|---|
| `claim.received.count` | Counter | 접수량 |
| `claim.adjudication.duration` | Timer | 심사 소요시간 |
| `claim.decision{decision}` | Counter | 판정 분포 |
| `claim.referred{reason}` | Counter | **회부 사유별 — 자동화율 개선의 출발점** |
| `claim.due_date.remaining` | Gauge | 지급기한 잔여 분포 |
| `claim.due_date.exceeded` | Counter | **기한 초과 건수 (법적 리스크)** |
| `outbox.pending.age` | Gauge | Outbox 지연 (릴레이 건강도) |
| `payment.unknown.count` | Gauge | **결과 불명 이체 (즉시 조치 대상)** |
| `ledger.contention.count` | Counter | 원장 락 충돌 |

### 8.3 알림 임계

| 조건 | 심각도 |
|---|---|
| 지급기한 초과 건 발생 | **P1** — 법적 의무 위반 |
| 이체 결과 불명 > 0, 10분 지속 | **P1** — 중복지급·미지급 위험 |
| Outbox pending age > 5분 | P2 |
| DLQ 적재 | P2 |
| 스냅샷 checksum 불일치 | **P1** — 데이터 변조 의심 |

---

## 9. 계약 테스트 (Consumer-Driven Contract)

공유 DTO 모듈 없이 두 레포의 호환성을 지킨다.

```
insurance-claims-platform/
└── src/test/resources/contracts/
    ├── policy-snapshot-response.json      ← claims가 기대하는 응답 형태
    └── policy-events/
        ├── policy.issued.json
        └── policy.corrected.json
```

1. claims가 계약 파일을 정의하고 **자기 레포에서 스텁으로 검증**
2. 계약 파일을 business-support CI가 가져가 **실제 응답이 계약을 만족하는지 검증**
3. BS가 계약을 깨면 **BS의 빌드가 실패** → 배포 전에 잡힌다

> 공유 라이브러리를 만들지 않는 이유는 [`01-context-map.md`](01-context-map.md) §8.1 참조.

---

## 다음 문서

- [`05-api.md`](05-api.md) — REST 명세
- [`06-data-model.md`](06-data-model.md) — 스키마
- [`07-architecture.md`](07-architecture.md) — 모듈 구조와 강제 장치
