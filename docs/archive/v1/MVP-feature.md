## 구현 범위 정하기 (MVP 기준)
### 1. 범위
- 핵심 플로우 1개 집중

```
보험금 청구 → 심사 → 지급 → 알림
```

### 2. 기능 목록
#### 사용자 관점
- 보험금 청구 등록
- 청구 상태 조회

#### 시스템 관점
- 이벤트 기반 처리
- 중복 지급 방지
- 장애 재처리
- 개인정보 암호화

#### 운영 관점
- 지표 수집
- 로그/감사 추적
- 장애 시나리오 문서화

---
## 서비스 아키텍처
```
[API Layer]
 ├─ Claim API
 ├─ Payment API
 └─ Query API

[Application Layer]
 ├─ Claim Service
 ├─ Payment Service
 ├─ Rule Engine Service
 ├─ Notification Service
 └─ Event Publisher

[Infrastructure Layer]
 ├─ Kafka (Producer / Consumer)
 ├─ Redis (Cache / Distributed Lock)
 ├─ Encryption Module
 └─ PostgreSQL (Persistence)
```

## 이벤트 흐름 아키텍처
```
claim.requested
   ↓
claim.reviewed
   ↓
claim.approved
   ↓
claim.disbursed
```

- 각 단계는 독립 이벤트
- 장애 발생 시 해당 단계만 재처리 가능
- snake_case 명명 규칙 사용