package com.finora.backend.repository;

import com.finora.backend.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByUserId(String userId);
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByIdAndUser_Id(String id, String userId);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance + :delta WHERE a.id = :accountId")
    int adjustBalance(@Param("accountId") String accountId, @Param("delta") BigDecimal delta);
}