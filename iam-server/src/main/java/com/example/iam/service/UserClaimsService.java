package com.example.iam.service;

import com.example.iam.domain.User;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserClaimsService {

    private final UserRepository userRepository;
    private final DepartmentRoleResolverService departmentRoleResolverService;

    @Transactional(readOnly = true)
    public UserClaimSnapshot buildSnapshot(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return UserClaimSnapshot.empty();
        }

        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        user.getRoles().forEach(role -> {
            roles.add(role.getCode());
            role.getPermissions().forEach(permission -> permissions.add(permission.getCode()));
        });

        if (user.getDepartment() != null) {
            departmentRoleResolverService.resolveInheritedRoles(user.getDepartment()).forEach(role -> {
                roles.add(role.getCode());
                role.getPermissions().forEach(permission -> permissions.add(permission.getCode()));
            });
        }

        return new UserClaimSnapshot(roles, permissions);
    }

    public record UserClaimSnapshot(Set<String> roles, Set<String> permissions) {
        public static UserClaimSnapshot empty() {
            return new UserClaimSnapshot(Set.of(), Set.of());
        }
    }
}
