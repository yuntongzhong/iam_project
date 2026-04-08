package com.example.iam.service;

import com.example.iam.domain.Permission;
import com.example.iam.domain.Role;
import com.example.iam.repository.PermissionRepository;
import com.example.iam.repository.RoleRepository;
import com.example.iam.web.dto.RoleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
    }

    public Role create(RoleRequest request) {
        Role role = new Role();
        apply(role, request);
        return roleRepository.save(role);
    }

    public Role update(Long id, RoleRequest request) {
        Role role = findById(id);
        apply(role, request);
        return roleRepository.save(role);
    }

    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

    private void apply(Role role, RoleRequest request) {
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        Set<Permission> permissions = request.permissionIds() == null || request.permissionIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(permissionRepository.findByIdIn(request.permissionIds()));
        role.setPermissions(permissions);
    }
}
