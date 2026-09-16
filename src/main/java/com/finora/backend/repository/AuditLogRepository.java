package com.finora.backend.repository;

import com.finora.backend.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}