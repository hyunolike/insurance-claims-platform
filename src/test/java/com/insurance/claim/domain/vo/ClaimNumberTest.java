package com.insurance.claim.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClaimNumberTest {

    @Test
    void generate_shouldMatchPattern() {
        ClaimNumber claimNumber = ClaimNumber.generate();

        assertThat(claimNumber.getValue()).matches("^CLM-\\d{8}-[A-Z0-9]{5}$");
    }

    @Test
    void from_shouldRejectInvalidFormat() {
        assertThatThrownBy(() -> ClaimNumber.from("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
