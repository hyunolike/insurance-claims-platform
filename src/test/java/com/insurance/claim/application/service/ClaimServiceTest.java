package com.insurance.claim.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.exception.ClaimNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("ClaimService 통합 테스트")
class ClaimServiceTest {

    @Autowired
    private ClaimService claimService;

    @Test
    @DisplayName("유효한 청구 데이터로 청구를 생성하면 정상적으로 저장되고 응답을 반환한다")
    void shouldCreateClaimSuccessfully() {
        // Given
        ClaimService.CreateClaimCommand command = createValidClaimCommand(
                "POL-77777",
                LocalDate.now().minusDays(2),
                "Broken arm after skiing accident",
                new BigDecimal("250000.00"),
                "Park Adventurer",
                "park@example.com"
        );

        // When
        ClaimResponse response = claimService.createClaim(command);

        // Then
        assertThat(response.getClaimId()).isNotNull();
        assertThat(response.getClaimNumber())
                .isNotNull()
                .startsWith("CLM-")
                .matches("CLM-\\d{8}-[A-Z0-9]{5}");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getPolicyNumber()).isEqualTo("POL-77777");
        assertThat(response.getClaimAmount()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(response.getDescription()).isEqualTo("Broken arm after skiing accident");
        assertThat(response.getClaimantName()).isEqualTo("Park Adventurer");
    }

    @Test
    @DisplayName("저장된 청구를 ID로 조회하면 정확한 데이터를 반환한다")
    void shouldReturnClaimWhenFound() {
        // Given
        ClaimResponse createdClaim = claimService.createClaim(createValidClaimCommand(
                "POL-88888",
                LocalDate.now(),
                "Rear bumper replacement",
                new BigDecimal("800000.00"),
                "Choi Driver",
                "choi@example.com"
        ));

        // When
        ClaimResponse fetchedClaim = claimService.getClaim(createdClaim.getClaimId());

        // Then
        assertThat(fetchedClaim.getClaimId()).isEqualTo(createdClaim.getClaimId());
        assertThat(fetchedClaim.getClaimNumber()).isEqualTo(createdClaim.getClaimNumber());
        assertThat(fetchedClaim.getPolicyNumber()).isEqualTo("POL-88888");
        assertThat(fetchedClaim.getClaimAmount()).isEqualByComparingTo(new BigDecimal("800000.00"));
        assertThat(fetchedClaim.getDescription()).isEqualTo("Rear bumper replacement");
        assertThat(fetchedClaim.getClaimantName()).isEqualTo("Choi Driver");
        assertThat(fetchedClaim.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("존재하지 않는 청구를 조회하면 ClaimNotFoundException이 발생한다")
    void shouldThrowClaimNotFoundExceptionWhenClaimDoesNotExist() {
        // Given
        Long nonExistentClaimId = 9999L;

        // When & Then
        assertThatThrownBy(() -> claimService.getClaim(nonExistentClaimId))
                .isInstanceOf(ClaimNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    @DisplayName("큰 금액의 청구도 정상적으로 생성된다")
    void shouldCreateClaimWithLargeAmount() {
        // Given
        ClaimService.CreateClaimCommand command = createValidClaimCommand(
                "POL-99999",
                LocalDate.now(),
                "Major surgery claim",
                new BigDecimal("50000000.00"),
                "Kim Patient",
                "kim@example.com"
        );

        // When
        ClaimResponse response = claimService.createClaim(command);

        // Then
        assertThat(response.getClaimId()).isNotNull();
        assertThat(response.getClaimAmount()).isEqualByComparingTo(new BigDecimal("50000000.00"));
    }

    // 테스트 헬퍼 메서드
    private ClaimService.CreateClaimCommand createValidClaimCommand(
            String policyNumber,
            LocalDate accidentDate,
            String description,
            BigDecimal claimedAmount,
            String customerName,
            String customerEmail
    ) {
        return new ClaimService.CreateClaimCommand(
                policyNumber,
                accidentDate,
                description,
                claimedAmount,
                customerName,
                customerEmail
        );
    }
}
