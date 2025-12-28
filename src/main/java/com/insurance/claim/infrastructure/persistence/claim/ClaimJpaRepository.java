package com.insurance.claim.infrastructure.persistence.claim;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimJpaRepository extends JpaRepository<ClaimEntity, Long> {

    Optional<ClaimEntity> findByClaimNumber(String claimNumber);
}
