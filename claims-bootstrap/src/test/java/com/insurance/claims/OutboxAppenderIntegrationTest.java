package com.insurance.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insurance.claims.application.port.out.OutboxAppender;
import com.insurance.claims.domain.shared.DomainEvent;
import com.insurance.claims.domain.shared.EventId;
import com.insurance.claims.support.IntegrationTestBase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Outbox 적재의 트랜잭션 원자성을 검증한다.
 *
 * <p>v1의 가장 심각한 기술 부채는 {@code @Transactional} 안에서
 * {@code ApplicationEventPublisher.publishEvent()}를 호출한 것이었다.
 * 리스너가 커밋 전에 실행되므로, Kafka 발행으로 바꾸는 순간 롤백된 트랜잭션의
 * 이벤트가 외부로 나간다. 여기서 검증하는 것이 바로 그 시나리오다.
 */
@DisplayName("Outbox 적재")
class OutboxAppenderIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OutboxAppender outboxAppender;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("커밋되면 이벤트가 남는다")
    void shouldPersistOnCommit() {
        TestEvent event = TestEvent.of("CLM-20260402-000001");

        transactionTemplate.executeWithoutResult(status ->
                outboxAppender.append(List.of(event)));

        assertThat(countByEventId(event.eventId().value())).isEqualTo(1);
    }

    @Test
    @DisplayName("★ 롤백되면 이벤트도 사라진다 — 커밋 전 발행이었다면 외부로 이미 나갔을 것")
    void shouldVanishOnRollback() {
        TestEvent event = TestEvent.of("CLM-20260402-000002");

        assertThatThrownBy(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    outboxAppender.append(List.of(event));
                    throw new IllegalStateException("심사 도중 실패");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(countByEventId(event.eventId().value()))
                .as("상태 변경과 이벤트는 원자적으로 함께 커밋되거나 함께 사라져야 한다")
                .isZero();
    }

    @Test
    @DisplayName("봉투에 이벤트 메타데이터가 담긴다")
    void shouldBuildEnvelope() {
        TestEvent event = TestEvent.of("CLM-20260402-000003");

        transactionTemplate.executeWithoutResult(status ->
                outboxAppender.append(List.of(event)));

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        var row = jdbc.queryForMap("""
                SELECT event_type, aggregate_type, aggregate_id, partition_key, status,
                       attempts, envelope::text AS envelope
                FROM outbox_event WHERE event_id = ?
                """, event.eventId().value());

        assertThat(row.get("event_type")).isEqualTo("claim.received");
        assertThat(row.get("aggregate_type")).isEqualTo("Claim");
        assertThat(row.get("aggregate_id")).isEqualTo("CLM-20260402-000003");
        assertThat(row.get("partition_key"))
                .as("파티션 키는 애그리거트 ID다. 같은 청구의 이벤트 순서가 보장된다.")
                .isEqualTo("CLM-20260402-000003");
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("attempts")).isEqualTo(0);
        assertThat((String) row.get("envelope"))
                .contains("\"producer\":\"claims-platform\"")
                .contains("\"eventType\":\"claim.received\"")
                // 페이로드는 이벤트가 손으로 만든 것이 그대로 실린다.
                // 리플렉션 직렬화 시절에는 EventId에 게터가 없어 여기서 전부 터졌다.
                .contains("\"payload\":{\"claimNo\":\"CLM-20260402-000003\"}");
    }

    @Test
    @DisplayName("빈 목록은 아무것도 적재하지 않는다")
    void shouldIgnoreEmptyInput() {
        long before = totalCount();

        transactionTemplate.executeWithoutResult(status -> {
            outboxAppender.append(List.of());
            outboxAppender.append(null);
        });

        assertThat(totalCount()).isEqualTo(before);
    }

    private int countByEventId(String eventId) {
        Integer count = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_id = ?", Integer.class, eventId);
        return count == null ? 0 : count;
    }

    private long totalCount() {
        Long count = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM outbox_event", Long.class);
        return count == null ? 0L : count;
    }

    /** Phase 2에서 실제 ClaimReceived로 대체된다. */
    record TestEvent(EventId eventId, String aggregateId, Instant occurredAt)
            implements DomainEvent {

        static TestEvent of(String claimNo) {
            return new TestEvent(EventId.generate(), claimNo, Instant.now());
        }

        @Override
        public String eventType() {
            return "claim.received";
        }

        @Override
        public Map<String, Object> payload() {
            return Map.of("claimNo", aggregateId);
        }
    }
}
