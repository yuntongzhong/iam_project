package com.example.iam.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostMfaRedirectResolverTest {

    private final PostMfaRedirectResolver resolver = new PostMfaRedirectResolver();

    @Test
    void shouldFallbackToAdminPageWhenNoSavedRequestExists() {
        MockHttpServletRequest request = loginRequest();

        assertEquals("/admin.html", resolver.resolve(request, requestCache(null)));
    }

    @Test
    void shouldRestoreOriginalAuthorizationRequest() {
        MockHttpServletRequest request = loginRequest();

        assertEquals(
                "http://127.0.0.1:8080/oauth2/authorize?client_id=app-a&response_type=code",
                resolver.resolve(request, requestCache(savedRequest("http://127.0.0.1:8080/oauth2/authorize?client_id=app-a&response_type=code")))
        );
    }

    @Test
    void shouldIgnoreLoginAndMfaPages() {
        MockHttpServletRequest request = loginRequest();

        assertEquals("/admin.html", resolver.resolve(request, requestCache(savedRequest("http://127.0.0.1:8080/login"))));
        assertEquals("/admin.html", resolver.resolve(request, requestCache(savedRequest("http://127.0.0.1:8080/mfa/setup"))));
    }

    @Test
    void shouldIgnoreStaticAssetTargets() {
        MockHttpServletRequest request = loginRequest();

        assertEquals("/admin.html", resolver.resolve(request, requestCache(savedRequest("http://127.0.0.1:8080/robots.txt?continue"))));
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        return request;
    }

    private RequestCache requestCache(SavedRequest savedRequest) {
        return new RequestCache() {
            @Override
            public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
            }

            @Override
            public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
                return savedRequest;
            }

            @Override
            public HttpServletRequest getMatchingRequest(HttpServletRequest request, HttpServletResponse response) {
                return null;
            }

            @Override
            public void removeRequest(HttpServletRequest request, HttpServletResponse response) {
            }
        };
    }

    private SavedRequest savedRequest(String redirectUrl) {
        return new SavedRequest() {
            @Override
            public String getRedirectUrl() {
                return redirectUrl;
            }

            @Override
            public List<jakarta.servlet.http.Cookie> getCookies() {
                return List.of();
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public List<String> getHeaderValues(String name) {
                return List.of();
            }

            @Override
            public Collection<String> getHeaderNames() {
                return List.of();
            }

            @Override
            public List<Locale> getLocales() {
                return List.of(Locale.SIMPLIFIED_CHINESE);
            }

            @Override
            public String[] getParameterValues(String name) {
                return new String[0];
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return Collections.emptyMap();
            }
        };
    }
}
