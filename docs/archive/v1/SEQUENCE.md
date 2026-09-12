## 보험금 청구 자동화 시퀀스

API 문서의 주요 흐름(`POST /claims` → `POST /claims/{id}/review` → `POST /claims/{id}/payout` → 알림/상태 조회)을 이벤트 기반 구조와 함께 표현했다.

### 1. 청구 생성 & 이벤트 발행
```mermaid
sequenceDiagram
    participant UserApp
    participant APIGW as API Gateway
    participant ClaimSvc as Claim Service
    participant RDB
    participant Kafka
    participant Notification

    UserApp->>APIGW: POST /claims
    APIGW->>ClaimSvc: Forward request
    ClaimSvc->>RDB: Insert Claim & Documents
    RDB-->>ClaimSvc: OK
    ClaimSvc->>UserApp: 201 Created (claimId, status=REQUESTED)
    ClaimSvc->>Kafka: publish claim.requested
    Kafka-->>Notification: consume claim.requested
    Notification->>UserApp: Push "청구 접수 완료"
```

### 2. 자동 심사/예외 처리
```mermaid
sequenceDiagram
    participant RuleEngine as Rule Engine Service
    participant ClaimSvc as Claim Service
    participant Kafka
    participant PaymentSvc as Payment Service
    participant Audit

    RuleEngine->>ClaimSvc: POST /claims/{id}/review
    ClaimSvc->>ClaimSvc: Apply decision & update status
    ClaimSvc->>Kafka: publish claim.reviewed
    alt Decision is APPROVED
        ClaimSvc->>Kafka: publish claim.approved
        Kafka-->>PaymentSvc: trigger payment process
    end
    Kafka-->>Audit: consume & log event
    ClaimSvc-->>RuleEngine: decision result JSON
```

### 3. 지급 및 알림
```mermaid
sequenceDiagram
    participant PaymentSvc as Payment Service
    participant ClaimSvc as Claim Service
    participant Kafka
    participant Notification
    participant Observability

    PaymentSvc->>ClaimSvc: POST /claims/{id}/payout
    ClaimSvc->>ClaimSvc: Update payout record
    ClaimSvc->>Kafka: publish claim.disbursed
    Kafka-->>Notification: send payout success template
    Kafka-->>Observability: metrics/trace ingestion
    ClaimSvc-->>PaymentSvc: Payout summary JSON
```

### 활용 메모
- 모든 API 응답에는 `traceId` 헤더를 포함, Observability가 시퀀스 전반을 추적.
- 실패 시 서비스는 에러 응답과 함께 `claim.failed` 이벤트를 발행해 재처리 큐에 적재.
- `Timeline` 조회(`GET /claims/{id}`)는 `ClaimEvent` 스트림을 기반으로 캐시를 갱신해 실시간 상태 표시를 보장.

### 전체 이벤트 흐름
```
claim.requested → claim.reviewed → claim.approved → claim.disbursed
```
