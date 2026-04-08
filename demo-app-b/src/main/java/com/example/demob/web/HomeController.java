package com.example.demob.web;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
public class HomeController {

    @GetMapping("/api/session")
    public Map<String, Object> session(@AuthenticationPrincipal OidcUser principal) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app", "demo-app-b");
        payload.put("title", "Demo App B");
        payload.put("username", principal.getPreferredUsername() != null ? principal.getPreferredUsername() : principal.getName());
        payload.put("subject", principal.getSubject());
        payload.put("email", principal.getEmail());
        payload.put("issuedAt", principal.getIssuedAt());
        payload.put("expiresAt", principal.getExpiresAt());
        payload.put("roles", toStringList(principal.getClaim("roles")));
        payload.put("permissions", toStringList(principal.getClaim("permissions")));
        payload.put("authorities", principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList());
        payload.put("claims", new TreeMap<>(principal.getClaims()));
        return payload;
    }

    private List<String> toStringList(Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
