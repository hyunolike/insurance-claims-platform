# CLAUDE.md

Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 지침.

## 프로젝트

실손의료보험 청구 자동화 백엔드. 2개 저장소로 구성된 시스템의 **보상(Claims) 측**.

- 짝 저장소: `hyunolike/insurance-business-support` (계약·언더라이팅 = 계약 정보의 원천)
- 스택: Java 21 LTS, Spring Boot 3.x, PostgreSQL 15, Kafka, Redis
- 아키텍처: DDD + 헥사고날, Gradle 멀티모듈, Transactional Outbox
- **현재 상태: Phase 0(골격) 완료. business-support Phase 1(스냅샷 API)이 완료되어 Phase 2를 시작할 수 있다**

## 작업 전 반드시 읽을 것

| 문서 | 언제 |
|---|---|
| `docs/design/00-domain-glossary.md` | **항상.** 용어를 새로 만들기 전에 여기 있는지 확인 |
| `docs/design/01-context-map.md` | business-support와 연동되는 작업 |
| `docs/design/02-domain-model.md` | 애그리거트·상태 변경 작업 |
| `docs/design/03-adjudication.md` | 심사 룰·금액 계산 작업 |
| `docs/design/07-architecture.md` | 모듈 추가·의존성 변경 |
| `docs/design/08-roadmap.md` | 지금 어느 Phase인지, 완료 조건이 뭔지 |

`docs/archive/v1/`은 폐기된 이전 설계다. **참조하지 말 것.**

## 절대 규칙

이 규칙들은 v1에서 실제로 무너진 것들이다. 빌드가 막도록 되어 있지만, 우회하지 말 것.

1. **`claims-domain`에 Spring/JPA/Jackson을 넣지 않는다.** 클래스패스에 없다. 필요해 보이면 설계가 틀린 것이다.
2. **`claims-application`에서 `ApplicationEventPublisher`를 쓰지 않는다.** 도메인 이벤트는 애그리거트가 `record()`하고 `OutboxAppender`로 저장한다.
3. **`@Transactional` 안에서 외부로 이벤트를 발행하지 않는다.** Outbox 테이블에 INSERT만 한다.
4. **민감정보를 로그·이벤트 페이로드에 넣지 않는다.** KCD 코드, 성명, 계좌번호, 진단명. 민감 타입은 `toString()`이 이미 마스킹돼 있으니 그대로 로깅하면 된다. `reveal*()` 반환값을 로깅하지 말 것.
5. **금액은 `Money` VO로만.** 원 단위 정수. `BigDecimal`을 도메인 필드로 쓰지 않는다.
6. **테스트에 H2를 쓰지 않는다.** Testcontainers PostgreSQL + Flyway 실행 + `ddl-auto: validate`.
7. **`rule_trace`와 `policy_snapshot`은 수정·삭제하지 않는다.** 재심사는 새 `Adjudication`을 만든다.
8. **심사는 `PolicySnapshot`만 본다.** `policy_replica`(읽기모델)로 판정하지 않는다.
9. **비밀값에 기본값을 주지 않는다.** 미설정 시 기동 실패가 정상이다.
10. **부지급에는 반드시 `DenialReason` 코드가 붙는다.** 메서드 시그니처로 강제되어 있다.

## 명령어

```bash
# 인프라 (compose.yaml — PostgreSQL, Redis, KRaft Kafka)
docker compose up -d
docker compose down -v

# 빌드·테스트
./gradlew clean build
./gradlew :claims-domain:test                              # 도메인 단위 (빠름, Spring 없음)
./gradlew test --tests '*ArchitectureTest'                 # 아키텍처 규칙
./gradlew jacocoTestCoverageVerification                   # 커버리지 게이트

# Phase 3부터
./gradlew :claims-rules:test --tests '*GoldenCaseTest'     # 심사 골든 케이스
# Phase 6부터
./gradlew generateOpenApiDocs                              # OpenAPI 갱신

# 실행
./gradlew :claims-bootstrap:bootRun
```

## 기능 추가 순서

항상 **도메인부터** 간다. 컨트롤러부터 만들지 않는다.

```
1. claims-domain        모델 + 불변식 + 이벤트 record()  → 순수 단위 테스트
2. claims-domain/port   필요한 포트 인터페이스 정의
3. claims-rules         룰이 추가되면 여기 + 골든 케이스
4. claims-application   유스케이스 + @Transactional 경계 + Outbox
5. claims-adapter-*     JPA 엔티티/어댑터, 컨트롤러      → Testcontainers, MockMvc
6. docs                 설계와 달라졌으면 설계 문서를 먼저 고친다
```

## 심사 룰을 추가·수정할 때

1. `docs/design/03-adjudication.md`의 룰 카탈로그에 **먼저** 등록 (ID 규칙: `R-{단계}-{번호}`)
2. 임계값·요율은 코드가 아니라 `ruleset_parameter` 테이블에
3. `RuleTrace`에 입력·출력·근거 조항을 남긴다
4. 골든 케이스 추가
5. **기존 골든 케이스가 깨지면 의도한 변경인지 반드시 확인** — 조용히 금액이 바뀌는 것을 막는 장치다
6. 애매하면 부지급이 아니라 **회부(`REFER`)**

## 커밋·PR

```
<type>: <내용> #<이슈번호>
feat · fix · docs · refactor · test · chore
```

- 브랜치: `feat/#이슈`, `fix/#이슈`, `docs/#이슈`, `refactor/#이슈`
- `main`/`develop` 직접 푸시 금지
- PR 머지 전 CI 전체 통과 필수 (build, ArchUnit, 커버리지, 골든 케이스, OpenAPI drift)
- 해당 Phase의 완료 조건 체크리스트를 PR 본문에 복사해 체크

## 현재 구현 상태

```
Phase 0  골격 (멀티모듈, ArchUnit, Testcontainers, CI)   ☑ 완료
Phase 1  BS: 계약 모델 + 스냅샷 API                      ☑ (다른 저장소 — 선행 조건 충족)
Phase 2  청구 접수 + 스냅샷 연동                          ☐  ← 다음
Phase 3  심사 엔진                                        ☐
Phase 4  지급 + Outbox/Kafka                              ☐
Phase 5  BS: 청약·언더라이팅                              ☐ (다른 저장소)
Phase 6  운영 강화 (암호화, 감사, 관측성)                 ☐
```

**Phase 0에서 실제로 만들어진 것**

```
claims-domain/          shared/  DomainEvent · EventId(ULID) · AggregateRoot
                        shared/vo/  Money (원 단위 정수)
                        policy/  SnapshotChecksum (BS와 합의한 알고리즘)
claims-application/     port/out/  OutboxAppender
claims-adapter-persistence/  outbox/  Entity · Repository · AppenderAdapter
                             db/migration/V1__baseline_infrastructure.sql
claims-bootstrap/       ClaimsApplication · SecurityConfig
                        test/  ArchitectureTest · FlywayMigrationTest
                               OutboxAppenderIntegrationTest · IntegrationTestBase
                               contract/  SnapshotChecksumContractTest (소비자 쪽)
나머지 모듈              package-info.java 로 책임만 문서화 (Phase 2~4에서 채움)
```

`claims-rules`, `claims-adapter-web/messaging/policy/external` 이 비어 있는 것은
정상이다. ArchUnit의 `withOptionalLayers(true)` / `allowEmptyShould(true)` 가
그 사실을 명시하고 있고, 해당 Phase에서 채우면서 함께 걷어낸다.

Phase를 완료하면 이 표와 `docs/design/08-roadmap.md`를 함께 갱신한다.

## 주의

약관 수치(자기부담률·최소공제금액·한도·지급기한)는 **설계 예시**이며 실제 약관으로 검증되지 않았다.
수치를 코드에 하드코딩하지 말고 `ruleset_parameter`로 관리한다.
