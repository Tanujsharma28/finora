package com.finora.backend.repository;

import com.finora.backend.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, String> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}