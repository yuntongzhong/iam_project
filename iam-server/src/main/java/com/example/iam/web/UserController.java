package com.example.iam.web;

import com.example.iam.domain.enums.UserStatus;
import com.example.iam.security.AuditLog;
import com.example.iam.service.UserImportService;
import com.example.iam.service.UserManagementService;
import com.example.iam.web.dto.PasswordResetRequest;
import com.example.iam.web.dto.UserImportResponse;
import com.example.iam.web.dto.UserRequest;
import com.example.iam.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final UserImportService userImportService;
    private final ApiMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_READ') or hasRole('ADMIN')")
    public List<UserResponse> list() {
        return userManagementService.findAll().stream().map(apiMapper::toUserResponse).toList();
    }

    @PostMapping
    @AuditLog(action = "创建用户")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return apiMapper.toUserResponse(userManagementService.create(request));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新用户")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return apiMapper.toUserResponse(userManagementService.update(id, request));
    }

    @PatchMapping("/{id}/password")
    @AuditLog(action = "重置用户密码")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        return apiMapper.toUserResponse(userManagementService.resetPassword(id, request.newPassword()));
    }

    @PatchMapping("/{id}/status")
    @AuditLog(action = "更新用户状态")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse updateStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        return apiMapper.toUserResponse(userManagementService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/roles")
    @AuditLog(action = "分配用户角色")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserResponse assignRoles(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        return apiMapper.toUserResponse(userManagementService.assignRoles(id, roleIds));
    }

    @GetMapping(value = "/import/template", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<String> downloadImportTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"iam-user-import-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(userImportService.template());
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditLog(action = "预检用户导入")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public UserImportResponse previewImport(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return userImportService.preview(file);
    }

    @PostMapping(value = "/import/commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditLog(action = "批量导入用户")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<UserImportResponse> commitImport(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UserImportResponse response = userImportService.commit(file);
        if (response.summary().errorRows() > 0) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "删除用户")
    @PreAuthorize("hasAuthority('PERM_USER_WRITE') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userManagementService.delete(id);
    }
}
