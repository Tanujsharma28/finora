package com.finora.backend.repository;

import com.finora.backend.domain.FraudFlag;
import com.finora.backend.domain.FraudFlagStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudFlagRepository extends JpaRepository<FraudFlag, String> {
    Optional<FraudFlag> findByTransactionId(String transactionId);
    List<FraudFlag> findByStatus(FraudFlagStatus status);
}