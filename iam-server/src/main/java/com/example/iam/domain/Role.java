package com.example.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "roles")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Role extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @ToString.Exclude
    private Set<Permission> permissions = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "roles")
    @ToString.Exclude
    private Set<User> users = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "roles")
    @ToString.Exclude
    private Set<Department> departments = new LinkedHashSet<>();
}
