package com.example.iam.repository;

import com.example.iam.domain.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Override
    @EntityGraph(attributePaths = {"parent", "roles", "children"})
    List<Department> findAll();
}
