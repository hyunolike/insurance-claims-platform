package com.insurance.claim.domain.event;

import com.insurance.claim.domain.vo.ClaimNumber;
import com.insurance.claim.domain.vo.Money;
import java.time.LocalDateTime;

/**
 * 청구가 승인되었을 때 발생하는 도메인 이벤트
 */
public record ClaimApproved(
        Long claimId,
        ClaimNumber claimNumber,
        Money claimAmount,
        LocalDateTime occurredOn
) implements DomainEvent {

    public ClaimApproved {
        if (claimId == null) {
            throw new IllegalArgumentException("claimId는 필수입니다.");
        }
        if (claimNumber == null) {
            throw new IllegalArgumentException("claimNumber는 필수입니다.");
        }
        if (claimAmount == null) {
            throw new IllegalArgumentException("claimAmount는 필수입니다.");
        }
        if (occurredOn == null) {
            occurredOn = LocalDateTime.now();
        }
    }

    @Override
    public String eventType() {
        return "claim.approved";
    }
}
