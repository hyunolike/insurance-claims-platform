package com.insurance.claim.application.service;

import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.dto.ClaimMapper;
import com.insurance.claim.application.exception.ClaimNotFoundException;
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
        return ClaimMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaim(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));
        return ClaimMapper.toResponse(claim);
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
