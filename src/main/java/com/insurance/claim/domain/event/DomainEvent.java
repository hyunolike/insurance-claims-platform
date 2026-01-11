package com.insurance.claim.domain.event;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트 기본 인터페이스
 * 모든 도메인 이벤트는 이 인터페이스를 구현해야 함
 */
public interface DomainEvent {

    /**
     * 이벤트가 발생한 시간
     */
    LocalDateTime occurredAt();

    /**
     * 이벤트 타입 (이벤트 식별자)
     */
    String eventType();
}