package com.insurance.claim.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.exception.ClaimNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ClaimServiceTest {

    @Autowired
    private ClaimService claimService;

    @Test
    void createClaim_shouldPersistAndReturnResponse() {
        ClaimResponse response = claimService.createClaim(new ClaimService.CreateClaimCommand(
                "POL-77777",
                LocalDate.now().minusDays(2),
                "Broken arm after skiing accident",
                new BigDecimal("250000.00"),
                "Park Adventurer",
                "park@example.com"
        ));

        assertThat(response.getClaimId()).isNotNull();
        assertThat(response.getClaimNumber()).startsWith("CLM-");
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getClaim_shouldReturnPersistedClaim() {
        ClaimResponse created = claimService.createClaim(new ClaimService.CreateClaimCommand(
                "POL-88888",
                LocalDate.now(),
                "Rear bumper replacement",
                new BigDecimal("800000.00"),
                "Choi Driver",
                "choi@example.com"
        ));

        ClaimResponse fetched = claimService.getClaim(created.getClaimId());

        assertThat(fetched.getClaimNumber()).isEqualTo(created.getClaimNumber());
        assertThat(fetched.getPolicyNumber()).isEqualTo("POL-88888");
    }

    @Test
    void getClaim_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> claimService.getClaim(9999L))
                .isInstanceOf(ClaimNotFoundException.class);
    }
}
