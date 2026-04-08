package com.example.iam.service;

import com.example.iam.domain.Department;
import com.example.iam.domain.Role;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class DepartmentRoleResolverService {

    public Set<Role> resolveInheritedRoles(Department department) {
        Set<Role> roles = new LinkedHashSet<>();
        Department current = department;
        while (current != null) {
            roles.addAll(current.getRoles());
            current = current.getParent();
        }
        return roles;
    }
}
