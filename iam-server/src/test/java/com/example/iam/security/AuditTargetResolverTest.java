package com.example.iam.security;

import com.example.iam.domain.AppClient;
import com.example.iam.repository.AppClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditTargetResolverTest {

    private final AppClientRepository appClientRepository = mock(AppClientRepository.class);
    private final AuditTargetResolver resolver = new AuditTargetResolver(appClientRepository);
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    @Test
    void shouldResolveOAuthClientFromSavedAuthorizationRequest() {
        AppClient client = AppClient.builder().clientId("app-a").clientName("Demo App A").build();
        when(appClientRepository.findByClientId("app-a")).thenReturn(Optional.of(client));

        MockHttpServletRequest authorizeRequest = new MockHttpServletRequest("GET", "/oauth2/authorize");
        authorizeRequest.setQueryString("response_type=code&client_id=app-a");
        authorizeRequest.setParameter("response_type", "code");
        authorizeRequest.setParameter("client_id", "app-a");
        requestCache.saveRequest(authorizeRequest, new MockHttpServletResponse());

        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/login");
        loginRequest.setSession(authorizeRequest.getSession(false));

        AuditTarget target = resolver.resolve(loginRequest);

        assertEquals("OAUTH_CLIENT", target.targetType());
        assertEquals("app-a", target.targetId());
        assertEquals("Demo App A", target.targetName());
    }

    @Test
    void shouldDefaultToIamConsoleWhenNoSavedRequestExists() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");

        AuditTarget target = resolver.resolve(request);

        assertEquals("IAM_CONSOLE", target.targetType());
        assertEquals("iam-console", target.targetId());
        assertEquals("IAM 控制台", target.targetName());
    }
}
