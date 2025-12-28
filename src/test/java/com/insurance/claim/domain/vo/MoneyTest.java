package com.insurance.claim.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void of_shouldNormalizeScale() {
        Money money = Money.of(new BigDecimal("100.1"));

        assertThat(money.asBigDecimal()).isEqualByComparingTo("100.10");
    }

    @Test
    void subtract_shouldFailWhenResultIsNegative() {
        Money money = Money.of(new BigDecimal("10.00"));
        Money larger = Money.of(new BigDecimal("20.00"));

        assertThatThrownBy(() -> money.subtract(larger))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void add_shouldReturnNewMoney() {
        Money result = Money.of(new BigDecimal("5.00"))
                .add(Money.of(new BigDecimal("2.50")));

        assertThat(result.asBigDecimal()).isEqualByComparingTo("7.50");
    }
}
