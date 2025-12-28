package com.insurance.claim.domain.repository;

import com.insurance.claim.domain.model.Claim;
import com.insurance.claim.domain.vo.ClaimNumber;
import java.util.Optional;

public interface ClaimRepository {

    Claim save(Claim claim);

    Optional<Claim> findById(Long id);

    Optional<Claim> findByClaimNumber(ClaimNumber claimNumber);
}
