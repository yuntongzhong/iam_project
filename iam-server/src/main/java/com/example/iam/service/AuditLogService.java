package com.example.iam.service;

import com.example.iam.domain.AuditLog;
import com.example.iam.domain.enums.AuditStatus;
import com.example.iam.repository.AuditLogRepository;
import com.example.iam.security.AuditTarget;
import com.example.iam.security.AuditTargetResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditTargetResolver auditTargetResolver;

    public AuditLog save(String username, String action, String ipAddress, AuditStatus status, String details) {
        return save(username, action, ipAddress, null, AuditTarget.none(), status, details);
    }

    public AuditLog save(String username, String action, HttpServletRequest request, AuditStatus status, String details) {
        AuditTarget target = auditTargetResolver.resolve(request);
        return save(username, action, resolveIpAddress(request), resolveUserAgent(request), target, status, details);
    }

    public AuditLog save(String username, String action, String ipAddress, String userAgent, AuditTarget target, AuditStatus status, String details) {
        AuditLog log = AuditLog.builder()
                .username(username == null || username.isBlank() ? "anonymous" : username)
                .action(action)
                .ipAddress(ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress)
                .targetType(target.targetType())
                .targetId(target.targetId())
                .targetName(target.targetName())
                .userAgent(truncate(userAgent, 500))
                .timestamp(LocalDateTime.now())
                .status(status)
                .details(details)
                .build();
        return auditLogRepository.save(log);
    }

    public List<AuditLog> findRecent() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    private String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return request.getRemoteAddr() == null || request.getRemoteAddr().isBlank() ? "unknown" : request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
