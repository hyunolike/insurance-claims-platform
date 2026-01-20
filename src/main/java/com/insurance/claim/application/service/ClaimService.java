package com.insurance.claim.application.service;

import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.dto.ClaimMapper;
import com.insurance.claim.application.exception.ClaimNotFoundException;
import com.insurance.claim.domain.event.DomainEventPublisher;
import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.repository.ClaimRepository;
import com.insurance.claim.domain.vo.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final DomainEventPublisher eventPublisher;

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

        // 저장 후 ID가 할당되면 이벤트 등록 및 발행
        saved.registerCreatedEvent();
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        return ClaimMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaim(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));
        return ClaimMapper.toResponse(claim);
    }

    @Transactional
    public ClaimResponse startReview(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.markInReview();
        Claim saved = claimRepository.save(claim);

        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        return ClaimMapper.toResponse(saved);
    }

    @Transactional
    public ClaimResponse approveClaim(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.approve();
        Claim saved = claimRepository.save(claim);

        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        return ClaimMapper.toResponse(saved);
    }

    @Transactional
    public ClaimResponse rejectClaim(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.reject();
        Claim saved = claimRepository.save(claim);

        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        return ClaimMapper.toResponse(saved);
    }

    @Transactional
    public ClaimResponse payClaim(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.markPaid();
        Claim saved = claimRepository.save(claim);

        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        return ClaimMapper.toResponse(saved);
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
