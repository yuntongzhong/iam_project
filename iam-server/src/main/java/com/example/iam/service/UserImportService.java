package com.example.iam.service;

import com.example.iam.domain.Department;
import com.example.iam.domain.Role;
import com.example.iam.domain.User;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.DepartmentRepository;
import com.example.iam.repository.RoleRepository;
import com.example.iam.repository.UserRepository;
import com.example.iam.web.dto.UserImportResponse;
import com.example.iam.web.dto.UserImportRowResponse;
import com.example.iam.web.dto.UserImportSummaryResponse;
import com.example.iam.web.dto.UserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserImportService {

    private static final List<String> TEMPLATE_HEADERS = List.of(
            "username",
            "email",
            "phone",
            "departmentPath",
            "roleCodes",
            "status",
            "password"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserManagementService userManagementService;
    private final CredentialPolicy credentialPolicy;
    private final Validator validator;

    public String template() {
        return "\uFEFF" + String.join(",", TEMPLATE_HEADERS) + "\n"
                + "alice.csv,alice.csv@example.com,13800001111,\"总部/研发中心\",\"ENGINEER\",ACTIVE,Alice#2026!Secure\n"
                + "bob.audit,bob.audit@example.com,13800002222,\"总部/销售中心\",\"AUDITOR\",DISABLED,Bob#2026!Audit\n";
    }

    public UserImportResponse preview(MultipartFile file) {
        return analyze(file).response(false);
    }

    @Transactional
    public UserImportResponse commit(MultipartFile file) {
        Analysis analysis = analyze(file);
        UserImportResponse preview = analysis.response(false);
        if (preview.summary().errorRows() > 0) {
            return preview;
        }
        for (ResolvedRow row : analysis.rows()) {
            if (row.existingUser() == null) {
                userManagementService.create(row.request());
            } else {
                userManagementService.update(row.existingUser().getId(), row.request());
            }
        }
        return analysis.response(true);
    }

    private Analysis analyze(MultipartFile file) {
        String csvContent = readContent(file);
        if (csvContent.isBlank()) {
            return new Analysis(List.of(), invalidFileResponse("上传文件为空，无法执行导入预检。"));
        }

        CSVParser parser = parse(csvContent);
        List<String> headers = sanitizeHeaders(parser.getHeaderNames());
        if (!TEMPLATE_HEADERS.equals(headers)) {
            return new Analysis(List.of(), invalidFileResponse("CSV 表头不匹配，请使用系统模板。期望表头："
                    + String.join(",", TEMPLATE_HEADERS)));
        }

        List<User> existingUsers = userRepository.findAll();
        Map<String, User> usersByUsername = existingUsers.stream()
                .collect(Collectors.toMap(user -> normalizeKey(user.getUsername()), user -> user, (left, right) -> left, LinkedHashMap::new));
        Map<String, User> usersByEmail = existingUsers.stream()
                .collect(Collectors.toMap(user -> normalizeKey(user.getEmail()), user -> user, (left, right) -> left, LinkedHashMap::new));
        Map<String, User> usersByPhone = existingUsers.stream()
                .collect(Collectors.toMap(user -> normalizeKey(user.getPhone()), user -> user, (left, right) -> left, LinkedHashMap::new));
        Map<String, Role> rolesByCode = roleRepository.findAll().stream()
                .collect(Collectors.toMap(role -> normalizeKey(role.getCode()), role -> role, (left, right) -> left, LinkedHashMap::new));
        List<Department> departments = departmentRepository.findAll();

        Set<String> fileUsernames = new LinkedHashSet<>();
        Map<String, Integer> fileEmails = new LinkedHashMap<>();
        Map<String, Integer> filePhones = new LinkedHashMap<>();
        List<ResolvedRow> rows = new ArrayList<>();

        for (CSVRecord record : parser.getRecords()) {
            if (isBlankRow(record)) {
                continue;
            }
            rows.add(resolveRow(record, usersByUsername, usersByEmail, usersByPhone, rolesByCode, departments, fileUsernames, fileEmails, filePhones));
        }

        return new Analysis(rows, null);
    }

    private ResolvedRow resolveRow(CSVRecord record,
                                   Map<String, User> usersByUsername,
                                   Map<String, User> usersByEmail,
                                   Map<String, User> usersByPhone,
                                   Map<String, Role> rolesByCode,
                                   List<Department> departments,
                                   Set<String> fileUsernames,
                                   Map<String, Integer> fileEmails,
                                   Map<String, Integer> filePhones) {
        int rowNumber = Math.toIntExact(record.getRecordNumber()) + 1;
        String username = value(record, "username");
        String email = value(record, "email");
        String phone = value(record, "phone");
        String departmentPath = value(record, "departmentPath");
        String roleCodesText = value(record, "roleCodes");
        String statusText = value(record, "status");
        String password = value(record, "password");

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        String usernameKey = normalizeKey(username);
        User existingUser = usernameKey == null ? null : usersByUsername.get(usernameKey);
        String operation = existingUser == null ? "CREATE" : "UPDATE";

        if (usernameKey != null && !fileUsernames.add(usernameKey)) {
            errors.add("同一个 CSV 中不能重复出现相同 username：" + username);
        }

        UserStatus status = parseStatus(statusText, errors);
        Department department = resolveDepartment(departmentPath, departments, errors);
        Set<Role> roles = resolveRoles(roleCodesText, rolesByCode, errors);

        if (existingUser == null && (password == null || password.isBlank())) {
            errors.add("新建用户必须提供 password。");
        }
        if (existingUser != null && (password == null || password.isBlank())) {
            warnings.add("未提供密码，系统将保留该用户原有密码。");
        }
        if (password != null && !password.isBlank()) {
            try {
                credentialPolicy.validatePassword("password", password);
            } catch (IllegalArgumentException ex) {
                errors.add(ex.getMessage());
            }
        }

        detectConflicts("email", email, existingUser, usersByEmail, fileEmails, rowNumber, errors);
        detectConflicts("phone", phone, existingUser, usersByPhone, filePhones, rowNumber, errors);

        UserRequest request = new UserRequest(
                username,
                password == null || password.isBlank() ? null : password,
                email,
                phone,
                status,
                department == null ? null : department.getId(),
                roles.stream().map(Role::getId).collect(Collectors.toCollection(LinkedHashSet::new))
        );
        validateRequest(request, errors);

        return new ResolvedRow(
                rowNumber,
                username,
                operation,
                request,
                existingUser,
                List.copyOf(warnings),
                List.copyOf(errors)
        );
    }

    private void validateRequest(UserRequest request, List<String> errors) {
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        violations.stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .filter(message -> !message.isBlank())
                .forEach(errors::add);
    }

    private void detectConflicts(String fieldName,
                                 String value,
                                 User existingUser,
                                 Map<String, User> existingIndex,
                                 Map<String, Integer> fileIndex,
                                 int rowNumber,
                                 List<String> errors) {
        String key = normalizeKey(value);
        if (key == null) {
            return;
        }

        Integer seenRow = fileIndex.putIfAbsent(key, rowNumber);
        if (seenRow != null && seenRow != rowNumber) {
            errors.add(fieldName + " 在 CSV 中重复出现：第 " + seenRow + " 行和第 " + rowNumber + " 行。");
        }

        User owner = existingIndex.get(key);
        if (owner != null && (existingUser == null || !owner.getId().equals(existingUser.getId()))) {
            errors.add(fieldName + " 已被现有用户占用：" + value);
        }
    }

    private UserStatus parseStatus(String rawStatus, List<String> errors) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return UserStatus.ACTIVE;
        }
        try {
            UserStatus status = UserStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
            if (status == UserStatus.LOCKED) {
                errors.add("导入状态仅允许 ACTIVE 或 DISABLED。");
            }
            return status;
        } catch (IllegalArgumentException ex) {
            errors.add("状态非法，仅支持 ACTIVE 或 DISABLED。");
            return UserStatus.ACTIVE;
        }
    }

    private Department resolveDepartment(String departmentPath, List<Department> departments, List<String> errors) {
        if (departmentPath == null || departmentPath.isBlank()) {
            return null;
        }

        List<String> segments = Arrays.stream(departmentPath.split("/"))
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toList();
        if (segments.isEmpty()) {
            return null;
        }

        List<Department> currentLevel = departments.stream()
                .filter(department -> department.getParent() == null)
                .toList();
        Department current = null;
        for (String segment : segments) {
            Department next = currentLevel.stream()
                    .filter(department -> segment.equals(department.getName()))
                    .findFirst()
                    .orElse(null);
            if (next == null) {
                errors.add("部门路径不存在：" + departmentPath);
                return null;
            }
            current = next;
            currentLevel = new ArrayList<>(next.getChildren());
        }
        return current;
    }

    private Set<Role> resolveRoles(String roleCodesText, Map<String, Role> rolesByCode, List<String> errors) {
        if (roleCodesText == null || roleCodesText.isBlank()) {
            return new LinkedHashSet<>();
        }
        Set<Role> roles = new LinkedHashSet<>();
        for (String code : roleCodesText.split(",")) {
            String trimmed = code.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            Role role = rolesByCode.get(normalizeKey(trimmed));
            if (role == null) {
                errors.add("未知角色编码：" + trimmed);
                continue;
            }
            roles.add(role);
        }
        return roles;
    }

    private boolean isBlankRow(CSVRecord record) {
        for (String header : TEMPLATE_HEADERS) {
            if (!value(record, header).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String value(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header).trim() : "";
    }

    private List<String> sanitizeHeaders(List<String> headers) {
        return headers.stream()
                .map(header -> header == null ? "" : header.replace("\uFEFF", "").trim())
                .toList();
    }

    private CSVParser parse(String content) {
        try {
            return CSVParser.parse(
                    new StringReader(content),
                    CSVFormat.DEFAULT.builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .setIgnoreEmptyLines(true)
                            .setTrim(true)
                            .build()
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("CSV 解析失败，请检查文件编码与格式。", ex);
        }
    }

    private String readContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取上传文件失败。", ex);
        }
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private UserImportResponse invalidFileResponse(String message) {
        UserImportRowResponse row = new UserImportRowResponse(1, null, "FILE", "ERROR", List.of(), List.of(message));
        return new UserImportResponse(new UserImportSummaryResponse(0, 0, 0, 1, false), List.of(row));
    }

    private record ResolvedRow(
            int rowNumber,
            String username,
            String operation,
            UserRequest request,
            User existingUser,
            List<String> warnings,
            List<String> errors
    ) {
        UserImportRowResponse response(boolean committed) {
            return new UserImportRowResponse(
                    rowNumber,
                    username,
                    operation,
                    errors.isEmpty() ? (committed ? "IMPORTED" : "READY") : "ERROR",
                    warnings,
                    errors
            );
        }
    }

    private record Analysis(
            List<ResolvedRow> rows,
            UserImportResponse invalidResponse
    ) {
        UserImportResponse response(boolean committed) {
            if (invalidResponse != null) {
                return invalidResponse;
            }
            List<UserImportRowResponse> responses = rows.stream()
                    .map(row -> row.response(committed))
                    .toList();
            int successRows = (int) rows.stream().filter(row -> row.errors().isEmpty()).count();
            int warningRows = (int) rows.stream().filter(row -> !row.warnings().isEmpty()).count();
            int errorRows = (int) rows.stream().filter(row -> !row.errors().isEmpty()).count();
            return new UserImportResponse(
                    new UserImportSummaryResponse(rows.size(), successRows, warningRows, errorRows, committed && errorRows == 0),
                    responses
            );
        }
    }
}
