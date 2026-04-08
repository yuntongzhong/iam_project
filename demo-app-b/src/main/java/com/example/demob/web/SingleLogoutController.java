package com.example.demob.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SingleLogoutController {

    private final String sessionCookieName;

    public SingleLogoutController(@Value("${server.servlet.session.cookie.name}") String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    @GetMapping(value = "/slo/front-channel-logout", produces = MediaType.TEXT_HTML_VALUE)
    public org.springframework.http.ResponseEntity<String> frontChannelLogout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie(sessionCookieName, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return org.springframework.http.ResponseEntity.ok("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"><title>front-channel logout</title></head>
                <body>ok</body>
                </html>
                """);
    }
}
