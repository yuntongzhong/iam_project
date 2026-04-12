package com.example.iam.web;

import com.example.iam.repository.PermissionRepository;
import com.example.iam.web.dto.PermissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;
    private final ApiMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ROLE_READ') or hasRole('ADMIN')")
    public List<PermissionResponse> list() {
        return permissionRepository.findAll().stream().map(apiMapper::toPermissionResponse).toList();
    }
}
