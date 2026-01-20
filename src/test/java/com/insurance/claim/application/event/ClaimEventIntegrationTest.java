package com.insurance.claim.application.event;

import com.insurance.claim.application.service.ClaimService;
import com.insurance.claim.domain.event.ClaimApprovedEvent;
import com.insurance.claim.domain.event.ClaimCreatedEvent;
import com.insurance.claim.domain.event.ClaimInReviewEvent;
import com.insurance.claim.domain.event.ClaimPaidEvent;
import com.insurance.claim.domain.event.ClaimRejectedEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("청구 도메인 이벤트 통합 테스트")
@org.springframework.context.annotation.Import(ClaimEventIntegrationTest.TestEventCollector.class)
class ClaimEventIntegrationTest {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private TestEventCollector testEventCollector;

    @BeforeEach
    void setUp() {
        testEventCollector.clear();
    }

    @Test
    @Transactional
    @DisplayName("청구 생성 시 ClaimCreatedEvent가 발행되어야 한다")
    void shouldPublishClaimCreatedEvent() {
        // Given
        ClaimService.CreateClaimCommand command = new ClaimService.CreateClaimCommand(
                "POL-12345",
                LocalDate.of(2025, 1, 10),
                "자동차 사고",
                BigDecimal.valueOf(1_000_000),
                "홍길동",
                "hong@example.com"
        );

        // When
        claimService.createClaim(command);

        // Then
        List<ClaimCreatedEvent> events = testEventCollector.getEventsOfType(ClaimCreatedEvent.class);
        assertThat(events).hasSize(1);

        ClaimCreatedEvent event = events.get(0);
        assertThat(event.getClaimNumber()).isNotNull();
        assertThat(event.getPolicyNumber()).isEqualTo("POL-12345");
        assertThat(event.getClaimAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        assertThat(event.getClaimantName()).isEqualTo("홍길동");
        assertThat(event.eventType()).isEqualTo("claim.created");
    }

    @Test
    @Transactional
    @DisplayName("청구 심사 시작 시 ClaimInReviewEvent가 발행되어야 한다")
    void shouldPublishClaimInReviewEvent() {
        // Given
        ClaimService.CreateClaimCommand command = new ClaimService.CreateClaimCommand(
                "POL-12345",
                LocalDate.of(2025, 1, 10),
                "자동차 사고",
                BigDecimal.valueOf(1_000_000),
                "홍길동",
                "hong@example.com"
        );
        Long claimId = claimService.createClaim(command).getClaimId();
        testEventCollector.clear(); // 생성 이벤트 제거

        // When
        claimService.markInReview(claimId);

        // Then
        List<ClaimInReviewEvent> events = testEventCollector.getEventsOfType(ClaimInReviewEvent.class);
        assertThat(events).hasSize(1);

        ClaimInReviewEvent event = events.get(0);
        assertThat(event.getClaimId()).isEqualTo(claimId);
        assertThat(event.eventType()).isEqualTo("claim.in_review");
    }

    @Test
    @Transactional
    @DisplayName("청구 승인 시 ClaimApprovedEvent가 발행되어야 한다")
    void shouldPublishClaimApprovedEvent() {
        // Given
        ClaimService.CreateClaimCommand command = new ClaimService.CreateClaimCommand(
                "POL-12345",
                LocalDate.of(2025, 1, 10),
                "자동차 사고",
                BigDecimal.valueOf(1_000_000),
                "홍길동",
                "hong@example.com"
        );
        Long claimId = claimService.createClaim(command).getClaimId();
        testEventCollector.clear();

        // When
        claimService.approve(claimId);

        // Then
        List<ClaimApprovedEvent> events = testEventCollector.getEventsOfType(ClaimApprovedEvent.class);
        assertThat(events).hasSize(1);

        ClaimApprovedEvent event = events.get(0);
        assertThat(event.getClaimId()).isEqualTo(claimId);
        assertThat(event.getClaimAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        assertThat(event.eventType()).isEqualTo("claim.approved");
    }

    @Test
    @Transactional
    @DisplayName("청구 거부 시 ClaimRejectedEvent가 발행되어야 한다")
    void shouldPublishClaimRejectedEvent() {
        // Given
        ClaimService.CreateClaimCommand command = new ClaimService.CreateClaimCommand(
                "POL-12345",
                LocalDate.of(2025, 1, 10),
                "자동차 사고",
                BigDecimal.valueOf(1_000_000),
                "홍길동",
                "hong@example.com"
        );
        Long claimId = claimService.createClaim(command).getClaimId();
        testEventCollector.clear();

        // When
        claimService.reject(claimId);

        // Then
        List<ClaimRejectedEvent> events = testEventCollector.getEventsOfType(ClaimRejectedEvent.class);
        assertThat(events).hasSize(1);

        ClaimRejectedEvent event = events.get(0);
        assertThat(event.getClaimId()).isEqualTo(claimId);
        assertThat(event.eventType()).isEqualTo("claim.rejected");
    }

    @Test
    @Transactional
    @DisplayName("청구 지급 완료 시 ClaimPaidEvent가 발행되어야 한다")
    void shouldPublishClaimPaidEvent() {
        // Given
        ClaimService.CreateClaimCommand command = new ClaimService.CreateClaimCommand(
                "POL-12345",
                LocalDate.of(2025, 1, 10),
                "자동차 사고",
                BigDecimal.valueOf(1_000_000),
                "홍길동",
                "hong@example.com"
        );
        Long claimId = claimService.createClaim(command).getClaimId();
        claimService.approve(claimId);
        testEventCollector.clear();

        // When
        claimService.markPaid(claimId);

        // Then
        List<ClaimPaidEvent> events = testEventCollector.getEventsOfType(ClaimPaidEvent.class);
        assertThat(events).hasSize(1);

        ClaimPaidEvent event = events.get(0);
        assertThat(event.getClaimId()).isEqualTo(claimId);
        assertThat(event.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        assertThat(event.eventType()).isEqualTo("claim.paid");
    }

    /**
     * 테스트용 이벤트 수집기
     * 발행된 이벤트를 메모리에 저장하여 검증에 사용
     */
    @Component
    public static class TestEventCollector {
        private final List<Object> events = new ArrayList<>();

        @EventListener
        public void handleEvent(ClaimCreatedEvent event) {
            events.add(event);
        }

        @EventListener
        public void handleEvent(ClaimInReviewEvent event) {
            events.add(event);
        }

        @EventListener
        public void handleEvent(ClaimApprovedEvent event) {
            events.add(event);
        }

        @EventListener
        public void handleEvent(ClaimRejectedEvent event) {
            events.add(event);
        }

        @EventListener
        public void handleEvent(ClaimPaidEvent event) {
            events.add(event);
        }

        public <T> List<T> getEventsOfType(Class<T> type) {
            return events.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .toList();
        }

        public void clear() {
            events.clear();
        }
    }
}