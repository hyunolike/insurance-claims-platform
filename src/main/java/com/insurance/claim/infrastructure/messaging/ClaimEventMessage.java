package com.insurance.claim.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka로 전송되는 청구 이벤트 메시지
 * Infrastructure 계층의 DTO - 직렬화/역직렬화 편의성을 위해 변경 가능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEventMessage {

    private String eventType;
    private String eventId; // UUID for deduplication

    // Claim data
    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private BigDecimal claimAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate accidentDate;

    private String claimantName;
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredOn;

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;
}
