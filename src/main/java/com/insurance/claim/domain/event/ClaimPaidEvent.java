package com.insurance.claim.domain.event;

import com.insurance.claim.domain.model.Claim;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 청구 지급 완료 이벤트
 * 청구 금액이 실제로 지급되었을 때 발행됨
 */
@Getter
@Builder
public class ClaimPaidEvent implements DomainEvent {

    private final Long claimId;
    private final String claimNumber;
    private final String policyNumber;
    private final BigDecimal paidAmount;
    private final String claimantName;
    private final String email;
    private final LocalDateTime occurredAt;

    public static ClaimPaidEvent from(Claim claim) {
        return ClaimPaidEvent.builder()
                .claimId(claim.getId())
                .claimNumber(claim.getClaimNumber().getValue())
                .policyNumber(claim.getPolicyNumber())
                .paidAmount(claim.getClaimAmount().asBigDecimal())
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
        return "claim.paid";
    }
}
