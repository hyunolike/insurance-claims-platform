package com.insurance.claim.infrastructure.event;

import com.insurance.claim.domain.event.ClaimApproved;
import com.insurance.claim.domain.event.ClaimCreated;
import com.insurance.claim.domain.event.ClaimInReview;
import com.insurance.claim.domain.event.ClaimPaid;
import com.insurance.claim.domain.event.ClaimRejected;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 청구 도메인 이벤트 리스너
 * Phase 2: 동기 이벤트 처리 (로깅)
 * Phase 3에서 Kafka producer로 확장 예정
 */
@Slf4j
@Component
public class ClaimEventListener {

    @EventListener
    public void handleClaimCreated(ClaimCreated event) {
        log.info("[EVENT] 청구 생성됨 - claimNumber: {}, policyNumber: {}, amount: {}, claimant: {}",
                event.claimNumber().getValue(),
                event.policyNumber(),
                event.claimAmount(),
                event.claimantName());
    }

    @EventListener
    public void handleClaimInReview(ClaimInReview event) {
        log.info("[EVENT] 청구 심사 시작 - claimNumber: {}", event.claimNumber().getValue());
    }

    @EventListener
    public void handleClaimApproved(ClaimApproved event) {
        log.info("[EVENT] 청구 승인됨 - claimNumber: {}, amount: {}",
                event.claimNumber().getValue(),
                event.claimAmount());
    }

    @EventListener
    public void handleClaimRejected(ClaimRejected event) {
        log.info("[EVENT] 청구 거절됨 - claimNumber: {}", event.claimNumber().getValue());
    }

    @EventListener
    public void handleClaimPaid(ClaimPaid event) {
        log.info("[EVENT] 청구 지급 완료 - claimNumber: {}, amount: {}",
                event.claimNumber().getValue(),
                event.claimAmount());
    }
}
