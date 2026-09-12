/**
 * Kafka 어댑터와 Outbox 릴레이.
 *
 * <p>릴레이는 {@code FOR UPDATE SKIP LOCKED}로 PENDING 이벤트를 집어 발행한다.
 * 인스턴스를 여러 개 띄워도 같은 행을 두 번 집지 않는다.
 *
 * <p>전달 보장은 최소 1회(at-least-once)다. 정확히 1회가 아니므로
 * 소비자는 {@code processed_event} 테이블로 멱등성을 지켜야 한다.
 *
 * <p>Phase 4에서 구현. 명세: {@code docs/design/04-events-and-integration.md}
 */
package com.insurance.claims.adapter.messaging;
