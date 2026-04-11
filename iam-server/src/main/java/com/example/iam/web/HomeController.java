package com.example.iam.web;

import com.example.iam.repository.UserRepository;
import com.example.iam.service.UserClaimsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserClaimsService userClaimsService;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        return "redirect:/admin.html";
    }

    @GetMapping("/api/session")
    @ResponseBody
    public Map<String, Object> session(Authentication authentication) {
        UserClaimsService.UserClaimSnapshot snapshot = userClaimsService.buildSnapshot(authentication.getName());
        var user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app", "iam-console");
        payload.put("title", "IAM 控制台");
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail());
        payload.put("departmentName", user.getDepartment() == null ? null : user.getDepartment().getName());
        payload.put("roles", snapshot.roles());
        payload.put("permissions", snapshot.permissions());
        payload.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList());
        return payload;
    }
}
