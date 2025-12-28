package com.insurance.claim.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClaimCreateRequest {

    @NotBlank(message = "보험 증권 번호는 필수입니다.")
    private String policyNumber;

    @NotNull(message = "사고 일자는 필수입니다.")
    @PastOrPresent(message = "사고 일자는 과거 또는 현재여야 합니다.")
    private LocalDate accidentDate;

    @NotBlank(message = "사고 내용은 필수입니다.")
    @Size(min = 10, max = 1000, message = "사고 내용은 10자 이상 1000자 이하여야 합니다.")
    private String description;

    @NotNull(message = "청구 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "청구 금액은 0보다 커야 합니다.")
    private BigDecimal claimAmount;

    @NotBlank(message = "청구인 이름은 필수입니다.")
    private String claimantName;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
