package com.insurance.claim.domain.event;

import com.insurance.claim.domain.vo.ClaimNumber;
import java.time.LocalDateTime;

/**
 * 청구가 심사 중 상태로 전환되었을 때 발생하는 도메인 이벤트
 */
public record ClaimInReview(
        Long claimId,
        ClaimNumber claimNumber,
        LocalDateTime occurredOn
) implements DomainEvent {

    public ClaimInReview {
        if (claimId == null) {
            throw new IllegalArgumentException("claimId는 필수입니다.");
        }
        if (claimNumber == null) {
            throw new IllegalArgumentException("claimNumber는 필수입니다.");
        }
        if (occurredOn == null) {
            occurredOn = LocalDateTime.now();
        }
    }

    @Override
    public String eventType() {
        return "claim.in-review";
    }
}
