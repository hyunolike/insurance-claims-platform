package com.insurance.claim.infrastructure.persistence.claim;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.model.ClaimStatus;
import com.insurance.claim.domain.repository.ClaimRepository;
import com.insurance.claim.domain.vo.ClaimNumber;
import com.insurance.claim.domain.vo.Money;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClaimRepositoryAdapter implements ClaimRepository {

    private final ClaimJpaRepository claimJpaRepository;

    @Override
    public Claim save(Claim claim) {
        ClaimEntity entity = toEntity(claim);
        ClaimEntity saved = claimJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Claim> findById(Long id) {
        return claimJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Claim> findByClaimNumber(ClaimNumber claimNumber) {
        return claimJpaRepository.findByClaimNumber(claimNumber.getValue())
                .map(this::toDomain);
    }

    private ClaimEntity toEntity(Claim claim) {
        ClaimEntity entity = ClaimEntity.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber().getValue())
                .policyNumber(claim.getPolicyNumber())
                .claimedAmount(claim.getClaimAmount().asBigDecimal())
                .status(claim.getStatus().name())
                .description(claim.getDescription())
                .accidentDate(claim.getAccidentDate())
                .claimantName(claim.getClaimantName())
                .email(claim.getEmail())
                .submittedAt(claim.getSubmittedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
        return entity;
    }

    private Claim toDomain(ClaimEntity entity) {
        return Claim.builder()
                .id(entity.getId())
                .claimNumber(ClaimNumber.from(entity.getClaimNumber()))
                .policyNumber(entity.getPolicyNumber())
                .claimAmount(Money.of(entity.getClaimedAmount()))
                .status(ClaimStatus.valueOf(entity.getStatus()))
                .description(entity.getDescription())
                .accidentDate(entity.getAccidentDate())
                .claimantName(entity.getClaimantName())
                .email(entity.getEmail())
                .submittedAt(entity.getSubmittedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
