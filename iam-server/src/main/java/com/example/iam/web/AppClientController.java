package com.example.iam.web;

import com.example.iam.security.AuditLog;
import com.example.iam.service.AppClientService;
import com.example.iam.web.dto.AppClientRequest;
import com.example.iam.web.dto.AppClientResponse;
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
@RequestMapping("/api/apps")
@RequiredArgsConstructor
public class AppClientController {

    private final AppClientService appClientService;
    private final ApiMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_APP_READ') or hasRole('ADMIN')")
    public List<AppClientResponse> list() {
        return appClientService.findAll().stream().map(apiMapper::toAppClientResponse).toList();
    }

    @PostMapping
    @AuditLog(action = "创建 OAuth2 客户端")
    @PreAuthorize("hasAuthority('PERM_APP_WRITE') or hasRole('ADMIN')")
    public AppClientResponse create(@Valid @RequestBody AppClientRequest request) {
        return apiMapper.toAppClientResponse(appClientService.create(request));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新 OAuth2 客户端")
    @PreAuthorize("hasAuthority('PERM_APP_WRITE') or hasRole('ADMIN')")
    public AppClientResponse update(@PathVariable Long id, @Valid @RequestBody AppClientRequest request) {
        return apiMapper.toAppClientResponse(appClientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "删除 OAuth2 客户端")
    @PreAuthorize("hasAuthority('PERM_APP_WRITE') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        appClientService.delete(id);
    }
}
