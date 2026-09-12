package com.insurance.claims.domain.shared.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Money")
class MoneyTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("0원 이상이면 생성된다")
        void shouldCreateWithNonNegativeAmount() {
            assertThat(Money.ofWon(0L)).isEqualTo(Money.ZERO);
            assertThat(Money.ofWon(124_000L).toWon()).isEqualTo(124_000L);
        }

        @Test
        @DisplayName("음수면 거부한다")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Money.ofWon(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("음수");
        }

        @Test
        @DisplayName("BigDecimal의 소수부는 거부한다 — 조용한 반올림은 금액 오차의 원인이 된다")
        void shouldRejectFractionalAmount() {
            assertThatThrownBy(() -> Money.ofWon(new BigDecimal("1000.50")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("소수부");
        }

        @Test
        @DisplayName("소수부가 0이면 허용한다 (1000.00 == 1000)")
        void shouldAcceptTrailingZeroScale() {
            assertThat(Money.ofWon(new BigDecimal("1000.00"))).isEqualTo(Money.ofWon(1000L));
        }

        @Test
        @DisplayName("null BigDecimal은 거부한다")
        void shouldRejectNullBigDecimal() {
            assertThatThrownBy(() -> Money.ofWon((BigDecimal) null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("연산")
    class Arithmetic {

        @Test
        @DisplayName("더한다")
        void shouldAdd() {
            assertThat(Money.ofWon(40_000L).plus(Money.ofWon(84_000L)))
                    .isEqualTo(Money.ofWon(124_000L));
        }

        @Test
        @DisplayName("뺀다")
        void shouldSubtract() {
            assertThat(Money.ofWon(60_000L).minus(Money.ofWon(20_000L)))
                    .isEqualTo(Money.ofWon(40_000L));
        }

        @Test
        @DisplayName("뺄셈 결과가 음수면 거부한다")
        void shouldRejectNegativeSubtraction() {
            assertThatThrownBy(() -> Money.ofWon(10_000L).minus(Money.ofWon(20_000L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("음수");
        }

        @Test
        @DisplayName("minusToZero는 음수 대신 0을 반환한다 — 자기부담금이 대상액을 넘는 경우")
        void shouldFloorToZero() {
            assertThat(Money.ofWon(10_000L).minusToZero(Money.ofWon(20_000L)))
                    .isEqualTo(Money.ZERO);
            assertThat(Money.ofWon(60_000L).minusToZero(Money.ofWon(20_000L)))
                    .isEqualTo(Money.ofWon(40_000L));
        }

        @ParameterizedTest(name = "{0}원 × {1} = {2}원")
        @DisplayName("비율 연산 결과는 원 미만 절사한다")
        @CsvSource({
                "60000, 0.20, 12000",
                "120000, 0.30, 36000",
                "12345, 0.20, 2469",
                "10001, 0.30, 3000",   // 3000.3 → 3000 (절사)
                "1, 0.99, 0"           // 0.99 → 0
        })
        void shouldMultiplyAndFloor(long base, String rate, long expected) {
            assertThat(Money.ofWon(base).multiply(new BigDecimal(rate)))
                    .isEqualTo(Money.ofWon(expected));
        }

        @Test
        @DisplayName("음수 비율은 거부한다")
        void shouldRejectNegativeRate() {
            assertThatThrownBy(() -> Money.ofWon(1000L).multiply(new BigDecimal("-0.1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("음수");
        }

        @Test
        @DisplayName("null 비율은 거부한다")
        void shouldRejectNullRate() {
            assertThatThrownBy(() -> Money.ofWon(1000L).multiply(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("overflow는 조용히 넘어가지 않는다")
        void shouldDetectOverflow() {
            Money huge = Money.ofWon(Long.MAX_VALUE);
            assertThatThrownBy(() -> huge.plus(Money.ofWon(1L)))
                    .isInstanceOf(ArithmeticException.class);
        }
    }

    @Nested
    @DisplayName("비교")
    class Comparison {

        @Test
        @DisplayName("자기부담금은 max(정률, 최소공제금액) — 실손 계산의 핵심 구조")
        void shouldPickLarger() {
            // 상급종합병원 통원: 정률 12,000 vs 최소공제 20,000 → 20,000
            assertThat(Money.max(Money.ofWon(12_000L), Money.ofWon(20_000L)))
                    .isEqualTo(Money.ofWon(20_000L));
            // 비급여: 정률 36,000 vs 최소공제 30,000 → 36,000
            assertThat(Money.max(Money.ofWon(36_000L), Money.ofWon(30_000L)))
                    .isEqualTo(Money.ofWon(36_000L));
        }

        @Test
        @DisplayName("한도 적용은 min — 잔여 한도와 계산액 중 작은 쪽")
        void shouldPickSmaller() {
            assertThat(Money.min(Money.ofWon(84_000L), Money.ofWon(50_000L)))
                    .isEqualTo(Money.ofWon(50_000L));
            assertThat(Money.min(Money.ofWon(30_000L), Money.ofWon(50_000L)))
                    .isEqualTo(Money.ofWon(30_000L));
        }

        @Test
        @DisplayName("같은 값이면 어느 쪽을 돌려줘도 동등하다")
        void shouldHandleEqualOperands() {
            Money a = Money.ofWon(1000L);
            Money b = Money.ofWon(1000L);
            assertThat(Money.max(a, b)).isEqualTo(a);
            assertThat(Money.min(a, b)).isEqualTo(a);
        }

        @Test
        @DisplayName("대소 비교")
        void shouldCompare() {
            Money small = Money.ofWon(100L);
            Money large = Money.ofWon(200L);

            assertThat(large.isGreaterThan(small)).isTrue();
            assertThat(small.isGreaterThan(large)).isFalse();
            assertThat(small.isLessThan(large)).isTrue();
            assertThat(large.isLessThan(small)).isFalse();
            assertThat(small).isLessThan(large);
        }

        @Test
        @DisplayName("0원 판별")
        void shouldDetectZero() {
            assertThat(Money.ZERO.isZero()).isTrue();
            assertThat(Money.ofWon(1L).isZero()).isFalse();
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 금액은 같다")
        void shouldBeEqualByValue() {
            assertThat(Money.ofWon(1000L))
                    .isEqualTo(Money.ofWon(1000L))
                    .hasSameHashCodeAs(Money.ofWon(1000L));
        }

        @Test
        @DisplayName("다른 금액·다른 타입·null과는 다르다")
        void shouldNotBeEqualToOthers() {
            Money money = Money.ofWon(1000L);
            assertThat(money).isNotEqualTo(Money.ofWon(2000L));
            assertThat(money).isNotEqualTo("1000");
            assertThat(money).isNotEqualTo(null);
            assertThat(money).isEqualTo(money);
        }

        @Test
        @DisplayName("사람이 읽을 수 있게 표시한다")
        void shouldRenderReadably() {
            assertThat(Money.ofWon(124_000L)).hasToString("124000원");
        }
    }

    @Test
    @DisplayName("실손 통원 산출: 청구 300,000원 → 지급 124,000원")
    void shouldReproduceOutpatientSettlement() {
        // docs/design/03-adjudication.md §4 예시 1
        // 급여 본인부담금 60,000 / 비급여 120,000 / 공단부담 120,000(보상 대상 아님)
        Money coveredBase = Money.ofWon(60_000L);
        Money uncoveredBase = Money.ofWon(120_000L);

        Money coveredDeductible = Money.max(
                coveredBase.multiply(new BigDecimal("0.20")),   // 12,000
                Money.ofWon(20_000L));                          // 상급종합 최소공제
        Money uncoveredDeductible = Money.max(
                uncoveredBase.multiply(new BigDecimal("0.30")), // 36,000
                Money.ofWon(30_000L));

        Money payable = coveredBase.minusToZero(coveredDeductible)
                .plus(uncoveredBase.minusToZero(uncoveredDeductible));

        assertThat(coveredDeductible).isEqualTo(Money.ofWon(20_000L));
        assertThat(uncoveredDeductible).isEqualTo(Money.ofWon(36_000L));
        assertThat(payable).isEqualTo(Money.ofWon(124_000L));

        // 회당 한도 200,000 이내
        assertThatNoException().isThrownBy(() -> Money.ofWon(200_000L).minus(payable));
    }
}
