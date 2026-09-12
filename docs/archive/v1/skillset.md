## 기술 스택 선정
### 1. core stack
| 영역        | 기술             | 선택 이유       |
| --------- | -------------- | ----------- |
| Language  | Java 17        | 토스 표준       |
| Framework | Spring Boot    | 생태계, 안정성    |
| Build     | Gradle         | 대규모 프로젝트 표준 |
| DB        | PostgreSQL     | 금융권 선호      |
| Cache     | Redis          | 분산락, 캐시     |
| Messaging | Kafka          | 이벤트 중심 아키텍처 |
| ORM       | JPA + QueryDSL | 가독성 + 성능    |
| Metrics   | Micrometer     | 지표 수집       |
| Logging   | Logback + JSON | 로그 표준화      |

### 2. 추가
| 평가 항목    | 기술                   |
| -------- | -------------------- |
| 내부 구현 이해 | HikariCP, Jackson    |
| 장애 대응    | Kafka DLQ            |
| 성능       | Redis Cache          |
| 측정       | Prometheus + Grafana |
| 기록       | ADR 문서               |