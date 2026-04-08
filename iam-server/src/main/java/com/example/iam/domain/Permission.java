package com.example.iam.domain;

import com.example.iam.domain.enums.PermissionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
@Table(name = "permissions")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Permission extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PermissionType permissionType;

    @Column(length = 150)
    private String resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_client_id")
    @ToString.Exclude
    private AppClient appClient;

    @Builder.Default
    @ManyToMany(mappedBy = "permissions")
    @ToString.Exclude
    private Set<Role> roles = new LinkedHashSet<>();
}
