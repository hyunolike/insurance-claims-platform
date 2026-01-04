# CLAUDE.MD - Insurance Claim Automation System

> Project context and guidelines for AI-assisted development with Claude Code

## Project Overview

**보험금 청구 자동화 시스템** - Event-driven backend system automating insurance claim processing from submission to disbursement.

- **Language**: Java 24
- **Framework**: Spring Boot
- **Architecture**: Event-driven with Kafka, Redis caching, PostgreSQL persistence
- **Timeline**: 2025-12-28 ~ 2026-01-12 (16 days)
- **Current Phase**: Phase 1 Complete - Claim Create/Query APIs implemented

## Project Structure

```
toss/
├── src/main/java/com/insurance/claim/
│   ├── api/              # REST API Controllers & DTOs
│   ├── application/      # Application Services & Business Logic
│   ├── domain/           # Domain Models, VOs, Repositories (interfaces)
│   ├── infrastructure/   # Adapters (JPA, Kafka, Redis)
│   └── config/           # Spring Configuration
├── src/test/java/        # Unit & Integration Tests
├── docs/                 # Architecture & Planning Documents
└── docker-compose.yml    # Local infrastructure (Kafka, Redis, PostgreSQL)
```

### Layer Responsibilities

- **api**: Controllers, request/response DTOs, exception handlers
- **application**: Service layer, use case orchestration, DTO mappers
- **domain**: Core business entities, value objects, repository interfaces (DDD)
- **infrastructure**: Implementation of domain repositories, external integrations

## Tech Stack

| Category | Technology | Port/Details |
|----------|-----------|--------------|
| Language | Java 24 | - |
| Framework | Spring Boot 3.x | - |
| Database | PostgreSQL 15 | 5432 |
| Cache | Redis 7 | 6379 |
| Messaging | Apache Kafka 7.5 | 9092 |
| ORM | Spring Data JPA | - |
| Build | Gradle 7.x | - |
| Testing | JUnit 5, Mockito | - |

## Development Environment

### Running Infrastructure

```bash
# Start all services (Kafka, Redis, PostgreSQL)
docker compose up -d

# Stop and clean up
docker compose down -v
```

### Database Credentials
- Host: localhost:5432
- Database: `claim`
- Username: `claim`
- Password: `changeme`

### Kafka
- Bootstrap: localhost:9092
- UI: http://localhost:8081

### Application

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Test
./gradlew test
```

## Current Implementation Status

### ✅ Phase 1 Complete (M1: 2025-12-31)
- [x] Domain model: Claim, ClaimStatus, Value Objects (Money, ClaimNumber)
- [x] JPA infrastructure with ClaimEntity and ClaimJpaRepository
- [x] Application service: ClaimService
- [x] REST API: POST /claims, GET /claims/{id}
- [x] Basic logging with RequestResponseLoggingAspect
- [x] Unit tests for domain and service layers

### Current Architecture Highlights

**Domain Layer** (DDD):
- `Claim`: Aggregate root with business logic
- `ClaimNumber`: Value object with auto-generation (CLM-YYYYMMDD-XXXXX)
- `Money`: Value object for currency handling
- `ClaimStatus`: Enum (SUBMITTED, IN_REVIEW, APPROVED, REJECTED, PAID)

**API Endpoints**:
- `POST /claims`: Create new claim
- `GET /claims/{id}`: Query claim by ID

### 🚧 Upcoming Phases

**Phase 2** (Target: 2026-01-02): Event-driven structure (synchronous)
- Domain events for state transitions
- Event sourcing foundation

**Phase 3** (Target: 2026-01-05): Kafka async processing
- Event publisher/consumer setup
- claim.requested, claim.reviewed events

**Phase 4** (Target: 2026-01-09): Review + Payment automation (MVP)
- Rule Engine service
- Payment service
- Full claim → review → payment flow

**Phase 5** (Target: 2026-01-12): Notification + Observability
- Notification service
- Metrics (Prometheus), distributed tracing

## Key Documentation

- [README.md](README.md): High-level project overview
- [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md): Technical decisions, risk management
- [docs/WBS.md](docs/WBS.md): Detailed work breakdown, Gantt chart
- [docs/PACKAGE_STRUCTURE.md](docs/PACKAGE_STRUCTURE.md): Layer-by-layer structure guide
- [docs/API.md](docs/API.md): API specifications
- [docs/ERD.md](docs/ERD.md): Database schema
- [docs/SEQUENCE.md](docs/SEQUENCE.md): Sequence diagrams

## Design Principles

### Event-Driven Architecture
- All state changes publish events
- Loose coupling between services
- Fault isolation and independent deployment

### Idempotency
- API calls must be safe for retries
- Use optimistic locking (If-Match headers)
- Kafka message reprocessing safety

### Data Protection
- Encrypt sensitive personal data
- Audit logging for all operations
- Minimal access privileges

### Observability
- Every API response includes `traceId`
- Distributed tracing across services
- Real-time SMART metrics monitoring

## Coding Conventions

### Commit Messages
```
<type>: <description>

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation
- refactor: Code improvement without behavior change
- test: Test-related work
- chore: Build/dependency maintenance
```

### Package Naming
- Follow existing structure: `com.insurance.claim.<layer>.<feature>`
- Keep layer boundaries clean (no circular dependencies)
- Domain layer must not depend on infrastructure

### Testing
- Unit tests for domain logic (100% coverage goal)
- Integration tests for API endpoints
- Use test fixtures and builders for readability

## Common Tasks

### Adding a New API Endpoint
1. Define request/response DTOs in `api/dto/`
2. Implement controller in `api/controller/`
3. Add service method in `application/service/`
4. Update domain if needed
5. Write integration tests

### Adding a New Domain Entity
1. Create entity in `domain/model/`
2. Define repository interface in `domain/repository/`
3. Implement JPA entity in `infrastructure/persistence/`
4. Implement repository adapter
5. Write unit tests

### Adding Kafka Event
1. Define event class in `domain/event/` (to be created)
2. Configure Kafka topic in application.yml
3. Implement publisher in `infrastructure/messaging/`
4. Implement consumer if needed
5. Test with Kafka UI (localhost:8081)

## Important Notes for Claude Code

1. **Follow DDD principles**: Keep domain pure, infrastructure adapts to domain
2. **Maintain layer separation**: API → Application → Domain ← Infrastructure
3. **Write tests first**: Domain tests → Service tests → API tests
4. **Use existing patterns**: Follow ClaimService, ClaimController examples
5. **Check documentation**: Consult docs/ before major changes
6. **Event-driven mindset**: Think in terms of events for state changes
7. **Security**: Never expose internal IDs in API responses, use business identifiers

## Contact & Resources

- **Project Tracking**: See docs/WBS.md for current sprint status
- **Architecture Questions**: Refer to docs/IMPLEMENTATION_PLAN.md
- **API Design**: Follow patterns in docs/API.md

---

**Last Updated**: 2026-01-04
**Current Branch**: nostalgic-germain
**Project Phase**: Phase 1 Complete, Preparing Phase 2
