package com.finora.backend.repository;

import com.finora.backend.domain.RecurringTransfer;
import com.finora.backend.domain.RecurringTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransferRepository extends JpaRepository<RecurringTransfer, String> {
    List<RecurringTransfer> findByNextRunDateLessThanEqualAndStatus(LocalDate date, RecurringTransferStatus status);
    List<RecurringTransfer> findByFromAccountId(String accountId);
}