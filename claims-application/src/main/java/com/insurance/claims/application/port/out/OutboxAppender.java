package com.insurance.claims.application.port.out;

import com.insurance.claims.domain.shared.DomainEvent;
import java.util.List;

/**
 * 도메인 이벤트를 Outbox에 적재하는 포트.
 *
 * <p><b>여기서 외부로 발행하지 않는다.</b> 구현체는 이벤트를 DB 테이블에 INSERT만 하며,
 * 실제 Kafka 발행은 별도 릴레이가 담당한다.
 *
 * <p>그래야 상태 변경과 이벤트 저장이 <b>하나의 트랜잭션</b>이 된다.
 * 커밋되면 이벤트도 있고, 롤백되면 이벤트도 없다.
 *
 * <pre>{@code
 * @Transactional
 * public void approve(ApproveCommand cmd) {
 *     Claim claim = claimRepository.findById(cmd.claimId()).orElseThrow();
 *     claim.approve(cmd.reviewer());        // 애그리거트가 이벤트 record()
 *     claimRepository.save(claim);
 *     outboxAppender.append(claim.pullEvents());   // 같은 트랜잭션의 INSERT
 * }   // ← 커밋 시점에 상태와 이벤트가 원자적으로 함께 저장된다
 * }</pre>
 *
 * @see docs/design/04-events-and-integration.md §4
 */
public interface OutboxAppender {

    /**
     * 이벤트를 Outbox에 적재한다. 호출자의 트랜잭션에 참여한다.
     *
     * @param events 애그리거트에서 {@code pullEvents()}로 꺼낸 이벤트. 비어 있으면 아무 일도 하지 않는다.
     */
    void append(List<? extends DomainEvent> events);
}
