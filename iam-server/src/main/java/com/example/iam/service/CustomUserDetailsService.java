package com.example.iam.service;

import com.example.iam.domain.Permission;
import com.example.iam.domain.Role;
import com.example.iam.domain.User;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DepartmentRoleResolverService departmentRoleResolverService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new LockedException("User account is locked");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new DisabledException("User account is disabled");
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Set<Role> allRoles = new LinkedHashSet<>(user.getRoles());
        if (user.getDepartment() != null) {
            allRoles.addAll(departmentRoleResolverService.resolveInheritedRoles(user.getDepartment()));
        }

        for (Role role : allRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getCode()));
            }
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(user.getStatus() == UserStatus.DISABLED)
                .accountLocked(user.getStatus() == UserStatus.LOCKED)
                .authorities(authorities)
                .build();
    }
}
