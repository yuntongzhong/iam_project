package com.example.iam.security;

import com.example.iam.domain.User;
import com.example.iam.repository.UserRepository;
import com.example.iam.service.TotpService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MfaAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final RedirectStrategy redirectStrategy = (request, response, url) -> response.sendRedirect(url);
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (!Boolean.TRUE.equals(user.getTotpEnabled()) && (user.getTotpSecret() == null || user.getTotpSecret().isBlank())) {
            user.setTotpSecret(totpService.generateSecret());
            userRepository.save(user);
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String targetUrl = savedRequest != null && UrlUtils.isValidRedirectUrl(savedRequest.getRedirectUrl())
                ? savedRequest.getRedirectUrl()
                : "/admin.html";

        HttpSession session = request.getSession(true);
        session.setAttribute(MfaSessionKeys.PENDING_AUTHENTICATION, authentication);
        session.setAttribute(MfaSessionKeys.POST_MFA_REDIRECT_URL, targetUrl);
        session.setAttribute(MfaSessionKeys.MFA_MODE, Boolean.TRUE.equals(user.getTotpEnabled()) ? "VERIFY" : "SETUP");
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        redirectStrategy.sendRedirect(request, response, Boolean.TRUE.equals(user.getTotpEnabled()) ? "/mfa/verify" : "/mfa/setup");
    }
}
