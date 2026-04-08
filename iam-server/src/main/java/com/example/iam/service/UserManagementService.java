package com.example.iam.service;

import com.example.iam.domain.Role;
import com.example.iam.domain.User;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.DepartmentRepository;
import com.example.iam.repository.RoleRepository;
import com.example.iam.repository.UserRepository;
import com.example.iam.web.dto.UserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialPolicy credentialPolicy;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public User create(UserRequest request) {
        User user = new User();
        apply(user, request, true);
        return userRepository.save(user);
    }

    public User update(Long id, UserRequest request) {
        User user = findById(id);
        apply(user, request, false);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public User resetPassword(Long id, String newPassword) {
        User user = findById(id);
        credentialPolicy.validatePassword("newPassword", newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    public User updateStatus(Long id, UserStatus status) {
        User user = findById(id);
        user.setStatus(status);
        return userRepository.save(user);
    }

    public User assignRoles(Long id, Set<Long> roleIds) {
        User user = findById(id);
        user.setRoles(roleIds == null || roleIds.isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(roleRepository.findByIdIn(roleIds)));
        return userRepository.save(user);
    }

    private void apply(User user, UserRequest request, boolean creating) {
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
        if (creating && (request.password() == null || request.password().isBlank())) {
            throw new IllegalArgumentException("password is required when creating a user");
        }
        if (creating || (request.password() != null && !request.password().isBlank())) {
            credentialPolicy.validatePassword("password", request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setDepartment(request.departmentId() == null ? null : departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.departmentId())));
        Set<Role> roles = request.roleIds() == null || request.roleIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(roleRepository.findByIdIn(request.roleIds()));
        user.setRoles(roles);
    }
}
