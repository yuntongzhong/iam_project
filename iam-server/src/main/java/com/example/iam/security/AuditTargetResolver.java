package com.example.iam.security;

import com.example.iam.domain.AppClient;
import com.example.iam.repository.AppClientRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditTargetResolver {

    private static final AuditTarget IAM_CONSOLE = new AuditTarget("IAM_CONSOLE", "iam-console", "IAM 控制台");

    private final AppClientRepository appClientRepository;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    public AuditTarget resolve(HttpServletRequest request) {
        if (request == null) {
            return AuditTarget.none();
        }

        AuditTarget savedRequestTarget = resolveSavedRequestTarget(request).orElse(null);
        if (savedRequestTarget != null) {
            return savedRequestTarget;
        }

        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return AuditTarget.none();
        }
        if ("/".equals(requestUri) || "/admin.html".equals(requestUri) || "/login".equals(requestUri) || requestUri.startsWith("/mfa/")) {
            return IAM_CONSOLE;
        }
        if (requestUri.startsWith("/api/")) {
            return new AuditTarget("HTTP_RESOURCE", requestUri, requestUri);
        }
        return new AuditTarget("PAGE", requestUri, requestUri);
    }

    private Optional<AuditTarget> resolveSavedRequestTarget(HttpServletRequest request) {
        SavedRequest savedRequest = requestCache.getRequest(request, null);
        if (savedRequest == null || savedRequest.getRedirectUrl() == null || savedRequest.getRedirectUrl().isBlank()) {
            return Optional.empty();
        }

        var uri = UriComponentsBuilder.fromUriString(savedRequest.getRedirectUrl()).build();
        Map<String, String> queryParams = uri.getQueryParams().toSingleValueMap();
        String clientId = queryParams.get("client_id");
        if (clientId != null && !clientId.isBlank()) {
            return Optional.of(resolveClientTarget(clientId));
        }

        String path = uri.getPath();
        if (path == null || path.isBlank() || "/".equals(path) || "/admin.html".equals(path) || "/login".equals(path)) {
            return Optional.of(IAM_CONSOLE);
        }
        return Optional.of(new AuditTarget("PAGE", path, path));
    }

    private AuditTarget resolveClientTarget(String clientId) {
        return appClientRepository.findByClientId(clientId)
                .map(appClient -> new AuditTarget("OAUTH_CLIENT", appClient.getClientId(), clientDisplayName(appClient)))
                .orElseGet(() -> new AuditTarget("OAUTH_CLIENT", clientId, clientId));
    }

    private String clientDisplayName(AppClient appClient) {
        if (appClient.getClientName() == null || appClient.getClientName().isBlank()) {
            return appClient.getClientId();
        }
        return appClient.getClientName();
    }
}
