package com.example.iam.service;

import com.example.iam.domain.Department;
import com.example.iam.domain.Role;
import com.example.iam.domain.User;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.DepartmentRepository;
import com.example.iam.repository.RoleRepository;
import com.example.iam.repository.UserRepository;
import com.example.iam.web.dto.UserImportResponse;
import com.example.iam.web.dto.UserRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserImportServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final UserManagementService userManagementService = mock(UserManagementService.class);
    private UserImportService userImportService;

    private Department headOffice;
    private Department engineering;
    private Role engineer;

    @BeforeEach
    void setUp() {
        userImportService = new UserImportService(
                userRepository,
                roleRepository,
                departmentRepository,
                userManagementService,
                new CredentialPolicy(),
                Validation.buildDefaultValidatorFactory().getValidator()
        );

        headOffice = Department.builder().name("总部").build();
        headOffice.setId(1L);
        engineering = Department.builder().name("研发中心").parent(headOffice).build();
        engineering.setId(2L);
        headOffice.setChildren(new LinkedHashSet<>(Set.of(engineering)));

        engineer = Role.builder().code("ENGINEER").name("研发工程师").build();
        engineer.setId(11L);

        when(userRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findAll()).thenReturn(List.of(engineer));
        when(departmentRepository.findAll()).thenReturn(List.of(headOffice, engineering));
    }

    @Test
    void previewShouldAcceptUtf8DepartmentPathAndQuotedRoles() {
        MockMultipartFile file = csv("""
                username,email,phone,departmentPath,roleCodes,status,password
                alice.csv,alice.csv@example.com,13800001111,"总部/研发中心","ENGINEER",ACTIVE,Alice#2026!Secure
                """);

        UserImportResponse response = userImportService.preview(file);

        assertEquals(1, response.summary().totalRows());
        assertEquals(1, response.summary().successRows());
        assertEquals(0, response.summary().errorRows());
        assertFalse(response.summary().committed());
    }

    @Test
    void previewShouldReportMissingDepartmentUnknownRoleIllegalStatusAndMissingPassword() {
        MockMultipartFile file = csv("""
                username,email,phone,departmentPath,roleCodes,status,password
                bob.csv,bob.csv@example.com,13800002222,"总部/不存在","UNKNOWN",LOCKED,
                """);

        UserImportResponse response = userImportService.preview(file);

        assertEquals(1, response.summary().errorRows());
        assertTrue(response.rows().get(0).errors().stream().anyMatch(message -> message.contains("部门路径不存在")));
        assertTrue(response.rows().get(0).errors().stream().anyMatch(message -> message.contains("未知角色编码")));
        assertTrue(response.rows().get(0).errors().stream().anyMatch(message -> message.contains("导入状态仅允许 ACTIVE 或 DISABLED")));
        assertTrue(response.rows().get(0).errors().stream().anyMatch(message -> message.contains("新建用户必须提供 password")));
    }

    @Test
    void previewShouldWarnWhenUpdatingExistingUserWithoutPassword() {
        User existingUser = User.builder()
                .username("alice")
                .email("alice@example.com")
                .phone("13800005678")
                .status(UserStatus.ACTIVE)
                .build();
        existingUser.setId(21L);
        when(userRepository.findAll()).thenReturn(List.of(existingUser));

        MockMultipartFile file = csv("""
                username,email,phone,departmentPath,roleCodes,status,password
                alice,alice@example.com,13800005678,"总部/研发中心","ENGINEER",ACTIVE,
                """);

        UserImportResponse response = userImportService.preview(file);

        assertEquals(1, response.summary().warningRows());
        assertEquals(0, response.summary().errorRows());
        assertTrue(response.rows().get(0).warnings().stream().anyMatch(message -> message.contains("保留该用户原有密码")));
    }

    @Test
    void commitShouldCallCreateForValidRowsOnly() {
        MockMultipartFile file = csv("""
                username,email,phone,departmentPath,roleCodes,status,password
                alice.csv,alice.csv@example.com,13800001111,"总部/研发中心","ENGINEER",ACTIVE,Alice#2026!Secure
                """);

        UserImportResponse response = userImportService.commit(file);

        assertTrue(response.summary().committed());
        ArgumentCaptor<UserRequest> requestCaptor = ArgumentCaptor.forClass(UserRequest.class);
        verify(userManagementService).create(requestCaptor.capture());
        assertEquals("alice.csv", requestCaptor.getValue().username());
        assertEquals(2L, requestCaptor.getValue().departmentId());
        assertEquals(Set.of(11L), requestCaptor.getValue().roleIds());
    }

    @Test
    void commitShouldNotPersistWhenPreviewHasErrors() {
        MockMultipartFile file = csv("""
                username,email,phone,departmentPath,roleCodes,status,password
                invalid,invalid@example.com,13800003333,"总部/不存在","ENGINEER",ACTIVE,Invalid#2026!Secure
                """);

        UserImportResponse response = userImportService.commit(file);

        assertEquals(1, response.summary().errorRows());
        verify(userManagementService, never()).create(any());
        verify(userManagementService, never()).update(any(), any());
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "users.csv", "text/csv", content.getBytes());
    }
}
