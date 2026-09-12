package com.insurance.claims.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.claims.domain.policy.SnapshotChecksum;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * ★ 계약 테스트 — <b>소비자 쪽</b>.
 *
 * <p>이 저장소는 계약의 <b>소비자</b>다. 계약 파일은 여기서 선언하고
 * business-support 로 동기화한다. 두 레포가 같은 파일을 들고, 각자의 관점에서 검증한다.
 *
 * <pre>
 *   business-support : 렌더한 결과가 정확히 이 바이트인가?        (제공자 의무)
 *   claims (여기)     : 이 바이트를 해싱하면 이 체크섬이 나오는가?  (소비자 기대)
 * </pre>
 *
 * <p><b>한쪽만 있으면 순환 검증이다.</b> 자기가 만든 값을 자기가 확인하는 것이므로
 * "오늘의 동작이 바뀌지 않았다"는 것만 알 수 있고, 상대와 합의했는지는 알 수 없다.
 *
 * <p>이것이 실제로 막는 사고: 어느 한쪽이 Jackson 설정을 바꾸거나(들여쓰기, null 제외)
 * 필드 표현을 바꾸면({@code 0.20} → {@code 0.2}), <b>이미 저장된 모든 스냅샷의
 * 무결성 검증이 실패한다.</b> 그때는 심사가 전부 수동 회부로 빠진다(R-POL-011).
 * 그 사고를 배포가 아니라 PR에서 잡는다.
 *
 * @see docs/design/01-context-map.md
 */
@Tag("contract")
@DisplayName("계약 — 스냅샷 체크섬 알고리즘 합의")
class SnapshotChecksumContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("★★ 계약 파일의 바이트열에서 계약 파일의 체크섬이 나온다")
    void shouldAgreeOnChecksumAlgorithm() throws Exception {
        JsonNode contract = readContract("policy-snapshot-canonical-body.json");

        String canonicalBody = contract.get("canonicalBody").asText();
        String expected = contract.get("checksum").asText();

        assertThat(SnapshotChecksum.of(canonicalBody).value())
                .as("business-support와 다른 값이 나오면, 저장된 스냅샷의 검증이 전부 실패한다")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("저장된 원문이 한 글자라도 바뀌면 불일치를 잡아낸다")
    void shouldDetectTampering() throws Exception {
        JsonNode contract = readContract("policy-snapshot-canonical-body.json");
        SnapshotChecksum checksum = SnapshotChecksum.parse(contract.get("checksum").asText());
        String original = contract.get("canonicalBody").asText();

        assertThat(checksum.matches(original)).isTrue();

        // 가입금액을 5천만 → 5억으로. 이런 변조가 통과하면 잘못된 근거로 지급된다.
        String tampered = original.replace("\"insuredAmount\":50000000",
                "\"insuredAmount\":500000000");
        assertThat(tampered).isNotEqualTo(original);
        assertThat(checksum.matches(tampered))
                .as("불일치는 경고가 아니라 심사 중단 사유다 (R-POL-011)")
                .isFalse();
    }

    @Test
    @DisplayName("★ 계약이 금지한 필드가 스냅샷 본문에 없다 — 받아서도 안 되는 정보")
    void shouldNotReceiveForbiddenFields() throws Exception {
        JsonNode forbidden = readContract("policy-snapshot-forbidden-fields.json")
                .get("forbiddenFieldNames");
        String body = readContract("policy-snapshot-canonical-body.json")
                .get("canonicalBody").asText();

        JsonNode parsed = MAPPER.readTree(body);
        for (JsonNode name : forbidden) {
            // 소비자도 함께 확인한다. 받지 않겠다고 선언한 것을 실제로 안 받는지
            // 제공자만 검사하면, 계약 파일이 갱신됐을 때 여기가 조용히 뒤처진다.
            assertThat(containsField(parsed, name.asText()))
                    .as("%s 를 받으면 우리 DB에도 남는다. 필요 없는 개인정보는 받지 않는다",
                            name.asText())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("체크섬 형식이 계약대로다 — sha256: + 64자리 hex")
    void shouldUseAgreedFormat() throws Exception {
        String checksum = readContract("policy-snapshot-canonical-body.json")
                .get("checksum").asText();

        assertThat(checksum).matches("^sha256:[0-9a-f]{64}$");
        assertThat(SnapshotChecksum.parse(checksum).value()).isEqualTo(checksum);
    }

    private boolean containsField(JsonNode node, String fieldName) {
        if (node.isObject()) {
            if (node.has(fieldName)) {
                return true;
            }
            for (JsonNode child : node) {
                if (containsField(child, fieldName)) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsField(child, fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonNode readContract(String fileName) throws Exception {
        String path = "/contracts/" + fileName;
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("계약 파일이 없습니다: " + path);
            }
            return MAPPER.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
