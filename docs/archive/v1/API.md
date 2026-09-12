## 보험금 청구 자동화 API

문서의 마인드맵·SMART·ERD 기반으로 모바일 앱/상담사/내부 서비스가 사용하는 대표 API를 정의했다. REST 중심이며 이벤트 기반 후속 처리는 Kafka에 위임한다.

### 1. 청구 생성 (Create Claim)
- **HTTP**: `POST /claims`
- **권한**: 고객 인증 토큰
- **요청**
```json
{
  "policyId": "uuid",
  "claimedAmount": 250000,
  "documents": [
    {"type": "RECEIPT", "url": "https://..."}
  ],
  "accidentAt": "2024-07-01T10:12:00Z"
}
```
- **응답**
```json
{
  "claimId": "uuid",
  "status": "REQUESTED",
  "submittedAt": "2024-07-01T10:13:00Z"
}
```
- **비고**: 성공 시 `claim.requested` 이벤트 발행, SMART 지표인 처리 시간 측정을 위해 `submittedAt` 저장.

### 2. 청구 상태 조회 (Get Claim Status)
- **HTTP**: `GET /claims/{claimId}`
- **권한**: 고객 또는 상담사
- **응답**
```json
{
  "claimId": "uuid",
  "status": "IN_REVIEW",
  "latestEvent": {
    "type": "claim.reviewed",
    "occurredAt": "2024-07-02T02:00:00Z"
  },
  "timeline": [
    {"type": "claim.requested", "occurredAt": "..."},
    {"type": "claim.reviewed", "occurredAt": "..."}
  ],
  "payout": {
    "amount": 230000,
    "status": "PENDING"
  }
}
```
- **비고**: `timeline`은 `ClaimEvent`를 기반으로 구성, 고객 신뢰 지표(정보 투명성)를 만족.

### 3. 심사 결정 등록 (Submit Review Decision)
- **HTTP**: `POST /claims/{claimId}/review`
- **권한**: 상담사 혹은 자동 Rule Engine Service-to-Service
- **요청**
```json
{
  "decision": "APPROVED",
  "reason": "자동 심사 규칙 #12",
  "reviewer": "rule-engine"
}
```
- **응답**
```json
{
  "claimId": "uuid",
  "status": "APPROVED",
  "decision": {
    "approved": true,
    "decidedAt": "2024-07-02T02:00:00Z"
  }
}
```
- **비고**: 결과 저장 후 `claim.reviewed` 이벤트 발행. APPROVED 결정 시 추가로 `claim.approved` 이벤트를 발행해 Payment Service를 트리거. Rule Engine과 Claim Service 간 멱등성을 위해 `If-Match` 헤더 사용 권장.

### 4. 지급 상태 업데이트 (Update Payout)
- **HTTP**: `POST /claims/{claimId}/payout`
- **권한**: 지급 서비스
- **요청**
```json
{
  "amount": 230000,
  "method": "ACCOUNT_TRANSFER",
  "payoutStatus": "SUCCESS"
}
```
- **응답**
```json
{
  "claimId": "uuid",
  "status": "DISBURSED",
  "payout": {
    "amount": 230000,
    "method": "ACCOUNT_TRANSFER",
    "processedAt": "2024-07-03T05:00:00Z"
  }
}
```
- **비고**: 지급 완료 시 `claim.disbursed` 이벤트 발행, Observability에서 SLA 추적.

### 5. 알림 기록 조회 (List Notifications)
- **HTTP**: `GET /claims/{claimId}/notifications`
- **권한**: 고객/상담사
- **응답**
```json
[
  {
    "notificationId": "uuid",
    "channel": "PUSH",
    "template": "CLAIM_SUBMITTED",
    "sentAt": "2024-07-01T10:14:00Z",
    "deliveryStatus": "DELIVERED"
  }
]
```
- **비고**: 고객 신뢰/투명성 확보와 재오픈율 감소를 위한 피드백 루프.

### 공통 고려사항
- 모든 응답은 `traceId` 헤더를 포함해 Observability 스택과 연동.
- 실패 시 JSON API 에러 포맷 사용:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "policyId is required"
}
```
- 이벤트 스키마(`ClaimEvent`)와 API 응답 필드를 최대한 일치시켜 재처리/디버깅을 단순화한다.

### API 간 프로세스 관계
1. `POST /claims`로 청구를 생성하면 `claim.requested` 이벤트가 발행되고, 이후 모든 단계의 기준 타임라인이 시작된다.
2. `GET /claims/{id}`는 `ClaimEvent` 타임라인을 실시간 조회해 생성/심사/지급 결과를 통합 제공한다.
3. `POST /claims/{id}/review`는 Rule Engine·상담사가 호출해 승인/거부를 기록하고 `claim.reviewed` 이벤트를 발행한다. 승인 결정 시 `claim.approved` 이벤트를 추가 발행해 Payment Service를 트리거한다.
4. `POST /claims/{id}/payout`은 승인된 청구에 대해 지급 상태를 업데이트하며 `claim.disbursed` 이벤트로 상태·알림·관측 지표를 갱신한다.
5. `GET /claims/{id}/notifications`는 앞선 이벤트에서 발송된 알림 히스토리를 조회해 고객 신뢰/재오픈율 관리 지표로 활용한다.

### 이벤트 흐름 요약
```
claim.requested → claim.reviewed → claim.approved → claim.disbursed
```
