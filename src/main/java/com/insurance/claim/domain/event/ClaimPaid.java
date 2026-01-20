package com.insurance.claim.domain.event;

import com.insurance.claim.domain.vo.ClaimNumber;
import com.insurance.claim.domain.vo.Money;
import java.time.LocalDateTime;

/**
 * 청구에 대한 지급이 완료되었을 때 발생하는 도메인 이벤트
 */
public record ClaimPaid(
        Long claimId,
        ClaimNumber claimNumber,
        Money claimAmount,
        LocalDateTime occurredOn
) implements DomainEvent {

    public ClaimPaid {
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
        return "claim.paid";
    }
}
