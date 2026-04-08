package com.example.iam.config;

import com.example.iam.domain.AppClient;
import com.example.iam.domain.Department;
import com.example.iam.domain.Permission;
import com.example.iam.domain.Role;
import com.example.iam.domain.User;
import com.example.iam.domain.enums.PermissionType;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.AppClientRepository;
import com.example.iam.repository.DepartmentRepository;
import com.example.iam.repository.PermissionRepository;
import com.example.iam.repository.RoleRepository;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AppClientRepository appClientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        Department headOffice = departmentRepository.save(Department.builder().name("总部").build());
        Department engineering = departmentRepository.save(Department.builder().name("研发中心").parent(headOffice).build());
        Department sales = departmentRepository.save(Department.builder().name("销售中心").parent(headOffice).build());

        AppClient appA = appClientRepository.save(AppClient.builder()
                .clientId(DemoCredentialCatalog.APP_A_CLIENT_ID)
                .clientSecret(passwordEncoder.encode(DemoCredentialCatalog.APP_A_CLIENT_SECRET))
                .clientName("Demo App A")
                .redirectUris(List.of("http://127.0.0.1:8081/login/oauth2/code/iam"))
                .postLogoutRedirectUris(List.of("http://127.0.0.1:8081/"))
                .scopes(List.of("openid", "profile"))
                .grantTypes(List.of("authorization_code", "refresh_token"))
                .authenticationMethods(List.of("client_secret_basic"))
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .active(true)
                .build());

        AppClient appB = appClientRepository.save(AppClient.builder()
                .clientId(DemoCredentialCatalog.APP_B_CLIENT_ID)
                .clientSecret(passwordEncoder.encode(DemoCredentialCatalog.APP_B_CLIENT_SECRET))
                .clientName("Demo App B")
                .redirectUris(List.of("http://127.0.0.1:8082/login/oauth2/code/iam"))
                .postLogoutRedirectUris(List.of("http://127.0.0.1:8082/"))
                .scopes(List.of("openid", "profile"))
                .grantTypes(List.of("authorization_code", "refresh_token"))
                .authenticationMethods(List.of("client_secret_basic"))
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .active(true)
                .build());

        Permission userRead = permissionRepository.save(permission("USER_READ", "用户查询", PermissionType.FUNCTION, "/api/users", null));
        Permission userWrite = permissionRepository.save(permission("USER_WRITE", "用户写入", PermissionType.FUNCTION, "/api/users", null));
        Permission deptRead = permissionRepository.save(permission("DEPARTMENT_READ", "部门查询", PermissionType.FUNCTION, "/api/departments", null));
        Permission deptWrite = permissionRepository.save(permission("DEPARTMENT_WRITE", "部门写入", PermissionType.FUNCTION, "/api/departments", null));
        Permission roleRead = permissionRepository.save(permission("ROLE_READ", "角色查询", PermissionType.FUNCTION, "/api/roles", null));
        Permission roleWrite = permissionRepository.save(permission("ROLE_WRITE", "角色写入", PermissionType.FUNCTION, "/api/roles", null));
        Permission appRead = permissionRepository.save(permission("APP_READ", "应用查询", PermissionType.FUNCTION, "/api/apps", null));
        Permission appWrite = permissionRepository.save(permission("APP_WRITE", "应用写入", PermissionType.FUNCTION, "/api/apps", null));
        Permission auditRead = permissionRepository.save(permission("AUDIT_READ", "审计日志查询", PermissionType.FUNCTION, "/api/audit-logs", null));
        Permission appAAccess = permissionRepository.save(permission("APP_A_ACCESS", "访问 Demo App A", PermissionType.APPLICATION, appA.getClientId(), appA));
        Permission appBAccess = permissionRepository.save(permission("APP_B_ACCESS", "访问 Demo App B", PermissionType.APPLICATION, appB.getClientId(), appB));

        Role admin = roleRepository.save(Role.builder()
                .code("ADMIN")
                .name("系统管理员")
                .description("拥有全部后台管理能力")
                .permissions(new LinkedHashSet<>(List.of(userRead, userWrite, deptRead, deptWrite, roleRead, roleWrite, appRead, appWrite, auditRead, appAAccess, appBAccess)))
                .build());

        Role engineer = roleRepository.save(Role.builder()
                .code("ENGINEER")
                .name("研发工程师")
                .description("继承研发部门基础访问权限")
                .permissions(new LinkedHashSet<>(List.of(appAAccess, appBAccess)))
                .build());

        Role auditor = roleRepository.save(Role.builder()
                .code("AUDITOR")
                .name("审计员")
                .description("查看登录审计")
                .permissions(new LinkedHashSet<>(List.of(auditRead)))
                .build());

        headOffice.setRoles(new LinkedHashSet<>(Set.of(admin)));
        engineering.setRoles(new LinkedHashSet<>(Set.of(engineer)));
        sales.setRoles(new LinkedHashSet<>(Set.of(auditor)));
        departmentRepository.saveAll(List.of(headOffice, engineering, sales));

        userRepository.save(User.builder()
                .username(DemoCredentialCatalog.ADMIN_USERNAME)
                .password(passwordEncoder.encode(DemoCredentialCatalog.ADMIN_PASSWORD))
                .email("admin@example.com")
                .phone("13800001234")
                .status(UserStatus.ACTIVE)
                .department(headOffice)
                .roles(new LinkedHashSet<>(Set.of(admin)))
                .build());

        userRepository.save(User.builder()
                .username(DemoCredentialCatalog.ALICE_USERNAME)
                .password(passwordEncoder.encode(DemoCredentialCatalog.ALICE_PASSWORD))
                .email("alice@example.com")
                .phone("13800005678")
                .status(UserStatus.ACTIVE)
                .department(engineering)
                .roles(new LinkedHashSet<>(Set.of(engineer)))
                .build());

        userRepository.save(User.builder()
                .username(DemoCredentialCatalog.BOB_USERNAME)
                .password(passwordEncoder.encode(DemoCredentialCatalog.BOB_PASSWORD))
                .email("bob@example.com")
                .phone("13800009876")
                .status(UserStatus.ACTIVE)
                .department(sales)
                .roles(new LinkedHashSet<>(Set.of(auditor)))
                .build());
    }

    private Permission permission(String code, String name, PermissionType type, String resource, AppClient appClient) {
        return Permission.builder()
                .code(code)
                .name(name)
                .description(name)
                .permissionType(type)
                .resource(resource)
                .appClient(appClient)
                .build();
    }
}
