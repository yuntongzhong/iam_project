package com.example.iam.web;

import com.example.iam.security.AuditLog;
import com.example.iam.service.RoleService;
import com.example.iam.web.dto.RoleRequest;
import com.example.iam.web.dto.RoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final ApiMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ROLE_READ') or hasRole('ADMIN')")
    public List<RoleResponse> list() {
        return roleService.findAll().stream().map(apiMapper::toRoleResponse).toList();
    }

    @PostMapping
    @AuditLog(action = "创建角色")
    @PreAuthorize("hasAuthority('PERM_ROLE_WRITE') or hasRole('ADMIN')")
    public RoleResponse create(@Valid @RequestBody RoleRequest request) {
        return apiMapper.toRoleResponse(roleService.create(request));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新角色")
    @PreAuthorize("hasAuthority('PERM_ROLE_WRITE') or hasRole('ADMIN')")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return apiMapper.toRoleResponse(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "删除角色")
    @PreAuthorize("hasAuthority('PERM_ROLE_WRITE') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
