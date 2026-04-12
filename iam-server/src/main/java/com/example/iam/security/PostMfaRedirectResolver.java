package com.example.iam.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PostMfaRedirectResolver {

    private static final String DEFAULT_TARGET_URL = "/admin.html";
    private static final Set<String> DISALLOWED_PREFIXES = Set.of("/login", "/mfa/", "/logout", "/error");

    public String resolve(HttpServletRequest request, RequestCache requestCache) {
        SavedRequest savedRequest = requestCache.getRequest(request, null);
        if (savedRequest == null) {
            return DEFAULT_TARGET_URL;
        }

        String redirectUrl = savedRequest.getRedirectUrl();
        if (!UrlUtils.isValidRedirectUrl(redirectUrl)) {
            return DEFAULT_TARGET_URL;
        }

        String requestUri = UrlUtils.buildFullRequestUrl(request);
        if (redirectUrl.equals(requestUri)) {
            return DEFAULT_TARGET_URL;
        }

        String path = extractPath(redirectUrl);
        if (DISALLOWED_PREFIXES.stream().anyMatch(path::startsWith) || looksLikeStaticAsset(path)) {
            return DEFAULT_TARGET_URL;
        }

        return redirectUrl;
    }

    private String extractPath(String redirectUrl) {
        if (!redirectUrl.startsWith("http://") && !redirectUrl.startsWith("https://")) {
            return redirectUrl;
        }

        int schemeSeparator = redirectUrl.indexOf("://");
        int pathStart = redirectUrl.indexOf('/', schemeSeparator + 3);
        if (pathStart < 0) {
            return "/";
        }
        return redirectUrl.substring(pathStart);
    }

    private boolean looksLikeStaticAsset(String path) {
        int queryStart = path.indexOf('?');
        String cleanPath = queryStart >= 0 ? path.substring(0, queryStart) : path;
        return cleanPath.matches(".+\\.(txt|ico|png|jpg|jpeg|gif|svg|webp|css|js|map|woff2?)$");
    }
}
