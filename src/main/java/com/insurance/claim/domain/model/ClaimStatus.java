package com.insurance.claim.domain.model;

import lombok.Getter;

@Getter
public enum ClaimStatus {
    PENDING("청구 접수"),
    IN_REVIEW("심사 중"),
    APPROVED("지급 승인"),
    REJECTED("지급 거절"),
    PAID("지급 완료"),
    CANCELLED("취소");

    private final String description;

    ClaimStatus(String description) {
        this.description = description;
    }
}
