package com.example.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MfaPendingRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        boolean pending = session != null && session.getAttribute(MfaSessionKeys.PENDING_AUTHENTICATION) != null;
        if (pending && !isAllowed(request.getRequestURI())) {
            String mode = String.valueOf(session.getAttribute(MfaSessionKeys.MFA_MODE));
            response.sendRedirect("VERIFY".equalsIgnoreCase(mode) ? "/mfa/verify" : "/mfa/setup");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String uri) {
        return uri.startsWith("/mfa/")
                || uri.startsWith("/error")
                || uri.startsWith("/login")
                || uri.startsWith("/logout")
                || uri.startsWith("/favicon");
    }
}
