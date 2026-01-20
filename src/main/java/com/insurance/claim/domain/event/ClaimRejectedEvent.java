package com.insurance.claim.domain.event;

import com.insurance.claim.domain.model.Claim;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 청구 거부 이벤트
 * 청구가 거부되었을 때 발행됨
 */
@Getter
@Builder
public class ClaimRejectedEvent implements DomainEvent {

    private final Long claimId;
    private final String claimNumber;
    private final String policyNumber;
    private final String claimantName;
    private final String email;
    private final LocalDateTime occurredAt;

    public static ClaimRejectedEvent from(Claim claim) {
        return ClaimRejectedEvent.builder()
                .claimId(claim.getId())
                .claimNumber(claim.getClaimNumber().getValue())
                .policyNumber(claim.getPolicyNumber())
                .claimantName(claim.getClaimantName())
                .email(claim.getEmail())
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "claim.rejected";
    }
}
