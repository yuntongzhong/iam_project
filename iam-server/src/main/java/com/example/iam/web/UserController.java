package com.example.iam.web;

import com.example.iam.domain.enums.UserStatus;
import com.example.iam.security.AuditLog;
import com.example.iam.service.UserManagementService;
import com.example.iam.web.dto.PasswordResetRequest;
import com.example.iam.web.dto.UserRequest;
import com.example.iam.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final ApiMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_READ') or hasRole('ADMIN')")
    public List<UserResponse> list() {
        return userManagementService.findAll().stream().map(apiMapper::toUserResponse).toList();
    }

    @PostMapping
    @AuditLog(action = "创建用户")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return apiMapper.toUserResponse(userManagementService.create(request));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新用户")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return apiMapper.toUserResponse(userManagementService.update(id, request));
    }

    @PatchMapping("/{id}/password")
    @AuditLog(action = "重置用户密码")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        return apiMapper.toUserResponse(userManagementService.resetPassword(id, request.newPassword()));
    }

    @PatchMapping("/{id}/status")
    @AuditLog(action = "更新用户状态")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse updateStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        return apiMapper.toUserResponse(userManagementService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/roles")
    @AuditLog(action = "分配用户角色")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse assignRoles(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        return apiMapper.toUserResponse(userManagementService.assignRoles(id, roleIds));
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "删除用户")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userManagementService.delete(id);
    }
}
