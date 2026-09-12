/**
 * REST 어댑터.
 *
 * <p>고객 API, 심사자 백오피스 API, 서비스 간 API.
 *
 * <p>도메인 규칙 위반은 4xx로 매핑한다. 500은 "우리가 예상하지 못한 것"에만 쓴다.
 * v1은 상태 전이 위반이 500으로 나갔다 — 재설계에서는 409다.
 *
 * <p>Phase 2에서 구현. 명세: {@code docs/design/05-api.md}
 */
package com.insurance.claims.adapter.web;
