package com.insurance.claim.domain.event;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트 기본 인터페이스
 * 모든 도메인 이벤트는 이 인터페이스를 구현해야 함
 * 순수 Java - 프레임워크 의존성 없음
 */
public interface DomainEvent {

    /**
     * 이벤트가 발생한 시간
     */
    LocalDateTime occurredAt();

    /**
     * 이벤트 타입 (Kafka topic routing에 사용)
     */
    String eventType();
}
