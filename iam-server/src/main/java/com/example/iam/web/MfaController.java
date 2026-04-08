package com.example.iam.web;

import com.example.iam.domain.User;
import com.example.iam.repository.UserRepository;
import com.example.iam.security.MfaSessionKeys;
import com.example.iam.service.TotpService;
import com.example.iam.web.dto.MfaCodeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping(value = "/setup", produces = MediaType.TEXT_HTML_VALUE)
    public String setupPage(HttpServletRequest request) {
        User user = pendingUser(request.getSession(false));
        String qrBase64 = totpService.generateQrCodeBase64(user.getUsername(), user.getTotpSecret());
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>TOTP 绑定</title>
                    <style>
                        body{font-family:'Segoe UI',sans-serif;background:linear-gradient(135deg,#eef2ff,#f8fafc);padding:32px;}
                        .card{max-width:640px;margin:0 auto;background:#fff;border-radius:20px;padding:32px;box-shadow:0 20px 45px rgba(15,23,42,.12);}
                        h1{margin-top:0;color:#0f172a;} img{display:block;margin:20px auto;border:8px solid #e2e8f0;border-radius:18px;}
                        code{background:#eff6ff;padding:4px 8px;border-radius:8px;} input{width:100%%;padding:14px;border:1px solid #cbd5e1;border-radius:12px;margin:12px 0;}
                        button{background:#0f766e;color:#fff;border:0;padding:12px 18px;border-radius:12px;cursor:pointer;}
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>绑定多因素认证</h1>
                        <p>用户 <code>%s</code> 首次登录需要绑定 TOTP。请用 Google Authenticator 扫描二维码，然后输入 6 位动态码。</p>
                        <img alt="TOTP QR" src="data:image/png;base64,%s"/>
                        <form onsubmit="submitCode(event)">
                            <input id="code" type="text" maxlength="6" placeholder="输入 6 位动态码" />
                            <button type="submit">完成绑定并继续登录</button>
                        </form>
                    </div>
                    <script>
                        async function submitCode(event) {
                            event.preventDefault();
                            const code = document.getElementById('code').value;
                            const resp = await fetch('/mfa/setup', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({code})
                            });
                            if (resp.redirected) { window.location = resp.url; return; }
                            const data = await resp.text();
                            alert(data);
                        }
                    </script>
                </body>
                </html>
                """.formatted(user.getUsername(), qrBase64);
    }

    @PostMapping("/setup")
    public void setup(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        HttpSession session = servletRequest.getSession(false);
        User user = pendingUser(session);
        if (!totpService.verifyCode(user.getTotpSecret(), request.code())) {
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "动态码错误");
            return;
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
        finalizeAuthentication(session, servletRequest, servletResponse);
    }

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public String verifyPage(HttpServletRequest request) {
        User user = pendingUser(request.getSession(false));
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>MFA 校验</title>
                    <style>
                        body{font-family:'Segoe UI',sans-serif;background:linear-gradient(135deg,#fdf2f8,#eef2ff);padding:32px;}
                        .card{max-width:520px;margin:0 auto;background:#fff;border-radius:20px;padding:32px;box-shadow:0 20px 45px rgba(15,23,42,.12);}
                        h1{margin-top:0;color:#0f172a;} input{width:100%%;padding:14px;border:1px solid #cbd5e1;border-radius:12px;margin:12px 0;}
                        button{background:#1d4ed8;color:#fff;border:0;padding:12px 18px;border-radius:12px;cursor:pointer;}
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>输入动态码</h1>
                        <p>用户 <strong>%s</strong> 已启用 TOTP，请输入 6 位验证码继续授权流程。</p>
                        <form onsubmit="submitCode(event)">
                            <input id="code" type="text" maxlength="6" placeholder="输入 6 位动态码" />
                            <button type="submit">验证并继续登录</button>
                        </form>
                    </div>
                    <script>
                        async function submitCode(event) {
                            event.preventDefault();
                            const code = document.getElementById('code').value;
                            const resp = await fetch('/mfa/verify', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({code})
                            });
                            if (resp.redirected) { window.location = resp.url; return; }
                            const data = await resp.text();
                            alert(data);
                        }
                    </script>
                </body>
                </html>
                """.formatted(user.getUsername());
    }

    @PostMapping("/verify")
    public void verify(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        HttpSession session = servletRequest.getSession(false);
        User user = pendingUser(session);
        if (!totpService.verifyCode(user.getTotpSecret(), request.code())) {
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "动态码错误");
            return;
        }
        finalizeAuthentication(session, servletRequest, servletResponse);
    }

    private User pendingUser(HttpSession session) {
        if (session == null) {
            throw new IllegalStateException("MFA session not found");
        }
        Authentication authentication = (Authentication) session.getAttribute(MfaSessionKeys.PENDING_AUTHENTICATION);
        if (authentication == null) {
            throw new IllegalStateException("Pending authentication not found");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Pending user not found"));
    }

    private void finalizeAuthentication(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication authentication = (Authentication) session.getAttribute(MfaSessionKeys.PENDING_AUTHENTICATION);
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);
        Object redirectValue = session.getAttribute(MfaSessionKeys.POST_MFA_REDIRECT_URL);
        session.removeAttribute(MfaSessionKeys.PENDING_AUTHENTICATION);
        session.removeAttribute(MfaSessionKeys.POST_MFA_REDIRECT_URL);
        session.removeAttribute(MfaSessionKeys.MFA_MODE);
        response.sendRedirect(redirectValue == null ? "/admin.html" : redirectValue.toString());
    }
}
