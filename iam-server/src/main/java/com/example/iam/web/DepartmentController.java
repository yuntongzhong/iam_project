package com.example.iam.web;

import com.example.iam.security.AuditLog;
import com.example.iam.service.DepartmentService;
import com.example.iam.web.dto.DepartmentRequest;
import com.example.iam.web.dto.DepartmentResponse;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final ApiMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_READ') or hasRole('ADMIN')")
    public List<DepartmentResponse> list() {
        return departmentService.findAll().stream()
                .filter(department -> department.getParent() == null)
                .map(apiMapper::toDepartmentResponse)
                .toList();
    }

    @PostMapping
    @AuditLog(action = "创建部门")
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_WRITE') or hasRole('ADMIN')")
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        return apiMapper.toDepartmentResponse(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新部门")
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_WRITE') or hasRole('ADMIN')")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return apiMapper.toDepartmentResponse(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "删除部门")
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_WRITE') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
