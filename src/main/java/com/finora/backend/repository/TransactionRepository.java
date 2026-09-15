package com.finora.backend.repository;

import com.finora.backend.domain.Transaction;
import com.finora.backend.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

   @Query("SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t " +
       "WHERE t.account.id = :accountId AND t.status = 'COMPLETED'")
BigDecimal findAverageAmountByAccountId(@Param("accountId") String accountId);
}