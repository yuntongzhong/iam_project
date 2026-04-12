package com.example.iam.service;

import com.example.iam.domain.Department;
import com.example.iam.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentRoleResolverServiceTest {

    private final DepartmentRoleResolverService service = new DepartmentRoleResolverService();

    @Test
    void shouldMergeParentRoles() {
        Role admin = Role.builder().code("ADMIN").name("管理员").build();
        Role engineer = Role.builder().code("ENGINEER").name("工程师").build();

        Department parent = Department.builder().name("总部").roles(new LinkedHashSet<>(Set.of(admin))).build();
        Department child = Department.builder().name("研发中心").parent(parent).roles(new LinkedHashSet<>(Set.of(engineer))).build();

        Set<Role> result = service.resolveInheritedRoles(child);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(role -> "ADMIN".equals(role.getCode())));
        assertTrue(result.stream().anyMatch(role -> "ENGINEER".equals(role.getCode())));
    }
}
