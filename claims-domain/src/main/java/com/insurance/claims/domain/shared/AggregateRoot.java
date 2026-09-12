package com.insurance.claims.domain.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 애그리거트 루트.
 *
 * <p><b>이벤트는 애그리거트가 기록한다.</b> 서비스가 만들지 않는다.
 *
 * <p>v1은 {@code ClaimService}가 이벤트를 생성해 발행했다. 그래서 상태를 바꾸는
 * 새 경로가 생길 때마다 이벤트 발행을 빠뜨릴 수 있었고, 실제로 도메인 규칙과
 * 이벤트가 따로 놀았다. 상태 전이 메서드 안에서 {@link #record(DomainEvent)}를
 * 호출하면 전이와 이벤트가 같은 자리에 있어 누락이 구조적으로 불가능해진다.
 *
 * <p>애플리케이션 계층은 {@link #pullEvents()}로 이벤트를 꺼내
 * <b>같은 트랜잭션에서</b> Outbox 테이블에 INSERT한다.
 * 트랜잭션 안에서 외부(Kafka)로 직접 발행하지 않는다 — 롤백되면 되돌릴 수 없다.
 *
 * @see docs/design/02-domain-model.md §7
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    /** 상태 전이 메서드 안에서 호출한다. */
    protected void record(DomainEvent event) {
        pendingEvents.add(Objects.requireNonNull(event, "도메인 이벤트는 null일 수 없습니다."));
    }

    /**
     * 기록된 이벤트를 꺼내고 비운다.
     *
     * <p>두 번 호출하면 두 번째는 빈 목록이다. 같은 이벤트가 Outbox에
     * 중복 INSERT되는 것을 막기 위한 의도된 동작이다.
     */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> drained = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return drained;
    }

    /** 아직 꺼내지 않은 이벤트를 들여다본다. 주로 테스트용. */
    public List<DomainEvent> peekEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public boolean hasPendingEvents() {
        return !pendingEvents.isEmpty();
    }
}
