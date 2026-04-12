package com.example.iam.service;

import com.example.iam.domain.AppClient;
import com.example.iam.domain.AuditLog;
import com.example.iam.domain.enums.AuditStatus;
import com.example.iam.repository.AppClientRepository;
import com.example.iam.repository.AuditLogRepository;
import com.example.iam.security.AuditTargetResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final AppClientRepository appClientRepository = mock(AppClientRepository.class);
    private final AuditLogService auditLogService = new AuditLogService(
            auditLogRepository,
            new AuditTargetResolver(appClientRepository)
    );
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    @Test
    void saveShouldCaptureOauthClientTargetFromSavedRequest() {
        AppClient appClient = AppClient.builder().clientId("app-b").clientName("Demo App B").build();
        when(appClientRepository.findByClientId("app-b")).thenReturn(Optional.of(appClient));

        MockHttpServletRequest authorizeRequest = new MockHttpServletRequest("GET", "/oauth2/authorize");
        authorizeRequest.setQueryString("response_type=code&client_id=app-b");
        authorizeRequest.setParameter("response_type", "code");
        authorizeRequest.setParameter("client_id", "app-b");
        authorizeRequest.addHeader("User-Agent", "JUnit Browser");
        authorizeRequest.setRemoteAddr("127.0.0.1");
        requestCache.saveRequest(authorizeRequest, new MockHttpServletResponse());

        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/login");
        loginRequest.setSession(authorizeRequest.getSession(false));
        loginRequest.addHeader("User-Agent", "JUnit Browser");
        loginRequest.setRemoteAddr("127.0.0.1");

        auditLogService.save("alice", "LOGIN_FAILURE", loginRequest, AuditStatus.FAILURE, "Bad credentials");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals("alice", log.getUsername());
        assertEquals("OAUTH_CLIENT", log.getTargetType());
        assertEquals("app-b", log.getTargetId());
        assertEquals("Demo App B", log.getTargetName());
        assertEquals("127.0.0.1", log.getIpAddress());
        assertEquals("JUnit Browser", log.getUserAgent());
    }

    @Test
    void saveShouldDefaultToIamConsoleForDirectLogin() {
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/login");
        loginRequest.addHeader("User-Agent", "JUnit Browser");
        loginRequest.setRemoteAddr("127.0.0.1");

        auditLogService.save("admin", "LOGIN_SUCCESS", loginRequest, AuditStatus.SUCCESS, "Username/password authentication passed");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals("IAM_CONSOLE", log.getTargetType());
        assertEquals("iam-console", log.getTargetId());
        assertNotNull(log.getTargetName());
    }
}
