package com.insurance.claims.domain.shared;

import java.time.Instant;

/**
 * 도메인 이벤트.
 *
 * <p>이벤트는 <b>이미 일어난 사실</b>이다. {@code ClaimApproved}(O) / {@code ApproveClaim}(X).
 * 발행 측은 누가 구독하는지 알지 못한다.
 *
 * <p>구현체는 반드시 불변이어야 하며, 민감정보(성명·계좌번호·KCD 코드·진단명)를
 * 필드로 갖지 않는다. 이벤트는 Kafka를 통해 컨텍스트 밖으로 나가므로
 * 한 번 실리면 통제할 수 없다.
 *
 * @see docs/design/04-events-and-integration.md
 */
public interface DomainEvent {

    /** 중복 제거 키. 소비자는 이 값으로 멱등성을 보장한다. */
    EventId eventId();

    /** Kafka 토픽 라우팅과 소비자 분기에 사용. 예: {@code claim.received} */
    String eventType();

    /** 이 이벤트가 속한 애그리거트의 대외 식별자. Kafka 파티션 키로 쓰인다. */
    String aggregateId();

    /** 사실이 발생한 시각. 발행 시각이 아니다. */
    Instant occurredAt();
}
