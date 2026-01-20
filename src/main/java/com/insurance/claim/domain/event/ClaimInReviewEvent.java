package com.insurance.claim.domain.event;

import com.insurance.claim.domain.model.Claim;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 청구 심사 시작 이벤트
 * 청구가 심사 대기 상태에서 심사 중으로 전환될 때 발행됨
 */
@Getter
@Builder
public class ClaimInReviewEvent implements DomainEvent {

    private final Long claimId;
    private final String claimNumber;
    private final String policyNumber;
    private final LocalDateTime occurredAt;

    public static ClaimInReviewEvent from(Claim claim) {
        return ClaimInReviewEvent.builder()
                .claimId(claim.getId())
                .claimNumber(claim.getClaimNumber().getValue())
                .policyNumber(claim.getPolicyNumber())
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "claim.in_review";
    }
}