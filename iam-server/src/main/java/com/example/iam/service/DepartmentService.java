package com.example.iam.service;

import com.example.iam.domain.Department;
import com.example.iam.domain.Role;
import com.example.iam.repository.DepartmentRepository;
import com.example.iam.repository.RoleRepository;
import com.example.iam.web.dto.DepartmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    public Department create(DepartmentRequest request) {
        Department department = new Department();
        apply(department, request);
        return departmentRepository.save(department);
    }

    public Department update(Long id, DepartmentRequest request) {
        Department department = findById(id);
        apply(department, request);
        return departmentRepository.save(department);
    }

    public void delete(Long id) {
        departmentRepository.deleteById(id);
    }

    private void apply(Department department, DepartmentRequest request) {
        department.setName(request.name());
        department.setParent(request.parentId() == null ? null : findById(request.parentId()));
        Set<Role> roles = request.roleIds() == null || request.roleIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(roleRepository.findByIdIn(request.roleIds()));
        department.setRoles(roles);
    }
}
