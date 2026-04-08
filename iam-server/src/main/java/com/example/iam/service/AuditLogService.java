package com.example.iam.service;

import com.example.iam.domain.AuditLog;
import com.example.iam.domain.enums.AuditStatus;
import com.example.iam.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog save(String username, String action, String ipAddress, AuditStatus status, String details) {
        AuditLog log = AuditLog.builder()
                .username(username == null || username.isBlank() ? "anonymous" : username)
                .action(action)
                .ipAddress(ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress)
                .timestamp(LocalDateTime.now())
                .status(status)
                .details(details)
                .build();
        return auditLogRepository.save(log);
    }

    public List<AuditLog> findRecent() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }
}
