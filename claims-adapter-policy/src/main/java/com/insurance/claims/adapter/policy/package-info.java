/**
 * business-support 계약 스냅샷 조회 ACL (Anti-Corruption Layer).
 *
 * <p>업스트림의 모델을 그대로 들이지 않고 claims의 언어로 번역한다.
 * business-support의 스키마 변경이 claims 도메인까지 전파되지 않게 하기 위함이다.
 *
 * <p>이 어댑터가 실패해도 <b>청구 접수는 막지 않는다.</b>
 * 보험금 지급기한이 접수일부터 기산되므로, 접수 거부가 고객에게 더 불리하다.
 * 스냅샷을 확보하지 못하면 {@code snapshotStatus=PENDING}으로 접수하고 재시도한다.
 *
 * <p>Phase 2에서 구현. 명세: {@code docs/design/01-context-map.md} §3
 */
package com.insurance.claims.adapter.policy;
