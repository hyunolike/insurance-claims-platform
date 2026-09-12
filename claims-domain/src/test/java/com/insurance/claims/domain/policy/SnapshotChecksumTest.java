package com.insurance.claims.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 스냅샷 체크섬.
 *
 * <p>이 값이 하는 일은 하나다 — <b>저장된 심사 근거가 그때 받은 그대로인지</b> 확인한다.
 * 청구는 접수부터 지급까지 며칠이 걸리고 재심사는 몇 달 뒤에도 일어난다.
 * 그 사이 저장된 스냅샷이 손상되거나 변조되면, 잘못된 근거로 지급·부지급이 결정된다.
 *
 * <p>business-support 와 같은 알고리즘을 쓰는지는 계약 테스트가 본다
 * ({@code SnapshotChecksumContractTest}). 여기서는 이 구현 자체의 규칙을 고정한다.
 */
@DisplayName("스냅샷 체크섬")
class SnapshotChecksumTest {

    private static final String BODY = "{\"policyNo\":\"P2026-0001234\",\"insuredAmount\":50000000}";

    @Nested
    @DisplayName("계산")
    class Calculation {

        @Test
        @DisplayName("같은 입력은 항상 같은 값")
        void shouldBeDeterministic() {
            assertThat(SnapshotChecksum.of(BODY)).isEqualTo(SnapshotChecksum.of(BODY));
        }

        @Test
        @DisplayName("sha256: + 64자리 hex 형식")
        void shouldUseAgreedFormat() {
            assertThat(SnapshotChecksum.of(BODY).value()).matches("^sha256:[0-9a-f]{64}$");
        }

        @Test
        @DisplayName("★ 한 글자만 달라도 값이 완전히 달라진다")
        void shouldChangeCompletelyOnTinyEdit() {
            String tampered = BODY.replace("50000000", "50000001");

            assertThat(SnapshotChecksum.of(tampered)).isNotEqualTo(SnapshotChecksum.of(BODY));
        }

        @Test
        @DisplayName("UTF-8로 해싱한다 — 한글이 섞여도 플랫폼 기본 인코딩에 흔들리지 않는다")
        void shouldHashAsUtf8() {
            String korean = "{\"target\":\"척추 및 그 부속기관\"}";

            // 기대값은 UTF-8 바이트에 대한 SHA-256이다. 플랫폼 기본 인코딩을 쓰면
            // 같은 문자열이 OS에 따라 다른 체크섬을 내고, 배포 환경이 바뀌는 순간
            // 저장된 스냅샷이 전부 검증 실패한다.
            assertThat(SnapshotChecksum.of(korean).value())
                    .isEqualTo(SnapshotChecksum.of(new String(
                            korean.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            java.nio.charset.StandardCharsets.UTF_8)).value());
        }

        @Test
        @DisplayName("null 입력은 거부한다")
        void shouldRejectNull() {
            assertThatThrownBy(() -> SnapshotChecksum.of(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("검증")
    class Verification {

        @Test
        @DisplayName("원문이 그대로면 일치한다")
        void shouldMatchIntactBody() {
            SnapshotChecksum checksum = SnapshotChecksum.of(BODY);

            assertThat(checksum.matches(BODY)).isTrue();
        }

        @Test
        @DisplayName("★ 가입금액이 바뀌면 불일치 — 심사 중단 사유다 (R-POL-011)")
        void shouldDetectTamperedAmount() {
            SnapshotChecksum checksum = SnapshotChecksum.of(BODY);
            String tampered = BODY.replace("50000000", "500000000");

            assertThat(checksum.matches(tampered))
                    .as("불일치를 경고만 하고 진행하면 잘못된 근거로 지급된다")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("파싱")
    class Parsing {

        @Test
        @DisplayName("business-support가 준 값을 읽는다")
        void shouldParseProvidedValue() {
            String value = SnapshotChecksum.of(BODY).value();

            assertThat(SnapshotChecksum.parse(value).value()).isEqualTo(value);
        }

        @Test
        @DisplayName("접두사가 없으면 거부한다")
        void shouldRejectWithoutPrefix() {
            String noPrefix = SnapshotChecksum.of(BODY).value().substring("sha256:".length());

            assertThatThrownBy(() -> SnapshotChecksum.parse(noPrefix))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("형식이 올바르지 않습니다");
        }

        @Test
        @DisplayName("길이가 다르면 거부한다 — 잘린 값을 조용히 받아들이지 않는다")
        void shouldRejectWrongLength() {
            assertThatThrownBy(() -> SnapshotChecksum.parse("sha256:abc123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null은 거부한다")
        void shouldRejectNull() {
            assertThatThrownBy(() -> SnapshotChecksum.parse(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 값이면 같다")
        void shouldBeEqualByValue() {
            SnapshotChecksum a = SnapshotChecksum.of(BODY);
            SnapshotChecksum b = SnapshotChecksum.parse(a.value());

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        @DisplayName("다른 타입·null과는 다르다")
        void shouldNotEqualOtherTypes() {
            SnapshotChecksum checksum = SnapshotChecksum.of(BODY);

            assertThat(checksum).isNotEqualTo(checksum.value()).isNotEqualTo(null);
        }

        @Test
        @DisplayName("toString은 값 그대로 — 체크섬은 민감정보가 아니다")
        void shouldExposeValueInToString() {
            SnapshotChecksum checksum = SnapshotChecksum.of(BODY);

            assertThat(checksum).hasToString(checksum.value());
        }
    }
}
