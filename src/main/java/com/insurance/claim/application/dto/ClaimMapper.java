package com.insurance.claim.application.dto;

import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.domain.model.Claim;

public final class ClaimMapper {

    private ClaimMapper() {
    }

    public static ClaimResponse toResponse(Claim claim) {
        return ClaimResponse.builder()
                .claimId(claim.getId())
                .claimNumber(claim.getClaimNumber().getValue())
                .policyNumber(claim.getPolicyNumber())
                .claimAmount(claim.getClaimAmount().asBigDecimal())
                .status(claim.getStatus().name())
                .accidentDate(claim.getAccidentDate())
                .description(claim.getDescription())
                .claimantName(claim.getClaimantName())
                .email(claim.getEmail())
                .submittedAt(claim.getSubmittedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
