package com.insurance.claim.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.model.ClaimStatus;
import com.insurance.claim.domain.vo.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Claim 도메인 이벤트 테스트")
class ClaimDomainEventTest {

    private Claim claim;

    @BeforeEach
    void setUp() {
        claim = Claim.create(
                "POL-12345",
                Money.of(new BigDecimal("1000000")),
                LocalDate.now().minusDays(1),
                "교통사고 치료비 청구",
                "홍길동",
                "hong@example.com"
        );
        // ID 할당 시뮬레이션 (DB 저장 후 상태)
        claim.assignId(1L);
    }

    @Nested
    @DisplayName("ClaimCreated 이벤트")
    class ClaimCreatedEventTest {

        @Test
        @DisplayName("registerCreatedEvent 호출 시 ClaimCreated 이벤트가 등록된다")
        void shouldRegisterClaimCreatedEvent() {
            claim.registerCreatedEvent();

            assertThat(claim.getDomainEvents()).hasSize(1);
            assertThat(claim.getDomainEvents().get(0)).isInstanceOf(ClaimCreated.class);

            ClaimCreated event = (ClaimCreated) claim.getDomainEvents().get(0);
            assertThat(event.claimId()).isEqualTo(1L);
            assertThat(event.claimNumber()).isEqualTo(claim.getClaimNumber());
            assertThat(event.policyNumber()).isEqualTo("POL-12345");
            assertThat(event.claimAmount().asBigDecimal()).isEqualByComparingTo("1000000");
            assertThat(event.eventType()).isEqualTo("claim.created");
        }

        @Test
        @DisplayName("ID가 할당되지 않은 상태에서 registerCreatedEvent 호출 시 예외가 발생한다")
        void shouldThrowExceptionWhenIdNotAssigned() {
            Claim newClaim = Claim.create(
                    "POL-99999",
                    Money.of(new BigDecimal("500000")),
                    LocalDate.now(),
                    "테스트 청구",
                    "테스터",
                    "test@example.com"
            );

            assertThatThrownBy(newClaim::registerCreatedEvent)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ID가 할당된 후");
        }
    }

    @Nested
    @DisplayName("상태 전환 이벤트")
    class StateTransitionEventTest {

        @Test
        @DisplayName("markInReview 호출 시 ClaimInReview 이벤트가 등록된다")
        void shouldRegisterClaimInReviewEvent() {
            claim.markInReview();

            assertThat(claim.getDomainEvents()).hasSize(1);
            assertThat(claim.getDomainEvents().get(0)).isInstanceOf(ClaimInReview.class);

            ClaimInReview event = (ClaimInReview) claim.getDomainEvents().get(0);
            assertThat(event.claimId()).isEqualTo(1L);
            assertThat(event.eventType()).isEqualTo("claim.in-review");
        }

        @Test
        @DisplayName("approve 호출 시 ClaimApproved 이벤트가 등록된다")
        void shouldRegisterClaimApprovedEvent() {
            claim.approve();

            assertThat(claim.getDomainEvents()).hasSize(1);
            assertThat(claim.getDomainEvents().get(0)).isInstanceOf(ClaimApproved.class);

            ClaimApproved event = (ClaimApproved) claim.getDomainEvents().get(0);
            assertThat(event.claimId()).isEqualTo(1L);
            assertThat(event.claimAmount().asBigDecimal()).isEqualByComparingTo("1000000");
            assertThat(event.eventType()).isEqualTo("claim.approved");
        }

        @Test
        @DisplayName("reject 호출 시 ClaimRejected 이벤트가 등록된다")
        void shouldRegisterClaimRejectedEvent() {
            claim.reject();

            assertThat(claim.getDomainEvents()).hasSize(1);
            assertThat(claim.getDomainEvents().get(0)).isInstanceOf(ClaimRejected.class);

            ClaimRejected event = (ClaimRejected) claim.getDomainEvents().get(0);
            assertThat(event.claimId()).isEqualTo(1L);
            assertThat(event.eventType()).isEqualTo("claim.rejected");
        }

        @Test
        @DisplayName("markPaid 호출 시 ClaimPaid 이벤트가 등록된다")
        void shouldRegisterClaimPaidEvent() {
            claim.approve();
            claim.clearDomainEvents(); // 승인 이벤트 제거

            claim.markPaid();

            assertThat(claim.getDomainEvents()).hasSize(1);
            assertThat(claim.getDomainEvents().get(0)).isInstanceOf(ClaimPaid.class);

            ClaimPaid event = (ClaimPaid) claim.getDomainEvents().get(0);
            assertThat(event.claimId()).isEqualTo(1L);
            assertThat(event.claimAmount().asBigDecimal()).isEqualByComparingTo("1000000");
            assertThat(event.eventType()).isEqualTo("claim.paid");
        }
    }

    @Nested
    @DisplayName("이벤트 관리")
    class EventManagementTest {

        @Test
        @DisplayName("clearDomainEvents 호출 시 모든 이벤트가 제거된다")
        void shouldClearAllEvents() {
            claim.registerCreatedEvent();
            claim.markInReview();

            assertThat(claim.getDomainEvents()).hasSize(2);

            claim.clearDomainEvents();

            assertThat(claim.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("getDomainEvents는 불변 리스트를 반환한다")
        void shouldReturnUnmodifiableList() {
            claim.registerCreatedEvent();

            assertThatThrownBy(() -> claim.getDomainEvents().add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("연속 상태 전환 시 여러 이벤트가 순서대로 등록된다")
        void shouldRegisterMultipleEventsInOrder() {
            claim.registerCreatedEvent();
            claim.markInReview();
            claim.approve();
            claim.markPaid();

            assertThat(claim.getDomainEvents()).hasSize(4);
            assertThat(claim.getDomainEvents().get(0)).isInstanceOf(ClaimCreated.class);
            assertThat(claim.getDomainEvents().get(1)).isInstanceOf(ClaimInReview.class);
            assertThat(claim.getDomainEvents().get(2)).isInstanceOf(ClaimApproved.class);
            assertThat(claim.getDomainEvents().get(3)).isInstanceOf(ClaimPaid.class);
        }
    }

    @Nested
    @DisplayName("상태 전환 규칙")
    class StateTransitionRulesTest {

        @Test
        @DisplayName("PENDING 상태에서 IN_REVIEW로 전환 가능하다")
        void shouldTransitionFromPendingToInReview() {
            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING);

            claim.markInReview();

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
        }

        @Test
        @DisplayName("IN_REVIEW 상태에서 APPROVED로 전환 가능하다")
        void shouldTransitionFromInReviewToApproved() {
            claim.markInReview();

            claim.approve();

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        }

        @Test
        @DisplayName("IN_REVIEW 상태에서 REJECTED로 전환 가능하다")
        void shouldTransitionFromInReviewToRejected() {
            claim.markInReview();

            claim.reject();

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        }

        @Test
        @DisplayName("APPROVED 상태에서 PAID로 전환 가능하다")
        void shouldTransitionFromApprovedToPaid() {
            claim.approve();

            claim.markPaid();

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PAID);
        }

        @Test
        @DisplayName("REJECTED 상태에서는 다른 상태로 전환할 수 없다")
        void shouldNotTransitionFromRejected() {
            claim.reject();

            assertThatThrownBy(claim::approve).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(claim::markPaid).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("PAID 상태에서는 다른 상태로 전환할 수 없다")
        void shouldNotTransitionFromPaid() {
            claim.approve();
            claim.markPaid();

            assertThatThrownBy(claim::approve).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(claim::reject).isInstanceOf(IllegalStateException.class);
        }
    }
}