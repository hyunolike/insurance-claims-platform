package com.insurance.claim.domain.event;

import java.time.LocalDateTime;

/**
 * 모든 도메인 이벤트의 기본 인터페이스
 * 순수 Java - 프레임워크 의존성 없음
 */
public interface DomainEvent {

    /**
     * 이벤트 발생 시각
     */
    LocalDateTime occurredOn();

    /**
     * 이벤트 타입 식별자 (Kafka topic routing에 사용)
     */
    String eventType();
}
