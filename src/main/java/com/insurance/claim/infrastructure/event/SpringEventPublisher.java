package com.insurance.claim.infrastructure.event;

import com.insurance.claim.domain.event.DomainEvent;
import com.insurance.claim.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationEventPublisher를 사용한 동기 이벤트 발행
 * Phase 2: 동기 이벤트 처리
 * Phase 3에서 Kafka 비동기 처리로 확장 예정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {} at {}", event.eventType(), event.occurredAt());
        applicationEventPublisher.publishEvent(event);
        log.info("Domain event published: {}", event.eventType());
    }
}
