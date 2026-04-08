package com.example.iam.config;

import com.example.iam.domain.User;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.UserRepository;
import com.example.iam.security.MfaAuthenticationSuccessHandler;
import com.example.iam.security.MfaPendingRedirectFilter;
import com.example.iam.service.CustomUserDetailsService;
import com.example.iam.service.DepartmentRoleResolverService;
import com.example.iam.service.JpaRegisteredClientRepositoryAdapter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final MfaAuthenticationSuccessHandler mfaAuthenticationSuccessHandler;
    private final MfaPendingRedirectFilter mfaPendingRedirectFilter;
    private final JpaRegisteredClientRepositoryAdapter registeredClientRepository;
    private final UserRepository userRepository;
    private final DepartmentRoleResolverService departmentRoleResolverService;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer -> authorizationServer.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .addFilterBefore(mfaPendingRedirectFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/mfa/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.successHandler(mfaAuthenticationSuccessHandler))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/mfa/**"))
                .addFilterBefore(mfaPendingRedirectFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new InMemoryOAuth2AuthorizationConsentService();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${app.security.issuer}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            Authentication principal = context.getPrincipal();
            if (principal == null || principal.getName() == null) {
                return;
            }
            Set<String> roles = new LinkedHashSet<>();
            Set<String> permissions = new LinkedHashSet<>();
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                return;
            }

            user.getRoles().forEach(role -> {
                roles.add(role.getCode());
                role.getPermissions().forEach(permission -> permissions.add(permission.getCode()));
            });
            if (user.getDepartment() != null) {
                departmentRoleResolverService.resolveInheritedRoles(user.getDepartment()).forEach(role -> {
                    roles.add(role.getCode());
                    role.getPermissions().forEach(permission -> permissions.add(permission.getCode()));
                });
            }

            context.getClaims().claim("roles", roles);
            context.getClaims().claim("permissions", permissions);
        };
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
