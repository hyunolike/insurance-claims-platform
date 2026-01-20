package com.insurance.claim.infrastructure.messaging;

import com.insurance.claim.domain.event.ClaimApprovedEvent;
import com.insurance.claim.domain.event.ClaimCreatedEvent;
import com.insurance.claim.domain.event.ClaimInReviewEvent;
import com.insurance.claim.domain.event.ClaimPaidEvent;
import com.insurance.claim.domain.event.ClaimRejectedEvent;
import com.insurance.claim.domain.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도메인 이벤트를 Kafka 메시지로 변환
 * Phase 3에서 Kafka 연동 시 사용 예정
 */
public class ClaimEventMapper {

    public static ClaimEventMessage toMessage(DomainEvent event) {
        return switch (event) {
            case ClaimCreatedEvent e -> fromClaimCreated(e);
            case ClaimInReviewEvent e -> fromClaimInReview(e);
            case ClaimApprovedEvent e -> fromClaimApproved(e);
            case ClaimRejectedEvent e -> fromClaimRejected(e);
            case ClaimPaidEvent e -> fromClaimPaid(e);
            default -> throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
        };
    }

    private static ClaimEventMessage fromClaimCreated(ClaimCreatedEvent event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.getClaimId())
                .claimNumber(event.getClaimNumber())
                .policyNumber(event.getPolicyNumber())
                .claimAmount(event.getClaimAmount())
                .accidentDate(event.getAccidentDate())
                .claimantName(event.getClaimantName())
                .email(event.getEmail())
                .occurredOn(event.occurredAt())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimInReview(ClaimInReviewEvent event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.getClaimId())
                .claimNumber(event.getClaimNumber())
                .occurredOn(event.occurredAt())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimApproved(ClaimApprovedEvent event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.getClaimId())
                .claimNumber(event.getClaimNumber())
                .claimAmount(event.getClaimAmount())
                .occurredOn(event.occurredAt())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimRejected(ClaimRejectedEvent event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.getClaimId())
                .claimNumber(event.getClaimNumber())
                .occurredOn(event.occurredAt())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    private static ClaimEventMessage fromClaimPaid(ClaimPaidEvent event) {
        return ClaimEventMessage.builder()
                .eventType(event.eventType())
                .eventId(UUID.randomUUID().toString())
                .claimId(event.getClaimId())
                .claimNumber(event.getClaimNumber())
                .claimAmount(event.getPaidAmount())
                .occurredOn(event.occurredAt())
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
