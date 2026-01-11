package com.insurance.claim.domain.event;

import com.insurance.claim.domain.model.Claim;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 청구 생성 이벤트
 * 청구가 최초 생성되었을 때 발행됨
 */
@Getter
@Builder
public class ClaimCreatedEvent implements DomainEvent {

    private final Long claimId;
    private final String claimNumber;
    private final String policyNumber;
    private final BigDecimal claimAmount;
    private final String claimantName;
    private final String email;
    private final LocalDate accidentDate;
    private final String description;
    private final LocalDateTime occurredAt;

    public static ClaimCreatedEvent from(Claim claim) {
        return ClaimCreatedEvent.builder()
                .claimId(claim.getId())
                .claimNumber(claim.getClaimNumber().getValue())
                .policyNumber(claim.getPolicyNumber())
                .claimAmount(claim.getClaimAmount().asBigDecimal())
                .claimantName(claim.getClaimantName())
                .email(claim.getEmail())
                .accidentDate(claim.getAccidentDate())
                .description(claim.getDescription())
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "claim.created";
    }
}
