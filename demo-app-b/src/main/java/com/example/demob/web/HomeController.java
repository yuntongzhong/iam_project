package com.example.demob.web;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home(@AuthenticationPrincipal OAuth2User principal) {
        OidcUser oidcUser = principal instanceof OidcUser oidc ? oidc : null;
        return Map.of(
                "app", "demo-app-b",
                "username", principal.getName(),
                "claims", oidcUser != null ? oidcUser.getClaims() : principal.getAttributes()
        );
    }
}
