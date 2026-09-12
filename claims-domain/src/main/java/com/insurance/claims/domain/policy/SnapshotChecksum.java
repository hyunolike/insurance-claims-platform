/*
 * ─────────────────────────────────────────────────────────────────────────────
 * 의도적 중복 (Shared Kernel)
 *
 * insurance-business-support 에도 같은 타입이 있다. 공유 라이브러리로 빼지 않는 것은
 * 결정이지 게으름이 아니다 — 공유 모듈은 두 바운디드 컨텍스트를 컴파일 타임에
 * 다시 묶어버려서, 컨텍스트를 나눈 의미를 없앤다.
 *
 * 대신 두 구현이 같은 답을 내는지는 계약 테스트가 검증한다.
 * 계약 파일(contracts/policy-snapshot-canonical-body.json)에 박힌 바이트열을
 * 양쪽이 각자의 알고리즘으로 해싱해 같은 값이 나오는지 본다.
 *
 * docs/design/01-context-map.md
 * ─────────────────────────────────────────────────────────────────────────────
 */
package com.insurance.claims.domain.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 계약 스냅샷 체크섬 — <b>검증하는 쪽</b>.
 *
 * <p>business-support가 스냅샷을 내려줄 때 이 값을 함께 준다. claims는 스냅샷 원문과
 * 체크섬을 함께 저장하고, <b>심사할 때마다</b> 저장된 원문으로 다시 계산해 대조한다.
 *
 * <p>불일치는 저장된 심사 근거가 변조되었거나 손상되었다는 뜻이다. 그 상태로 심사하면
 * 잘못된 근거로 지급·부지급이 결정되므로, 심사를 중단하고 수동심사로 회부한다(R-POL-011).
 * <b>불일치를 경고만 하고 진행하지 않는다.</b>
 *
 * <p>형식: {@code sha256:<64자리 hex>}. 계산 대상은 체크섬 필드를 제외한 응답 본문의
 * 정규화 JSON(키 정렬 + 공백 제거)이다.
 */
public final class SnapshotChecksum {

    private static final String PREFIX = "sha256:";
    private static final int HEX_LENGTH = 64;

    private final String value;

    private SnapshotChecksum(String value) {
        this.value = value;
    }

    /** 정규화된 JSON 문자열로부터 체크섬을 계산한다. */
    public static SnapshotChecksum of(String canonicalJson) {
        Objects.requireNonNull(canonicalJson, "정규화 JSON은 필수입니다.");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return new SnapshotChecksum(PREFIX + HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JRE가 제공해야 하는 알고리즘이다.
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    /** business-support가 준 값을 읽어들인다. */
    public static SnapshotChecksum parse(String value) {
        Objects.requireNonNull(value, "체크섬은 필수입니다.");
        if (!value.startsWith(PREFIX) || value.length() != PREFIX.length() + HEX_LENGTH) {
            throw new IllegalArgumentException("체크섬 형식이 올바르지 않습니다: " + value);
        }
        return new SnapshotChecksum(value);
    }

    /**
     * 저장된 스냅샷 원문이 변조되지 않았는가.
     *
     * <p>{@code false}면 심사를 진행하지 않는다.
     */
    public boolean matches(String canonicalJson) {
        return this.equals(of(canonicalJson));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SnapshotChecksum other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
