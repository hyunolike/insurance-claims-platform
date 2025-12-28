package com.insurance.claim.config.logging;

import com.insurance.claim.api.dto.request.ClaimCreateRequest;
import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.service.ClaimService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class RequestResponseLoggingAspect {

    @Around("within(com.insurance.claim.api.controller..*)")
    public Object logControllerFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "API");
    }

    @Around("within(com.insurance.claim.application.service..*)")
    public Object logServiceFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "SERVICE");
    }

    private Object logExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String signature = joinPoint.getSignature().toShortString();
        log.info("[{}] Enter {} args={}", layer, signature, summarizeArguments(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            log.info("[{}] Exit {} result={} elapsedMs={}", layer, signature,
                    summarizeResult(result), stopWatch.getTotalTimeMillis());
            return result;
        } catch (Exception ex) {
            stopWatch.stop();
            log.error("[{}] Exception {} message={} elapsedMs={}", layer, signature,
                    ex.getMessage(), stopWatch.getTotalTimeMillis(), ex);
            throw ex;
        }
    }

    private String summarizeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(summarizeValue(arg));
        }
        return builder.toString();
    }

    private String summarizeValue(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof ClaimCreateRequest request) {
            return "ClaimCreateRequest(policyNumber=%s, claimAmount=%s, claimantName=%s, email=%s)"
                    .formatted(
                            request.getPolicyNumber(),
                            request.getClaimAmount(),
                            maskEmail(request.getEmail()),
                            request.getClaimantName()
                    );
        }
        if (arg instanceof ClaimService.CreateClaimCommand command) {
            return "CreateClaimCommand(policyNumber=%s, amount=%s, claimant=%s, email=%s)"
                    .formatted(
                            command.policyNumber(),
                            command.claimAmount(),
                            command.claimantName(),
                            maskEmail(command.email())
                    );
        }
        return arg.getClass().getSimpleName();
    }

    private String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        if (result instanceof ClaimResponse response) {
            return "ClaimResponse(id=%d, claimNumber=%s, status=%s)"
                    .formatted(
                            response.getClaimId(),
                            response.getClaimNumber(),
                            response.getStatus()
                    );
        }
        return result.getClass().getSimpleName();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "N/A";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
