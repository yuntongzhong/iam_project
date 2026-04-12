package com.example.iam.web;

import com.example.iam.service.AuditLogService;
import com.example.iam.web.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final ApiMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_READ') or hasRole('ADMIN')")
    public List<AuditLogResponse> list() {
        return auditLogService.findRecent().stream().map(apiMapper::toAuditLogResponse).toList();
    }
}
