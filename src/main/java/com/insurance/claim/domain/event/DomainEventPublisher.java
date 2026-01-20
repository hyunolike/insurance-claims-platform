package com.insurance.claim.domain.event;

import java.util.List;

/**
 * 도메인 이벤트 발행을 위한 인터페이스
 * 순수 Java - 프레임워크 의존성 없음
 */
public interface DomainEventPublisher {

    /**
     * 단일 도메인 이벤트 발행
     */
    void publish(DomainEvent event);

    /**
     * 여러 도메인 이벤트 일괄 발행
     */
    default void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}