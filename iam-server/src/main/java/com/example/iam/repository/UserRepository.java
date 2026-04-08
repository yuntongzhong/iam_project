package com.example.iam.repository;

import com.example.iam.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {
            "roles",
            "roles.permissions",
            "department",
            "department.roles",
            "department.roles.permissions",
            "department.parent",
            "department.parent.roles",
            "department.parent.roles.permissions"
    })
    Optional<User> findByUsername(String username);
}
