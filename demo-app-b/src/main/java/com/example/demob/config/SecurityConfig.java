package com.example.demob.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            @Value("${app.security.iam-logout-uri}") String iamLogoutUri
    ) throws Exception {
        OAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        ((DefaultOAuth2AuthorizationRequestResolver) resolver)
                .setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/signed-out.html", "/slo/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2.authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver)))
                .oauth2Client(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String postLogoutRedirectUri = ServletUriComponentsBuilder.fromRequestUri(request)
                                    .replacePath(request.getContextPath() + "/signed-out.html")
                                    .replaceQuery(null)
                                    .build()
                                    .toUriString();
                            String redirectUri = UriComponentsBuilder.fromUriString(iamLogoutUri)
                                    .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
                                    .build()
                                    .toUriString();
                            response.sendRedirect(redirectUri);
                        }));

        return http.build();
    }
}
