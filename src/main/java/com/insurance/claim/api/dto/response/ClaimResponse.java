package com.insurance.claim.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClaimResponse {

    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private BigDecimal claimAmount;
    private String status;
    private LocalDate accidentDate;
    private String description;
    private String claimantName;
    private String email;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
