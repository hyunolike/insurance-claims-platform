package com.insurance.claim.api.controller;

import com.insurance.claim.api.dto.request.ClaimCreateRequest;
import com.insurance.claim.api.dto.response.ClaimResponse;
import com.insurance.claim.application.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(
            @Valid @RequestBody ClaimCreateRequest request
    ) {
        ClaimResponse response = claimService.createClaim(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable Long claimId) {
        ClaimResponse response = claimService.getClaim(claimId);
        return ResponseEntity.ok(response);
    }

    private ClaimService.CreateClaimCommand toCommand(ClaimCreateRequest request) {
        return new ClaimService.CreateClaimCommand(
                request.getPolicyNumber(),
                request.getAccidentDate(),
                request.getDescription(),
                request.getClaimAmount(),
                request.getClaimantName(),
                request.getEmail()
        );
    }
}
