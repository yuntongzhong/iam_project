package com.example.iam.security;

import com.example.iam.domain.User;
import com.example.iam.domain.enums.UserStatus;
import com.example.iam.repository.UserRepository;
import com.example.iam.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationEventListenerTest {

    @Test
    void shouldLockUserAfterFiveFailures() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuthenticationEventListener listener = new AuthenticationEventListener(userRepository, auditLogService);

        User user = User.builder()
                .username("alice")
                .email("alice@example.com")
                .phone("13800001111")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(4)
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                new UsernamePasswordAuthenticationToken("alice", "wrong"),
                new BadCredentialsException("Bad credentials")
        );

        listener.onFailure(event);

        verify(userRepository, times(1)).save(user);
        verify(auditLogService, times(1)).save(any(), any(), any(), any(), any());
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.LOCKED, user.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(5, user.getFailedLoginAttempts());
    }
}
