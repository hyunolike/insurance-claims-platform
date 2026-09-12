package com.insurance.claims;

import static org.assertj.core.api.Assertions.assertThat;

import com.insurance.claims.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 마이그레이션이 실제 PostgreSQL에서 실행되는지 검증한다.
 *
 * <p>v1에서는 이 테스트가 없었고, 그래서 마이그레이션 SQL이 운영에 배포될 때까지
 * 한 번도 실행되지 않았다.
 */
@DisplayName("Flyway 마이그레이션")
class FlywayMigrationTest extends IntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("V1이 적용되고 기반 테이블이 생성된다")
    void shouldApplyBaselineMigration() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                String.class);
        assertThat(applied).contains("1");

        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public'
                """, String.class);
        assertThat(tables)
                .contains("outbox_event", "processed_event", "idempotency_record");
    }

    @Test
    @DisplayName("Outbox 부분 인덱스가 만들어진다 — PENDING만 인덱싱해 릴레이 폴링 비용을 낮춘다")
    void shouldCreatePartialIndex() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        String definition = jdbc.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE tablename = 'outbox_event' AND indexname = 'idx_outbox_pending'
                """, String.class);

        assertThat(definition)
                .as("부분 인덱스는 PostgreSQL 고유 기능이다. H2였다면 이 검증이 불가능했다.")
                .contains("WHERE")
                .contains("PENDING");
    }

    @Test
    @DisplayName("status 제약이 잘못된 값을 거부한다")
    void shouldEnforceStatusCheckConstraint() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> violations = new ArrayList<>();

        try {
            jdbc.update("""
                    INSERT INTO outbox_event
                        (event_id, event_type, aggregate_type, aggregate_id,
                         partition_key, envelope, occurred_at, status)
                    VALUES ('01JBX7K3QM8W2ZP4NRTV9C6DY0', 'test.event', 'Test', 'T-1',
                            'T-1', '{}'::jsonb, NOW(), 'NOT_A_VALID_STATUS')
                    """);
            violations.add("잘못된 status가 INSERT되었습니다");
        } catch (Exception expected) {
            // CHECK 제약이 거부하는 것이 정상이다
        }

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("event_id 유니크 제약이 중복 적재를 막는다")
    void shouldRejectDuplicateEventId() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String eventId = "01JBX7K3QM8W2ZP4NRTV9C6DY1";

        jdbc.update("""
                INSERT INTO outbox_event
                    (event_id, event_type, aggregate_type, aggregate_id,
                     partition_key, envelope, occurred_at)
                VALUES (?, 'test.event', 'Test', 'T-1', 'T-1', '{}'::jsonb, NOW())
                """, eventId);

        List<String> violations = new ArrayList<>();
        try {
            jdbc.update("""
                    INSERT INTO outbox_event
                        (event_id, event_type, aggregate_type, aggregate_id,
                         partition_key, envelope, occurred_at)
                    VALUES (?, 'test.event', 'Test', 'T-1', 'T-1', '{}'::jsonb, NOW())
                    """, eventId);
            violations.add("중복 event_id가 INSERT되었습니다");
        } catch (Exception expected) {
            // UNIQUE 제약이 거부하는 것이 정상이다
        }

        assertThat(violations).isEmpty();
    }
}
