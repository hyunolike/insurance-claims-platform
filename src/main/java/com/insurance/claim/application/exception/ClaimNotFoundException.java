package com.insurance.claim.application.exception;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(Long id) {
        super("청구 정보를 찾을 수 없습니다. id=" + id);
    }
}
