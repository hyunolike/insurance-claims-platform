package com.insurance.claim.infrastructure.messaging;

import com.insurance.claim.domain.event.ClaimApproved;
import com.insurance.claim.domain.event.ClaimCreated;
import com.insurance.claim.domain.event.ClaimInReview;
import com.insurance.claim.domain.event.ClaimPaid;
import com.insurance.claim.domain.event.ClaimRejected;
import com.insurance.claim.domain.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도메인 이벤트를 Kafka 메시지로 변환
 */
public class ClaimEventMapper {

    public static ClaimEventMessage toMessage(DomainEvent event) {
        return switch (event) {
            case ClaimCreated e -> fromClaimCreated(e);
            case ClaimInReview e -> fromClaimInReview(e);
            case ClaimApproved e -> fromClaimApproved(e);
            case ClaimRejected e -> fromClaimRejected(e);
            case ClaimPaid e -> fromClaimPaid(e);
            default -> throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
        };
    }

    private static ClaimEventMessage fromClaimCreated(ClaimCreated event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.claimId())
                .claimNumber(event.claimNumber().getValue())
                .policyNumber(event.policyNumber())
                .claimAmount(event.claimAmount().asBigDecimal())
                .accidentDate(event.accidentDate())
                .claimantName(event.claimantName())
                .email(event.email())
                .occurredOn(event.occurredOn())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimInReview(ClaimInReview event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.claimId())
                .claimNumber(event.claimNumber().getValue())
                .occurredOn(event.occurredOn())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimApproved(ClaimApproved event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.claimId())
                .claimNumber(event.claimNumber().getValue())
                .claimAmount(event.claimAmount().asBigDecimal())
                .occurredOn(event.occurredOn())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimRejected(ClaimRejected event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.claimId())
                .claimNumber(event.claimNumber().getValue())
                .occurredOn(event.occurredOn())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimPaid(ClaimPaid event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.claimId())
                .claimNumber(event.claimNumber().getValue())
                .claimAmount(event.claimAmount().asBigDecimal())
                .occurredOn(event.occurredOn())
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
