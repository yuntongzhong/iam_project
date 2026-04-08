package com.example.iam.domain;

import com.example.iam.domain.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_clients")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AppClient extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(nullable = false, length = 200)
    private String clientSecret;

    @Column(nullable = false, length = 100)
    private String clientName;

    @Builder.Default
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> redirectUris = new ArrayList<>();

    @Builder.Default
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> postLogoutRedirectUris = new ArrayList<>();

    @Builder.Default
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> scopes = new ArrayList<>();

    @Builder.Default
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> grantTypes = new ArrayList<>();

    @Builder.Default
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> authenticationMethods = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean requireProofKey = Boolean.TRUE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean requireAuthorizationConsent = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @OneToMany(mappedBy = "appClient")
    @ToString.Exclude
    private List<Permission> permissions = new ArrayList<>();
}
