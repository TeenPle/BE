package com.shu.backend.domain.adminaudit.repository;

import com.shu.backend.domain.adminaudit.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long>, JpaSpecificationExecutor<AdminAuditLog> {

    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
