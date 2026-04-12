package com.example.iam.repository;

import com.example.iam.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findAll();

    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findByIdIn(Collection<Long> ids);
}
