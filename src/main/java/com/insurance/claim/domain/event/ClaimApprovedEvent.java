package com.insurance.claim.domain.event;

import com.insurance.claim.domain.model.Claim;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 청구 승인 이벤트
 * 청구가 승인되었을 때 발행됨
 */
@Getter
@Builder
public class ClaimApprovedEvent implements DomainEvent {

    private final Long claimId;
    private final String claimNumber;
    private final String policyNumber;
    private final BigDecimal claimAmount;
    private final String claimantName;
    private final String email;
    private final LocalDateTime occurredAt;

    public static ClaimApprovedEvent from(Claim claim) {
        return ClaimApprovedEvent.builder()
                .claimId(claim.getId())
                .claimNumber(claim.getClaimNumber().getValue())
                .policyNumber(claim.getPolicyNumber())
                .claimAmount(claim.getClaimAmount().asBigDecimal())
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
        return "claim.approved";
    }
}
