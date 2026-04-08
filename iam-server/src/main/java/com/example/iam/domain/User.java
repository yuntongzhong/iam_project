package com.example.iam.domain;

import com.example.iam.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class User extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, length = 200)
    private String password;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Builder.Default
    @Column(name = "is_totp_enabled", nullable = false)
    private Boolean totpEnabled = Boolean.FALSE;

    @Column(name = "totp_secret", length = 120)
    private String totpSecret;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @ToString.Exclude
    private Department department;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    private Set<Role> roles = new LinkedHashSet<>();
}
