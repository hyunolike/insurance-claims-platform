package com.insurance.claim.application.event;

import com.insurance.claim.domain.event.ClaimApprovedEvent;
import com.insurance.claim.domain.event.ClaimCreatedEvent;
import com.insurance.claim.domain.event.ClaimInReviewEvent;
import com.insurance.claim.domain.event.ClaimPaidEvent;
import com.insurance.claim.domain.event.ClaimRejectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 청구 도메인 이벤트 리스너
 * Phase 2에서는 로그 출력으로 이벤트 수신 확인
 * Phase 4에서 실제 알림 발송 구현 예정
 */
@Slf4j
@Component
public class ClaimEventListener {

    @EventListener
    public void handleClaimCreated(ClaimCreatedEvent event) {
        log.info("========================================");
        log.info("[이벤트 수신] 청구 생성: {}", event.eventType());
        log.info("  - 청구번호: {}", event.getClaimNumber());
        log.info("  - 청구인: {}", event.getClaimantName());
        log.info("  - 청구금액: {}", event.getClaimAmount());
        log.info("  - 이메일: {}", event.getEmail());
        log.info("  - 발생시각: {}", event.occurredAt());
        log.info("========================================");

        // TODO: Phase 4에서 실제 알림 발송 구현
        // - 이메일 발송: "청구가 접수되었습니다"
        // - SMS 발송 (선택사항)
    }

    @EventListener
    public void handleClaimInReview(ClaimInReviewEvent event) {
        log.info("========================================");
        log.info("[이벤트 수신] 청구 심사 시작: {}", event.eventType());
        log.info("  - 청구번호: {}", event.getClaimNumber());
        log.info("  - 발생시각: {}", event.occurredAt());
        log.info("========================================");

        // TODO: Phase 4에서 실제 알림 발송 구현
        // - 이메일 발송: "청구 심사가 시작되었습니다"
    }

    @EventListener
    public void handleClaimApproved(ClaimApprovedEvent event) {
        log.info("========================================");
        log.info("[이벤트 수신] 청구 승인: {}", event.eventType());
        log.info("  - 청구번호: {}", event.getClaimNumber());
        log.info("  - 청구인: {}", event.getClaimantName());
        log.info("  - 승인금액: {}", event.getClaimAmount());
        log.info("  - 이메일: {}", event.getEmail());
        log.info("  - 발생시각: {}", event.occurredAt());
        log.info("========================================");

        // TODO: Phase 4에서 실제 알림 발송 및 지급 프로세스 트리거
        // - 이메일 발송: "청구가 승인되었습니다"
        // - 지급 프로세스 시작
    }

    @EventListener
    public void handleClaimRejected(ClaimRejectedEvent event) {
        log.info("========================================");
        log.info("[이벤트 수신] 청구 거부: {}", event.eventType());
        log.info("  - 청구번호: {}", event.getClaimNumber());
        log.info("  - 청구인: {}", event.getClaimantName());
        log.info("  - 이메일: {}", event.getEmail());
        log.info("  - 발생시각: {}", event.occurredAt());
        log.info("========================================");

        // TODO: Phase 4에서 실제 알림 발송 구현
        // - 이메일 발송: "청구가 거부되었습니다" + 사유
    }

    @EventListener
    public void handleClaimPaid(ClaimPaidEvent event) {
        log.info("========================================");
        log.info("[이벤트 수신] 청구 지급 완료: {}", event.eventType());
        log.info("  - 청구번호: {}", event.getClaimNumber());
        log.info("  - 청구인: {}", event.getClaimantName());
        log.info("  - 지급금액: {}", event.getPaidAmount());
        log.info("  - 이메일: {}", event.getEmail());
        log.info("  - 발생시각: {}", event.occurredAt());
        log.info("========================================");

        // TODO: Phase 4에서 실제 알림 발송 구현
        // - 이메일 발송: "보험금이 지급되었습니다"
        // - SMS 발송 (선택사항)
    }
}
