package com.insurance.claim.domain.model;

import com.insurance.claim.domain.vo.ClaimNumber;
import com.insurance.claim.domain.vo.Money;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Claim {

    private Long id;
    private final ClaimNumber claimNumber;
    private final String policyNumber;
    private final Money claimAmount;
    private ClaimStatus status;
    private final String description;
    private final LocalDate accidentDate;
    private final String claimantName;
    private final String email;
    private final LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    @Builder
    private Claim(
            Long id,
            ClaimNumber claimNumber,
            String policyNumber,
            Money claimAmount,
            ClaimStatus status,
            String description,
            LocalDate accidentDate,
            String claimantName,
            String email,
            LocalDateTime submittedAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.claimNumber = claimNumber == null ? ClaimNumber.generate() : claimNumber;
        this.policyNumber = requireNonBlank(policyNumber, "보험 증권 번호는 필수입니다.");
        this.claimAmount = Objects.requireNonNull(claimAmount, "청구 금액은 필수입니다.");
        this.status = status == null ? ClaimStatus.PENDING : status;
        this.description = requireNonBlank(description, "사고 내용은 필수입니다.");
        this.accidentDate = Objects.requireNonNull(accidentDate, "사고 일자는 필수입니다.");
        this.claimantName = requireNonBlank(claimantName, "청구인 이름은 필수입니다.");
        this.email = email;
        this.submittedAt = submittedAt == null ? LocalDateTime.now() : submittedAt;
        this.updatedAt = updatedAt == null ? this.submittedAt : updatedAt;
    }

    public static Claim create(
            String policyNumber,
            Money claimAmount,
            LocalDate accidentDate,
            String description,
            String claimantName,
            String email
    ) {
        return Claim.builder()
                .policyNumber(policyNumber)
                .claimAmount(claimAmount)
                .accidentDate(accidentDate)
                .description(description)
                .claimantName(claimantName)
                .email(email)
                .build();
    }

    public void markInReview() {
        ensureStatus(ClaimStatus.PENDING);
        this.status = ClaimStatus.IN_REVIEW;
        touch();
    }

    public void approve() {
        ensureStatus(ClaimStatus.PENDING, ClaimStatus.IN_REVIEW);
        this.status = ClaimStatus.APPROVED;
        touch();
    }

    public void reject() {
        ensureStatus(ClaimStatus.PENDING, ClaimStatus.IN_REVIEW);
        this.status = ClaimStatus.REJECTED;
        touch();
    }

    public void markPaid() {
        ensureStatus(ClaimStatus.APPROVED);
        this.status = ClaimStatus.PAID;
        touch();
    }

    public void assignId(Long id) {
        if (this.id != null) {
            return;
        }
        this.id = id;
    }

    public void syncUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "업데이트 시간은 필수입니다.");
    }

    private void ensureStatus(ClaimStatus... expected) {
        for (ClaimStatus candidate : expected) {
            if (status == candidate) {
                return;
            }
        }
        throw new IllegalStateException("현재 상태(" + status + ")에서는 수행할 수 없는 작업입니다.");
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
