package com.example.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "departments")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Department extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    private Department parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent")
    @ToString.Exclude
    private Set<Department> children = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "department")
    @ToString.Exclude
    private Set<User> users = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "department_roles",
            joinColumns = @JoinColumn(name = "department_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    private Set<Role> roles = new LinkedHashSet<>();
}
