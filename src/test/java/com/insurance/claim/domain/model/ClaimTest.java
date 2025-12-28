package com.insurance.claim.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insurance.claim.domain.vo.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClaimTest {

    @Test
    void create_shouldInitializeWithPendingStatusAndNumber() {
        Claim claim = Claim.create(
                "POL-12345",
                Money.of(new BigDecimal("1000.00")),
                LocalDate.now(),
                "Rear-end collision with minor damage",
                "Kim Tester",
                "tester@example.com"
        );

        assertThat(claim.getClaimNumber()).isNotNull();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(claim.getClaimAmount().asBigDecimal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void approve_shouldChangeStatusWhenPending() {
        Claim claim = createPendingClaim();

        claim.approve();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void approve_shouldRejectWhenAlreadyApproved() {
        Claim claim = createPendingClaim();
        claim.approve();

        assertThatThrownBy(claim::approve)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markPaid_shouldRequireApprovedStatus() {
        Claim claim = createPendingClaim();

        assertThatThrownBy(claim::markPaid).isInstanceOf(IllegalStateException.class);

        claim.approve();
        claim.markPaid();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PAID);
    }

    private Claim createPendingClaim() {
        return Claim.create(
                "POL-54321",
                Money.of(new BigDecimal("500.00")),
                LocalDate.now().minusDays(1),
                "Minor fender bender",
                "Lee Sample",
                "sample@example.com"
        );
    }
}
