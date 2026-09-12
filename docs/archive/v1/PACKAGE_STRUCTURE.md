# 패키지 구조 가이드

## 📋 목차
- [전체 구조 개요](#전체-구조-개요)
- [레이어별 상세 설명](#레이어별-상세-설명)
  - [1. API 레이어](#1-api-레이어)
  - [2. Application 레이어](#2-application-레이어)
  - [3. Domain 레이어](#3-domain-레이어)
  - [4. Infrastructure 레이어](#4-infrastructure-레이어)
  - [5. Config 레이어](#5-config-레이어)
- [의존성 규칙](#의존성-규칙)
- [실전 예제](#실전-예제)

---

## 전체 구조 개요

```
src/main/java/com/insurance/claim/
├── api/                              # 프레젠테이션 레이어
│   ├── controller/                   # REST API 컨트롤러
│   ├── dto/
│   │   ├── request/                  # API 요청 DTO
│   │   └── response/                 # API 응답 DTO
│   └── exception/                    # 글로벌 예외 핸들러
│
├── application/                      # 애플리케이션 레이어
│   ├── service/                      # 애플리케이션 서비스
│   ├── usecase/                      # 유즈케이스 (선택적)
│   └── dto/                          # 애플리케이션 내부 DTO
│
├── domain/                           # 도메인 레이어
│   ├── model/                        # 엔티티, Aggregate
│   ├── vo/                           # Value Object
│   ├── repository/                   # 리포지토리 인터페이스
│   ├── service/                      # 도메인 서비스
│   └── event/                        # 도메인 이벤트
│
├── infrastructure/                   # 인프라 레이어
│   ├── persistence/                  # JPA 구현체
│   ├── external/                     # 외부 API 클라이언트
│   └── messaging/                    # 메시징
│
└── config/                           # 설정 레이어
    ├── security/                     # 보안 설정
    ├── database/                     # 데이터베이스 설정
    └── properties/                   # 프로퍼티 설정
```

### 아키텍처 스타일
- **헥사고날 아키텍처 (Hexagonal Architecture)**
- **클린 아키텍처 (Clean Architecture)**
- **DDD (Domain-Driven Design)**

---

## 레이어별 상세 설명

## 1. API 레이어

> 외부 세계와의 통신 창구. HTTP 요청/응답을 처리합니다.

### 1.1 controller/

**역할**: REST API 엔드포인트 정의

**책임**:
- HTTP 요청 수신 및 검증
- Application Service 호출
- HTTP 응답 반환

**예시 코드**:
```java
package com.insurance.claim.api.controller;

import com.insurance.claim.api.dto.request.ClaimCreateRequest;
import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(
            @Valid @RequestBody ClaimCreateRequest request) {
        ClaimResponse response = claimService.createClaim(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable Long claimId) {
        ClaimResponse response = claimService.getClaim(claimId);
        return ResponseEntity.ok(response);
    }
}
```

### 1.2 dto/request/

**역할**: 클라이언트로부터 받는 요청 데이터 정의

**책임**:
- 입력 데이터 구조 정의
- 유효성 검증 (Bean Validation)

**예시 코드**:
```java
package com.insurance.claim.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ClaimCreateRequest {

    @NotBlank(message = "보험 증권 번호는 필수입니다")
    private String policyNumber;

    @NotNull(message = "사고 일자는 필수입니다")
    @PastOrPresent(message = "사고 일자는 과거 또는 현재여야 합니다")
    private LocalDate accidentDate;

    @NotBlank(message = "사고 내용은 필수입니다")
    @Size(min = 10, max = 1000, message = "사고 내용은 10자 이상 1000자 이하여야 합니다")
    private String description;

    @NotNull(message = "청구 금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "청구 금액은 0보다 커야 합니다")
    private BigDecimal claimAmount;

    @NotBlank(message = "청구인 이름은 필수입니다")
    private String claimantName;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
}
```

### 1.3 dto/response/

**역할**: 클라이언트에게 반환하는 응답 데이터 정의

**예시 코드**:
```java
package com.insurance.claim.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ClaimResponse {

    private Long claimId;
    private String policyNumber;
    private String claimNumber;
    private LocalDate accidentDate;
    private String description;
    private BigDecimal claimAmount;
    private String status;
    private String claimantName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 1.4 exception/

**역할**: 전역 예외 처리 및 커스텀 예외 정의

**예시 코드**:
```java
package com.insurance.claim.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClaimNotFound(ClaimNotFoundException e) {
        log.error("청구 정보를 찾을 수 없음: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(e.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message("입력값 검증에 실패했습니다")
                .validationErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception e) {
        log.error("예상치 못한 에러 발생", e);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("서버 내부 오류가 발생했습니다")
                .build();

        return ResponseEntity.internalServerError().body(error);
    }
}
```

```java
package com.insurance.claim.api.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;
}
```

```java
package com.insurance.claim.api.exception;

public class ClaimNotFoundException extends RuntimeException {
    public ClaimNotFoundException(Long claimId) {
        super("청구 ID " + claimId + "를 찾을 수 없습니다");
    }
}
```

---

## 2. Application 레이어

> 비즈니스 로직을 조율하는 레이어. 트랜잭션 경계를 정의합니다.

### 2.1 service/

**역할**: 유즈케이스 구현 및 트랜잭션 관리

**책임**:
- 비즈니스 로직 조율
- 트랜잭션 관리
- Domain 객체 간 협력 조정
- DTO 변환

**중요: 인터페이스가 필요한가?**
- **대부분의 경우 필요 없습니다!** 구현체만 만드세요
- Service 인터페이스를 만들던 전통은 과거 패턴입니다
- Spring AOP는 CGLIB으로 클래스도 프록시 생성 가능
- Mockito로 클래스도 Mock 가능
- 실제로 여러 구현체가 있을 때만 인터페이스 고려

**예시 코드**:
```java
package com.insurance.claim.application.service;

import com.insurance.claim.api.dto.request.ClaimCreateRequest;
import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.api.exception.ClaimNotFoundException;
import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.repository.ClaimRepository;
import com.insurance.claim.domain.service.ClaimDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimDomainService claimDomainService;

    @Transactional
    public ClaimResponse createClaim(ClaimCreateRequest request) {
        log.info("청구 생성 시작: policyNumber={}", request.getPolicyNumber());

        // 1. Domain 객체 생성
        Claim claim = Claim.builder()
                .policyNumber(request.getPolicyNumber())
                .accidentDate(request.getAccidentDate())
                .description(request.getDescription())
                .claimAmount(request.getClaimAmount())
                .claimantName(request.getClaimantName())
                .email(request.getEmail())
                .build();

        // 2. Domain Service를 통한 비즈니스 규칙 검증
        claimDomainService.validateClaim(claim);

        // 3. 저장
        Claim savedClaim = claimRepository.save(claim);

        log.info("청구 생성 완료: claimId={}", savedClaim.getId());

        // 4. Response 변환
        return toResponse(savedClaim);
    }

    public ClaimResponse getClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));

        return toResponse(claim);
    }

    @Transactional
    public void approveClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));

        // Domain 객체의 비즈니스 메서드 호출
        claim.approve();

        // 변경 감지로 자동 저장
    }

    private ClaimResponse toResponse(Claim claim) {
        return ClaimResponse.builder()
                .claimId(claim.getId())
                .policyNumber(claim.getPolicyNumber())
                .claimNumber(claim.getClaimNumber())
                .accidentDate(claim.getAccidentDate())
                .description(claim.getDescription())
                .claimAmount(claim.getClaimAmount())
                .status(claim.getStatus().name())
                .claimantName(claim.getClaimantName())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
```

### 2.2 usecase/

**역할**: 특정 유즈케이스를 명시적으로 표현 (선택적)

**언제 사용?**:
- 복잡한 비즈니스 흐름을 명확히 구분하고 싶을 때
- CQRS 패턴을 적용할 때
- Service가 너무 커질 때

**예시 코드**:
```java
package com.insurance.claim.application.usecase;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.repository.ClaimRepository;
import com.insurance.claim.infrastructure.messaging.ClaimEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 청구 승인 유즈케이스
 *
 * 비즈니스 흐름:
 * 1. 청구 정보 조회
 * 2. 승인 가능 여부 검증
 * 3. 청구 승인 처리
 * 4. 승인 완료 이벤트 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApproveClaimUseCase {

    private final ClaimRepository claimRepository;
    private final ClaimEventPublisher eventPublisher;

    @Transactional
    public void execute(Long claimId, String approverName) {
        log.info("청구 승인 유즈케이스 시작: claimId={}, approver={}", claimId, approverName);

        // 1. 청구 조회
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("청구를 찾을 수 없습니다"));

        // 2. 승인 처리
        claim.approve();
        claim.setApproverName(approverName);

        // 3. 이벤트 발행
        eventPublisher.publishClaimApproved(claim);

        log.info("청구 승인 완료: claimId={}", claimId);
    }
}
```

### 2.3 dto/

**역할**: 애플리케이션 레이어 내부에서 사용하는 DTO

**언제 사용?**:
- API DTO와 Domain 객체 사이의 중간 데이터 전달
- 여러 Domain 객체의 정보를 조합할 때

**예시 코드**:
```java
package com.insurance.claim.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ClaimSummaryDto {
    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private BigDecimal claimAmount;
    private String status;
    private String claimantName;

    // 여러 집계 정보를 담을 수 있음
    private int documentCount;
    private boolean hasAllRequiredDocuments;
}
```

---

## 3. Domain 레이어

> 핵심 비즈니스 로직이 있는 레이어. 외부 의존성이 없어야 합니다.

### 3.1 model/

**역할**: 비즈니스 엔티티 및 Aggregate 정의

**책임**:
- 비즈니스 규칙 포함
- 상태 변경 로직
- 도메인 불변성 유지

**예시 코드**:
```java
package com.insurance.claim.domain.model;

import com.insurance.claim.domain.vo.ClaimNumber;
import com.insurance.claim.domain.vo.Money;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Claim {

    private Long id;
    private String policyNumber;
    private ClaimNumber claimNumber;
    private LocalDate accidentDate;
    private String description;
    private Money claimAmount;
    private ClaimStatus status;
    private String claimantName;
    private String email;
    private String approverName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Claim(String policyNumber, LocalDate accidentDate, String description,
                 BigDecimal claimAmount, String claimantName, String email) {
        this.policyNumber = policyNumber;
        this.claimNumber = ClaimNumber.generate();
        this.accidentDate = accidentDate;
        this.description = description;
        this.claimAmount = new Money(claimAmount);
        this.status = ClaimStatus.PENDING;
        this.claimantName = claimantName;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void approve() {
        if (this.status != ClaimStatus.PENDING) {
            throw new IllegalStateException("대기 중인 청구만 승인할 수 있습니다");
        }
        this.status = ClaimStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (this.status != ClaimStatus.PENDING) {
            throw new IllegalStateException("대기 중인 청구만 거부할 수 있습니다");
        }
        this.status = ClaimStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isApprovable() {
        return this.status == ClaimStatus.PENDING
                && this.claimAmount.isLessThanOrEqual(new Money(new BigDecimal("1000000")));
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
        this.updatedAt = LocalDateTime.now();
    }
}
```

```java
package com.insurance.claim.domain.model;

public enum ClaimStatus {
    PENDING("대기중"),
    APPROVED("승인"),
    REJECTED("거부"),
    PAID("지급완료"),
    CANCELLED("취소");

    private final String description;

    ClaimStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### 3.2 vo/ (Value Object)

**역할**: 불변 값 객체 정의

**특징**:
- 불변성 (Immutable)
- 동등성 비교 (equals, hashCode)
- 비즈니스 의미를 가진 값

**예시 코드**:
```java
package com.insurance.claim.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 청구 번호 Value Object
 *
 * 형식: CLM-YYYYMMDD-XXXXX
 * 예: CLM-20250115-A3F9E
 */
@Getter
@EqualsAndHashCode
public class ClaimNumber {

    private static final String PREFIX = "CLM";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String value;

    private ClaimNumber(String value) {
        this.value = value;
    }

    public static ClaimNumber generate() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String uniquePart = UUID.randomUUID().toString()
                .substring(0, 5).toUpperCase();
        String claimNumber = String.format("%s-%s-%s", PREFIX, datePart, uniquePart);
        return new ClaimNumber(claimNumber);
    }

    public static ClaimNumber from(String value) {
        if (value == null || !value.matches("^CLM-\\d{8}-[A-Z0-9]{5}$")) {
            throw new IllegalArgumentException("잘못된 청구 번호 형식입니다: " + value);
        }
        return new ClaimNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

```java
package com.insurance.claim.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 금액 Value Object
 */
@Getter
@EqualsAndHashCode
public class Money {

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0보다 작을 수 없습니다");
        }
        this.amount = amount;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) <= 0;
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
```

### 3.3 repository/

**역할**: 리포지토리 인터페이스 정의 (구현체는 infrastructure에)

**책임**:
- 영속성 추상화
- Domain 중심의 조회 메서드 정의

**중요: Repository는 왜 인터페이스가 필요한가?**
- **Service와 달리 Repository는 반드시 인터페이스로 정의합니다!**
- **의존성 역전 원칙 (DIP)** 적용
  - Domain(핵심)이 Infrastructure(구현)에 의존하면 안 됨
  - Domain에서 인터페이스 정의 → Infrastructure에서 구현
- **진짜 여러 구현체가 가능**
  - JPA 구현, MyBatis 구현, Memory 구현, Mock 구현 등
  - 테스트에서 InMemory Repository로 쉽게 교체 가능
- **기술 독립성 유지**
  - Domain 레이어는 JPA, Spring 등의 기술에 의존하지 않음
  - 영속성 기술을 변경해도 Domain은 변경 불필요

**예시 코드**:
```java
package com.insurance.claim.domain.repository;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.model.ClaimStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Claim Repository 인터페이스
 *
 * 구현체는 infrastructure.persistence 패키지에 위치
 */
public interface ClaimRepository {

    Claim save(Claim claim);

    Optional<Claim> findById(Long id);

    List<Claim> findAll();

    List<Claim> findByPolicyNumber(String policyNumber);

    List<Claim> findByStatus(ClaimStatus status);

    List<Claim> findByAccidentDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByPolicyNumber(String policyNumber);

    void delete(Claim claim);
}
```

### 3.4 service/

**역할**: 도메인 서비스 - 여러 엔티티에 걸친 비즈니스 로직

**언제 사용?**:
- 단일 엔티티에 속하지 않는 비즈니스 로직
- 여러 엔티티 간 협력이 필요한 로직

**예시 코드**:
```java
package com.insurance.claim.domain.service;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 청구 도메인 서비스
 *
 * 여러 엔티티에 걸친 비즈니스 규칙을 처리
 */
@Service
@RequiredArgsConstructor
public class ClaimDomainService {

    private final ClaimRepository claimRepository;

    /**
     * 청구 유효성 검증
     */
    public void validateClaim(Claim claim) {
        // 1. 사고 일자는 보험 증권 발행일 이후여야 함
        validateAccidentDate(claim);

        // 2. 중복 청구 확인
        validateDuplicateClaim(claim);

        // 3. 청구 금액 한도 확인
        validateClaimAmountLimit(claim);
    }

    private void validateAccidentDate(Claim claim) {
        LocalDate accidentDate = claim.getAccidentDate();
        long daysSinceAccident = ChronoUnit.DAYS.between(accidentDate, LocalDate.now());

        if (daysSinceAccident > 365) {
            throw new IllegalStateException(
                    "사고 발생일로부터 1년이 경과한 청구는 접수할 수 없습니다");
        }

        if (accidentDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("사고 일자가 미래일 수 없습니다");
        }
    }

    private void validateDuplicateClaim(Claim claim) {
        boolean hasDuplicateClaim = claimRepository
                .findByPolicyNumber(claim.getPolicyNumber())
                .stream()
                .anyMatch(existing ->
                    existing.getAccidentDate().equals(claim.getAccidentDate()));

        if (hasDuplicateClaim) {
            throw new IllegalStateException(
                    "동일한 보험증권과 사고일자로 이미 청구가 존재합니다");
        }
    }

    private void validateClaimAmountLimit(Claim claim) {
        // 실제로는 보험 상품 정보를 조회하여 확인해야 함
        // 여기서는 예시로 간단히 처리
        if (!claim.isApprovable()) {
            throw new IllegalStateException("청구 금액이 승인 가능 한도를 초과했습니다");
        }
    }
}
```

### 3.5 event/

**역할**: 도메인 이벤트 정의

**언제 사용?**:
- 도메인에서 중요한 일이 발생했을 때
- 비동기 처리가 필요한 경우
- 다른 바운디드 컨텍스트에 알림이 필요한 경우

**예시 코드**:
```java
package com.insurance.claim.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 청구 승인 이벤트
 */
@Getter
@Builder
public class ClaimApprovedEvent {

    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private BigDecimal claimAmount;
    private String approverName;
    private LocalDateTime approvedAt;

    public static ClaimApprovedEvent from(Long claimId, String claimNumber,
                                          String policyNumber, BigDecimal claimAmount,
                                          String approverName) {
        return ClaimApprovedEvent.builder()
                .claimId(claimId)
                .claimNumber(claimNumber)
                .policyNumber(policyNumber)
                .claimAmount(claimAmount)
                .approverName(approverName)
                .approvedAt(LocalDateTime.now())
                .build();
    }
}
```

```java
package com.insurance.claim.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 청구 생성 이벤트
 */
@Getter
@Builder
public class ClaimCreatedEvent {

    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private String claimantName;
    private String email;
    private LocalDateTime createdAt;
}
```

---

## 4. Infrastructure 레이어

> 외부 시스템과의 연동을 담당하는 레이어

### 4.1 persistence/

**역할**: 데이터베이스 접근 구현

**책임**:
- JPA Entity 정의
- Repository 구현
- Domain 객체 ↔ JPA Entity 변환

**예시 코드**:
```java
package com.insurance.claim.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Claim JPA Entity
 *
 * Domain 모델과 분리하여 영속성 관심사를 격리
 */
@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_policy_number", columnList = "policy_number"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_accident_date", columnList = "accident_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", nullable = false, length = 50)
    private String policyNumber;

    @Column(name = "claim_number", nullable = false, unique = true, length = 50)
    private String claimNumber;

    @Column(name = "accident_date", nullable = false)
    private LocalDate accidentDate;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "claim_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal claimAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClaimStatusEntity status;

    @Column(name = "claimant_name", nullable = false, length = 100)
    private String claimantName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "approver_name", length = 100)
    private String approverName;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Builder 및 Domain 변환 메서드는 Mapper에서 처리
}
```

```java
package com.insurance.claim.infrastructure.persistence;

public enum ClaimStatusEntity {
    PENDING, APPROVED, REJECTED, PAID, CANCELLED
}
```

```java
package com.insurance.claim.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository
 */
public interface ClaimJpaRepository extends JpaRepository<ClaimEntity, Long> {

    List<ClaimEntity> findByPolicyNumber(String policyNumber);

    List<ClaimEntity> findByStatus(ClaimStatusEntity status);

    List<ClaimEntity> findByAccidentDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByPolicyNumber(String policyNumber);

    @Query("SELECT c FROM ClaimEntity c WHERE c.status = :status " +
           "AND c.claimAmount >= :minAmount")
    List<ClaimEntity> findLargeClaimsByStatus(
            @Param("status") ClaimStatusEntity status,
            @Param("minAmount") BigDecimal minAmount);
}
```

```java
package com.insurance.claim.infrastructure.persistence;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.model.ClaimStatus;
import com.insurance.claim.domain.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ClaimRepository 구현체
 *
 * Domain Repository 인터페이스를 구현하여 영속성을 제공
 */
@Repository
@RequiredArgsConstructor
public class ClaimRepositoryImpl implements ClaimRepository {

    private final ClaimJpaRepository jpaRepository;
    private final ClaimMapper mapper;

    @Override
    public Claim save(Claim claim) {
        ClaimEntity entity = mapper.toEntity(claim);
        ClaimEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Claim> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Claim> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Claim> findByPolicyNumber(String policyNumber) {
        return jpaRepository.findByPolicyNumber(policyNumber).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Claim> findByStatus(ClaimStatus status) {
        ClaimStatusEntity entityStatus = ClaimStatusEntity.valueOf(status.name());
        return jpaRepository.findByStatus(entityStatus).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Claim> findByAccidentDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByAccidentDateBetween(startDate, endDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPolicyNumber(String policyNumber) {
        return jpaRepository.existsByPolicyNumber(policyNumber);
    }

    @Override
    public void delete(Claim claim) {
        jpaRepository.deleteById(claim.getId());
    }
}
```

```java
package com.insurance.claim.infrastructure.persistence;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.model.ClaimStatus;
import com.insurance.claim.domain.vo.ClaimNumber;
import org.springframework.stereotype.Component;

/**
 * Domain ↔ Entity 변환 Mapper
 */
@Component
public class ClaimMapper {

    public ClaimEntity toEntity(Claim domain) {
        ClaimEntity entity = new ClaimEntity();
        // 실제로는 리플렉션 또는 빌더 패턴 사용
        // 여기서는 개념 설명을 위한 의사 코드
        // entity.setId(domain.getId());
        // entity.setPolicyNumber(domain.getPolicyNumber());
        // ... 등등
        return entity;
    }

    public Claim toDomain(ClaimEntity entity) {
        return Claim.builder()
                .policyNumber(entity.getPolicyNumber())
                .accidentDate(entity.getAccidentDate())
                .description(entity.getDescription())
                .claimAmount(entity.getClaimAmount())
                .claimantName(entity.getClaimantName())
                .email(entity.getEmail())
                .build();
        // 실제로는 더 복잡한 변환 로직 필요
    }
}
```

### 4.2 external/

**역할**: 외부 API 클라이언트 구현

**예시 코드**:
```java
package com.insurance.claim.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 보험 정책 조회 외부 API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyApiClient {

    private final RestTemplate restTemplate;

    public PolicyResponse getPolicy(String policyNumber) {
        log.info("보험 정책 조회: policyNumber={}", policyNumber);

        String url = "https://api.insurance.com/policies/" + policyNumber;

        try {
            return restTemplate.getForObject(url, PolicyResponse.class);
        } catch (Exception e) {
            log.error("보험 정책 조회 실패", e);
            throw new ExternalApiException("보험 정책 조회에 실패했습니다", e);
        }
    }
}
```

```java
package com.insurance.claim.infrastructure.external;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PolicyResponse {
    private String policyNumber;
    private String holderName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal coverageAmount;
    private String status;
}
```

### 4.3 messaging/

**역할**: 메시징 시스템 연동 (Kafka, RabbitMQ 등)

**예시 코드**:
```java
package com.insurance.claim.infrastructure.messaging;

import com.insurance.claim.domain.event.ClaimApprovedEvent;
import com.insurance.claim.domain.model.Claim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 청구 이벤트 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimEventPublisher {

    private static final String TOPIC_CLAIM_APPROVED = "claim.approved";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishClaimApproved(Claim claim) {
        ClaimApprovedEvent event = ClaimApprovedEvent.from(
                claim.getId(),
                claim.getClaimNumber().getValue(),
                claim.getPolicyNumber(),
                claim.getClaimAmount().getAmount(),
                claim.getApproverName()
        );

        log.info("청구 승인 이벤트 발행: claimId={}", claim.getId());

        kafkaTemplate.send(TOPIC_CLAIM_APPROVED, event.getClaimId().toString(), event);
    }
}
```

```java
package com.insurance.claim.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 청구 이벤트 리스너
 */
@Slf4j
@Component
public class ClaimEventListener {

    @KafkaListener(topics = "claim.approved", groupId = "claim-service")
    public void handleClaimApproved(String message) {
        log.info("청구 승인 이벤트 수신: {}", message);

        // 이벤트 처리 로직
        // 예: 이메일 발송, 알림 전송 등
    }
}
```

---

## 5. Config 레이어

> 애플리케이션 설정을 담당하는 레이어

### 5.1 security/

**역할**: 보안 설정

**예시 코드**:
```java
package com.insurance.claim.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
```

### 5.2 database/

**역할**: 데이터베이스 설정

**예시 코드**:
```java
package com.insurance.claim.config.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/claim");
        config.setUsername("claim");
        config.setPassword("changeme");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);

        return new HikariDataSource(config);
    }
}
```

### 5.3 properties/

**역할**: 외부 설정 프로퍼티 매핑

**예시 코드**:
```java
package com.insurance.claim.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "claim")
public class ClaimProperties {

    private Approval approval = new Approval();
    private Notification notification = new Notification();

    @Getter
    @Setter
    public static class Approval {
        private int autoApprovalMaxAmount = 1000000;
        private int manualReviewThreshold = 500000;
    }

    @Getter
    @Setter
    public static class Notification {
        private boolean emailEnabled = true;
        private boolean smsEnabled = false;
        private String fromEmail = "noreply@insurance.com";
    }
}
```

```yaml
# application.yml
claim:
  approval:
    auto-approval-max-amount: 1000000
    manual-review-threshold: 500000
  notification:
    email-enabled: true
    sms-enabled: false
    from-email: noreply@insurance.com
```

---

## 의존성 규칙

### 레이어 간 의존성 방향

```
api → application → domain ← infrastructure
                      ↓
                    config
```

### 핵심 원칙

1. **Domain은 다른 레이어를 의존하지 않음**
   - 순수한 비즈니스 로직만 포함
   - 외부 프레임워크 의존성 최소화

2. **Application은 Domain만 의존**
   - Infrastructure는 인터페이스를 통해 추상화

3. **Infrastructure는 Domain의 인터페이스를 구현**
   - 의존성 역전 원칙 (DIP) 적용

4. **API는 Application을 호출**
   - Domain을 직접 호출하지 않음

### 잘못된 의존성 예시

❌ **잘못된 예**:
```java
// Controller에서 Repository 직접 호출
@RestController
public class ClaimController {
    private final ClaimRepository repository; // ❌

    public ClaimResponse getClaim(Long id) {
        return repository.findById(id); // ❌
    }
}
```

✅ **올바른 예**:
```java
// Controller는 Service를 통해서만 접근
@RestController
public class ClaimController {
    private final ClaimService service; // ✅

    public ClaimResponse getClaim(Long id) {
        return service.getClaim(id); // ✅
    }
}
```

---

## 실전 예제

### 완전한 청구 생성 흐름

```
1. ClaimController (api/controller)
   ↓
2. ClaimService (application/service)
   ↓
3. ClaimDomainService (domain/service) - 비즈니스 규칙 검증
   ↓
4. Claim (domain/model) - 엔티티 생성
   ↓
5. ClaimRepository (domain/repository) - 인터페이스
   ↓
6. ClaimRepositoryImpl (infrastructure/persistence) - 구현체
   ↓
7. ClaimJpaRepository (infrastructure/persistence) - JPA
```

### 코드 흐름

**1단계: API 요청 수신**
```java
@PostMapping
public ResponseEntity<ClaimResponse> createClaim(
        @Valid @RequestBody ClaimCreateRequest request) {
    ClaimResponse response = claimService.createClaim(request);
    return ResponseEntity.ok(response);
}
```

**2단계: Application Service 처리**
```java
@Transactional
public ClaimResponse createClaim(ClaimCreateRequest request) {
    // Domain 객체 생성
    Claim claim = Claim.builder()
            .policyNumber(request.getPolicyNumber())
            // ...
            .build();

    // Domain Service로 검증
    claimDomainService.validateClaim(claim);

    // 저장
    Claim savedClaim = claimRepository.save(claim);

    // DTO 변환 후 반환
    return toResponse(savedClaim);
}
```

**3단계: Domain Service 검증**
```java
public void validateClaim(Claim claim) {
    validateAccidentDate(claim);
    validateDuplicateClaim(claim);
    validateClaimAmountLimit(claim);
}
```

**4단계: Repository를 통한 저장**
```java
@Override
public Claim save(Claim claim) {
    ClaimEntity entity = mapper.toEntity(claim);
    ClaimEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
}
```

---

## 정리

### 각 레이어 요약

| 레이어 | 역할 | 핵심 포인트 |
|-------|------|-----------|
| **api** | HTTP 통신 | 요청/응답 처리, 검증 |
| **application** | 유즈케이스 조율 | 트랜잭션, DTO 변환 |
| **domain** | 비즈니스 로직 | 순수 도메인, 외부 의존성 없음 |
| **infrastructure** | 외부 연동 | DB, API, 메시징 |
| **config** | 설정 | Spring 설정, 빈 등록 |

### 패키지 선택 가이드

**처음 시작할 때**:
- `controller`, `dto/request`, `dto/response` ✅
- `service` ✅
- `model`, `repository` ✅
- `persistence` ✅

**필요할 때 추가**:
- `exception` - 예외가 많아질 때
- `usecase` - 서비스가 복잡해질 때
- `vo` - Value Object가 많을 때
- `event`, `messaging` - 이벤트 기반 처리 필요 시
- `external` - 외부 API 연동 시

---

## 참고 자료

- Domain-Driven Design (Eric Evans)
- Clean Architecture (Robert C. Martin)
- Hexagonal Architecture (Alistair Cockburn)