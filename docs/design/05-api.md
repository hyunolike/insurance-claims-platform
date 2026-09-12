# 05. API 명세 — claims-platform

Base: `/api/v1`
인증: `Authorization: Bearer {token}` (고객) / `X-Service-Token` (서비스 간)

> v1의 문제였던 "문서와 코드의 경로·필드 불일치"를 막기 위해,
> **이 문서는 OpenAPI 스펙에서 생성**한다. 손으로 관리하지 않는다. ([`07-architecture.md`](07-architecture.md) §7)

---

## 1. 공통 규약

### 1.1 응답 헤더

| 헤더 | 내용 |
|---|---|
| `X-Trace-Id` | 분산추적 ID. 모든 응답에 포함 |
| `ETag` | 리소스 버전. 조건부 수정에 사용 |

### 1.2 오류 응답

```jsonc
{
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "timestamp": "2026-04-02T10:15:00+09:00",
  "status": 409,
  "code": "CLAIM_ILLEGAL_TRANSITION",
  "message": "현재 상태(PAID)에서는 수행할 수 없는 작업입니다.",
  "details": [
    { "field": "status", "reason": "PAID → SCREENING 전이는 허용되지 않습니다." }
  ]
}
```

### 1.3 오류 코드 매핑

**v1에서 상태 전이 위반이 500으로 나가던 문제를 여기서 교정한다.**

| 예외 | HTTP | `code` |
|---|---|---|
| 검증 실패 | 400 | `VALIDATION_FAILED` |
| 인증 없음 | 401 | `UNAUTHENTICATED` |
| 권한 없음 | 403 | `FORBIDDEN` |
| 청구 없음 | 404 | `CLAIM_NOT_FOUND` |
| **상태 전이 위반** | **409** | `CLAIM_ILLEGAL_TRANSITION` |
| 멱등키 재사용(본문 상이) | 422 | `IDEMPOTENCY_KEY_REUSED` |
| 멱등 처리 진행중 | 409 | `IDEMPOTENT_REQUEST_IN_PROGRESS` |
| 계약 스냅샷 확보 실패 | 202 | (오류 아님. 접수는 성공, 심사 보류) |
| 업스트림 장애 | 503 | `UPSTREAM_UNAVAILABLE` |
| 기타 | 500 | `INTERNAL_ERROR` |

> **500은 "우리가 예상 못 한 것"에만 쓴다.** 도메인 규칙 위반은 4xx다.

### 1.4 멱등성

상태 변경 요청은 `Idempotency-Key` 헤더 **필수**. 없으면 400.

```http
POST /api/v1/claims
Idempotency-Key: 7f3a9c21-8e44-4b1a-9f2c-1d5e8a7b3c90
```

---

## 2. 고객 API

### 2.1 청구 접수

```http
POST /api/v1/claims
Authorization: Bearer {token}
Idempotency-Key: {uuid}
Content-Type: application/json
```

```jsonc
{
  "policyNo": "P2026-0001234",
  "insuredRef": "CI-xxxx",
  "accident": {
    "accidentDate": "2026-03-14",
    "accidentType": "DISEASE",          // INJURY | DISEASE
    "primaryDiagnosis": "J20.9",
    "description": "급성 기관지염으로 통원 치료"
  },
  "treatments": [
    {
      "treatmentDate": "2026-03-14",
      "type": "OUTPATIENT",             // INPATIENT | OUTPATIENT | PRESCRIPTION
      "institution": {
        "code": "11100001",             // 요양기관번호
        "name": "○○대학교병원",
        "grade": "TERTIARY"             // CLINIC|HOSPITAL|GENERAL|TERTIARY|PHARMACY
      },
      "primaryDiagnosis": "J20.9",
      "secondaryDiagnoses": [],
      "charges": {
        "coveredPatientShare": 60000,   // 급여 본인부담금
        "coveredNhisShare": 120000,     // 급여 공단부담금 (정합성 검증용)
        "uncovered": 120000,            // 비급여
        "majorUncovered": 0             // 3대 비급여 (uncovered의 부분집합)
      }
    }
  ],
  "documents": [
    { "type": "RECEIPT",       "fileId": "FILE-abc123" },
    { "type": "ITEMIZED_BILL", "fileId": "FILE-abc124" }
  ],
  "payoutAccount": {
    "bankCode": "088",
    "accountNo": "110-123-456789",
    "holderName": "홍길동"
  }
}
```

**응답 `201 Created`**

```jsonc
{
  "claimNo": "CLM-20260402-000123",
  "status": "RECEIVED",
  "claimedAmount": 300000,
  "receivedAt": "2026-04-02T10:15:00+09:00",
  "paymentDueDate": {
    "dueDate": "2026-04-07",
    "businessDays": 3,
    "reason": "STANDARD_3BD"
  },
  "snapshotStatus": "SECURED"           // SECURED | PENDING | FAILED
}
```

**응답 `202 Accepted`** — business-support 장애로 스냅샷 미확보

```jsonc
{
  "claimNo": "CLM-20260402-000123",
  "status": "RECEIVED",
  "snapshotStatus": "PENDING",
  "message": "접수가 완료되었습니다. 계약 정보 확인 후 심사가 시작됩니다.",
  "paymentDueDate": { "dueDate": "2026-04-07", "businessDays": 3, "reason": "STANDARD_3BD" }
}
```

> **접수는 절대 막지 않는다.** 지급기한이 접수일부터 기산되므로, 접수 거부가 고객에게 더 불리하다.

**검증 규칙**

| 필드 | 규칙 |
|---|---|
| `policyNo` | 필수. 읽기모델에 존재해야 함 (없으면 404) |
| `accident.accidentDate` | 필수. 과거 또는 오늘 |
| `treatments` | 최소 1건 |
| `treatments[].treatmentDate` | ≥ `accidentDate` |
| `charges` | 각 항목 ≥ 0. 전부 0이면 400 |
| `payoutAccount` | 필수 |
| `documents` | `RECEIPT` 필수. 미충족 시 접수는 되나 `DOCS_REQUIRED`로 전환 |

### 2.2 청구 조회

```http
GET /api/v1/claims/{claimNo}
```

```jsonc
{
  "claimNo": "CLM-20260402-000123",
  "status": "PARTIALLY_APPROVED",
  "statusLabel": "일부승인",
  "policyNo": "P2026-0001234",
  "accidentDate": "2026-03-14",
  "claimedAmount": 300000,
  "payableAmount": 124000,
  "receivedAt": "2026-04-02T10:15:00+09:00",
  "paymentDueDate": { "dueDate": "2026-04-07", "businessDays": 3, "reason": "STANDARD_3BD" },

  "timeline": [
    { "type": "claim.received",     "at": "2026-04-02T10:15:00+09:00", "label": "청구 접수" },
    { "type": "claim.screening_started", "at": "2026-04-02T10:15:02+09:00", "label": "심사 시작" },
    { "type": "claim.adjudicated",  "at": "2026-04-02T10:15:03+09:00", "label": "심사 완료" }
  ],

  "settlement": {                       // 고객이 이해할 수 있는 산출 내역
    "lines": [
      {
        "coverageName": "급여 통원의료비",
        "base": 60000, "deductible": 20000, "payable": 40000,
        "note": "상급종합병원 최소공제금액 20,000원 적용"
      },
      {
        "coverageName": "비급여 통원의료비",
        "base": 120000, "deductible": 36000, "payable": 84000,
        "note": "자기부담금 30% 적용"
      }
    ],
    "excludedFromCoverage": [
      { "amount": 120000, "reason": "국민건강보험 공단부담금은 보상 대상이 아닙니다." }
    ],
    "total": 124000
  },

  "payout": { "status": "PENDING", "amount": 124000, "paidAt": null },
  "appeal": { "appealable": true, "deadline": "2026-07-02" }
}
```

> **`settlement`가 이 API의 핵심.** "왜 30만원 청구했는데 12만 4천원인가"에 답하지 못하면 민원이 된다.
> 내부 `BenefitLine`을 고객 언어로 번역해서 내려준다. 룰 ID는 노출하지 않는다.

### 2.3 청구 목록

```http
GET /api/v1/claims?status=PAID&from=2026-01-01&to=2026-12-31&page=0&size=20
```

```jsonc
{
  "content": [ { "claimNo": "...", "status": "...", "claimedAmount": 0, "payableAmount": 0, "receivedAt": "..." } ],
  "page": { "number": 0, "size": 20, "totalElements": 47, "totalPages": 3 }
}
```

### 2.4 서류 보완

```http
POST /api/v1/claims/{claimNo}/documents
Idempotency-Key: {uuid}
```

```jsonc
{ "documents": [ { "type": "DIAGNOSIS", "fileId": "FILE-abc125" } ] }
```

`DOCS_REQUIRED` → `RECEIVED` 전환 + **지급기한 시계 재개**.

### 2.5 청구 철회

```http
POST /api/v1/claims/{claimNo}/withdrawal
Idempotency-Key: {uuid}
```

```jsonc
{ "reason": "서류를 잘못 제출했습니다." }
```

`PAID` 상태면 `409`.

### 2.6 이의제기 (재심사)

```http
POST /api/v1/claims/{claimNo}/appeals
Idempotency-Key: {uuid}
```

```jsonc
{
  "reason": "부담보 부위와 무관한 상병입니다.",
  "documents": [ { "type": "DIAGNOSIS", "fileId": "FILE-abc130" } ]
}
```

→ `REOPENED`. 새 `Adjudication`이 생성되며 **기존 심사는 보존**된다.

---

## 3. 심사자 API (백오피스)

권한: `ROLE_ADJUSTER` 이상

### 3.1 심사 대기 큐

```http
GET /api/v1/adjuster/queue?referReason=R-RTE-010&sort=dueDate,asc&page=0
```

```jsonc
{
  "content": [
    {
      "claimNo": "CLM-20260402-000123",
      "referReasons": [ { "ruleId": "R-RTE-010", "label": "자동승인 한도 초과" } ],
      "claimedAmount": 3700000,
      "tentativePayable": 3700000,
      "dueDate": "2026-04-07",
      "remainingBusinessDays": 2,
      "slaRisk": "WARNING"                 // OK | WARNING | BREACHED
    }
  ]
}
```

**`dueDate` 오름차순이 기본 정렬.** 기한 임박 건이 위로 온다.

### 3.2 심사 상세 (트레이스 포함)

```http
GET /api/v1/adjuster/claims/{claimNo}/adjudications/{adjudicationId}
```

```jsonc
{
  "adjudicationId": "ADJ-20260402-000456",
  "mode": "AUTO",
  "decision": "REFERRED",
  "rulesetVersion": "2026.01",
  "snapshotChecksum": "sha256:9f2c...",
  "snapshotVerified": true,

  "benefitLines": [ /* 담보별 산출 */ ],

  "traces": [
    {
      "seq": 12, "ruleId": "R-CAL-020", "ruleName": "급여 자기부담금 차감",
      "clause": "실손의료보험 표준약관 제3조 제1항",
      "input":  { "claimBase": 60000, "coinsuranceRate": "0.20", "minDeductible": 20000 },
      "output": { "deductible": 20000, "payable": 40000 },
      "verdict": "APPLIED"
    }
  ],

  "policySnapshot": { /* 원문 */ },
  "claimHistory": { "last12MonthsCount": 4, "last12MonthsPaid": 820000 }
}
```

### 3.3 심사 확정

```http
POST /api/v1/adjuster/claims/{claimNo}/decision
Idempotency-Key: {uuid}
```

```jsonc
{
  "decision": "PARTIALLY_APPROVED",
  "benefitLines": [
    { "coverageCode": "COV-OUTP-COVERED",   "payable": 40000 },
    { "coverageCode": "COV-OUTP-UNCOVERED", "payable": 60000 }
  ],
  "overrideReason": "비급여 항목 중 도수치료 24,000원은 3대 비급여 특약 미가입으로 제외",
  "denialReasons": []
}
```

**검증**
- 자동 계산과 금액이 다르면 `overrideReason` **필수** (없으면 400)
- `DENIED`면 `denialReasons` 최소 1건 필수
- 심사자 ID·시각 자동 기록, `MANUAL_OVERRIDE` 트레이스 추가

### 3.4 서류 보완 요청

```http
POST /api/v1/adjuster/claims/{claimNo}/document-request
```

```jsonc
{
  "requiredDocuments": ["DIAGNOSIS", "ADMISSION_CERT"],
  "message": "입퇴원확인서와 진단서를 추가로 제출해 주세요.",
  "dueDate": "2026-04-16"
}
```

→ `DOCS_REQUIRED` + **지급기한 시계 정지** + 고객 알림 이벤트.

### 3.5 조사 연장 (3영업일 → 10영업일)

```http
POST /api/v1/adjuster/claims/{claimNo}/investigation
```

```jsonc
{ "reason": "타사 중복가입 확인 필요", "expectedCompletionDate": "2026-04-16" }
```

→ `dueDate` 재계산 + **고객 통지 이벤트 발행** (통지 누락 자체가 민원 사유).

---

## 4. 서비스 간 API

### 4.1 지급 결과 콜백

```http
POST /api/v1/internal/payments/{instructionId}/result
X-Service-Token: {token}
Idempotency-Key: {transferId}
```

```jsonc
{
  "result": "SUCCESS",                  // SUCCESS | FAILED | UNKNOWN
  "transferId": "TRF-20260405-998877",
  "processedAt": "2026-04-05T14:22:31+09:00",
  "failureCode": null
}
```

> `UNKNOWN`은 실패가 아니다. `PAID`로 전환하지 않고 조회 대사(reconciliation) 대상으로 둔다.

### 4.2 환수 개시

```http
POST /api/v1/internal/claims/{claimNo}/reclaim
```

```jsonc
{
  "reason": "OUT_OF_POCKET_CAP_REFUND",  // 본인부담상한제 환급
  "amount": 35000,
  "evidence": "2025년도 본인부담상한액 초과 환급 통보"
}
```

---

## 5. 관리 API

| 엔드포인트 | 용도 |
|---|---|
| `GET /actuator/health` | 헬스체크 (liveness/readiness 분리) |
| `GET /actuator/prometheus` | 지표 |
| `POST /api/v1/admin/outbox/{eventId}/republish` | Outbox 수동 재발행 |
| `POST /api/v1/admin/dlq/{messageId}/reprocess` | DLQ 재처리 |
| `GET /api/v1/admin/payments/unknown` | 결과 불명 이체 목록 |

> **`/health`를 직접 만들지 않는다.** v1은 별도 `HealthController`를 뒀는데,
> Actuator가 DB·Kafka·Redis 연결 상태까지 확인해 주므로 중복이고 품질도 낮다.

---

## 6. 페이지네이션·정렬 규약

- `page`(0-base), `size`(기본 20, 최대 100)
- `sort=field,direction` (예: `sort=dueDate,asc`)
- 목록 응답은 항상 `{ content, page }` 형태

---

## 7. 버전 정책

- 경로 버전 `/api/v1`
- **필드 추가는 하위호환** → 버전 유지
- 필드 삭제·의미 변경 → `/api/v2` 신설, `v1`은 `Deprecation`/`Sunset` 헤더로 예고 후 6개월 유지

---

## 다음 문서

- [`06-data-model.md`](06-data-model.md) — 스키마
- [`07-architecture.md`](07-architecture.md) — 모듈 구조
