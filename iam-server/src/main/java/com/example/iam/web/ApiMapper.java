package com.example.iam.web;

import com.example.iam.domain.AppClient;
import com.example.iam.domain.AuditLog;
import com.example.iam.domain.Department;
import com.example.iam.domain.Permission;
import com.example.iam.domain.Role;
import com.example.iam.domain.User;
import com.example.iam.service.DepartmentRoleResolverService;
import com.example.iam.web.dto.AppClientResponse;
import com.example.iam.web.dto.AuditLogResponse;
import com.example.iam.web.dto.DepartmentResponse;
import com.example.iam.web.dto.PermissionResponse;
import com.example.iam.web.dto.RoleResponse;
import com.example.iam.web.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class ApiMapper {

    private final DepartmentRoleResolverService departmentRoleResolverService;

    public ApiMapper(DepartmentRoleResolverService departmentRoleResolverService) {
        this.departmentRoleResolverService = departmentRoleResolverService;
    }

    public UserResponse toUserResponse(User user) {
        Set<String> roleCodes = new TreeSet<>();
        Set<String> permissionCodes = new TreeSet<>();
        user.getRoles().forEach(role -> {
            roleCodes.add(role.getCode());
            role.getPermissions().forEach(permission -> permissionCodes.add(permission.getCode()));
        });
        if (user.getDepartment() != null) {
            departmentRoleResolverService.resolveInheritedRoles(user.getDepartment()).forEach(role -> {
                roleCodes.add(role.getCode());
                role.getPermissions().forEach(permission -> permissionCodes.add(permission.getCode()));
            });
        }
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                maskPhone(user.getPhone()),
                user.getStatus(),
                user.getFailedLoginAttempts(),
                user.getTotpEnabled(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null,
                roleCodes,
                permissionCodes
        );
    }

    public DepartmentResponse toDepartmentResponse(Department department) {
        List<DepartmentResponse> children = department.getChildren().stream()
                .sorted(Comparator.comparing(Department::getName))
                .map(this::toDepartmentResponse)
                .toList();
        Set<String> roleCodes = department.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toCollection(TreeSet::new));
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getParent() != null ? department.getParent().getId() : null,
                roleCodes,
                children
        );
    }

    public RoleResponse toRoleResponse(Role role) {
        Set<PermissionResponse> permissions = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PermissionResponse::code))));
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(), permissions);
    }

    public PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.getPermissionType(),
                permission.getResource(),
                permission.getAppClient() != null ? permission.getAppClient().getId() : null
        );
    }

    public AppClientResponse toAppClientResponse(AppClient appClient) {
        return new AppClientResponse(
                appClient.getId(),
                appClient.getClientId(),
                appClient.getClientName(),
                appClient.getRedirectUris(),
                appClient.getPostLogoutRedirectUris(),
                appClient.getScopes(),
                appClient.getGrantTypes(),
                appClient.getAuthenticationMethods(),
                appClient.getRequireProofKey(),
                appClient.getRequireAuthorizationConsent(),
                appClient.getActive()
        );
    }

    public AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUsername(),
                auditLog.getAction(),
                auditLog.getIpAddress(),
                auditLog.getTimestamp(),
                auditLog.getStatus(),
                auditLog.getDetails()
        );
    }

    private String maskPhone(String phone) {
        return phone == null ? null : phone.replaceAll("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)", "$1****$2");
    }
}
