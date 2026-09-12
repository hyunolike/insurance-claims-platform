/**
 * 외부 시스템 어댑터 — 이체, 알림, 예금주 실명조회, 타사 실손 조회, FDS.
 *
 * <p>이번 범위에서는 스텁으로 구현한다. 다만 {@code return true}로 두지 않는다.
 * 지연·실패·타임아웃을 재현할 수 있어야 그 경로를 테스트할 수 있고,
 * 스텁의 값어치는 거기서 나온다.
 *
 * <p>특히 이체 타임아웃은 <b>실패가 아니다.</b> 성공인지 실패인지 모르는 상태이므로
 * 재시도하면 중복 지급 위험이 있다. 조회 대사로 확인될 때까지 PAID로 전환하지 않는다.
 *
 * <p>Phase 4에서 구현. 명세: {@code docs/design/04-events-and-integration.md} §7
 */
package com.insurance.claims.adapter.external;
