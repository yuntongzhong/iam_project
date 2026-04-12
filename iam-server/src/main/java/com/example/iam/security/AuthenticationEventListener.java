package com.example.iam.security;

import com.example.iam.domain.enums.AuditStatus;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.UserRepository;
import com.example.iam.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = String.valueOf(event.getAuthentication().getPrincipal());
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(UserStatus.LOCKED);
                auditLogService.save(username, "LOGIN_LOCKED", currentRequest(), AuditStatus.LOCKED, "Too many failed logins");
            } else {
                auditLogService.save(username, "LOGIN_FAILURE", currentRequest(), AuditStatus.FAILURE, "Bad credentials");
            }
            userRepository.save(user);
        });
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        Object principal = authentication.getPrincipal();
        String username = principal instanceof UserDetails userDetails ? userDetails.getUsername() : authentication.getName();
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            if (user.getStatus() != UserStatus.DISABLED) {
                user.setStatus(UserStatus.ACTIVE);
            }
            userRepository.save(user);
            auditLogService.save(username, "LOGIN_SUCCESS", currentRequest(), AuditStatus.SUCCESS, "Username/password authentication passed");
        });
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}
