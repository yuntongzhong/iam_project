package com.example.iam.domain;

import com.example.iam.domain.enums.AuditStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AuditLog extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 200)
    private String action;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditStatus status;

    @Column(columnDefinition = "TEXT")
    private String details;
}
