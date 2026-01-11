package com.insurance.claim.application.service;

import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.dto.ClaimMapper;
import com.insurance.claim.application.exception.ClaimNotFoundException;
import com.insurance.claim.domain.event.ClaimApprovedEvent;
import com.insurance.claim.domain.event.ClaimCreatedEvent;
import com.insurance.claim.domain.event.ClaimInReviewEvent;
import com.insurance.claim.domain.event.ClaimPaidEvent;
import com.insurance.claim.domain.event.ClaimRejectedEvent;
import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.repository.ClaimRepository;
import com.insurance.claim.domain.vo.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ClaimResponse createClaim(CreateClaimCommand command) {
        Claim claim = Claim.create(
                command.policyNumber(),
                Money.of(command.claimAmount()),
                command.accidentDate(),
                command.description(),
                command.claimantName(),
                command.email()
        );

        Claim saved = claimRepository.save(claim);

        // 청구 생성 이벤트 발행
        ClaimCreatedEvent event = ClaimCreatedEvent.from(saved);
        eventPublisher.publishEvent(event);
        log.info("청구 생성 이벤트 발행: claimId={}, claimNumber={}", saved.getId(), saved.getClaimNumber().getValue());

        return ClaimMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaim(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));
        return ClaimMapper.toResponse(claim);
    }

    @Transactional
    public ClaimResponse markInReview(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.markInReview();
        Claim updated = claimRepository.save(claim);

        // 심사 시작 이벤트 발행
        ClaimInReviewEvent event = ClaimInReviewEvent.from(updated);
        eventPublisher.publishEvent(event);
        log.info("청구 심사 시작 이벤트 발행: claimId={}, claimNumber={}", updated.getId(), updated.getClaimNumber().getValue());

        return ClaimMapper.toResponse(updated);
    }

    @Transactional
    public ClaimResponse approve(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.approve();
        Claim updated = claimRepository.save(claim);

        // 청구 승인 이벤트 발행
        ClaimApprovedEvent event = ClaimApprovedEvent.from(updated);
        eventPublisher.publishEvent(event);
        log.info("청구 승인 이벤트 발행: claimId={}, claimNumber={}", updated.getId(), updated.getClaimNumber().getValue());

        return ClaimMapper.toResponse(updated);
    }

    @Transactional
    public ClaimResponse reject(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.reject();
        Claim updated = claimRepository.save(claim);

        // 청구 거부 이벤트 발행
        ClaimRejectedEvent event = ClaimRejectedEvent.from(updated);
        eventPublisher.publishEvent(event);
        log.info("청구 거부 이벤트 발행: claimId={}, claimNumber={}", updated.getId(), updated.getClaimNumber().getValue());

        return ClaimMapper.toResponse(updated);
    }

    @Transactional
    public ClaimResponse markPaid(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.markPaid();
        Claim updated = claimRepository.save(claim);

        // 청구 지급 완료 이벤트 발행
        ClaimPaidEvent event = ClaimPaidEvent.from(updated);
        eventPublisher.publishEvent(event);
        log.info("청구 지급 완료 이벤트 발행: claimId={}, claimNumber={}", updated.getId(), updated.getClaimNumber().getValue());

        return ClaimMapper.toResponse(updated);
    }

    public record CreateClaimCommand(
            String policyNumber,
            LocalDate accidentDate,
            String description,
            BigDecimal claimAmount,
            String claimantName,
            String email
    ) {
    }
}
