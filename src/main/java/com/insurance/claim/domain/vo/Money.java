package com.insurance.claim.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money {

    private static final int SCALE = 2;
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "금액은 필수입니다.");
        BigDecimal scaled = amount.setScale(SCALE, RoundingMode.HALF_UP);
        if (scaled.signum() < 0) {
            throw new IllegalArgumentException("금액은 음수가 될 수 없습니다.");
        }
        this.amount = scaled;
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public BigDecimal asBigDecimal() {
        return amount;
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        BigDecimal result = amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("결과 금액은 음수가 될 수 없습니다.");
        }
        return new Money(result);
    }

    public boolean isGreaterThan(Money other) {
        return amount.compareTo(other.amount) > 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }
}
