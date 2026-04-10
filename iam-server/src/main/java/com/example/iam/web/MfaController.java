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
import org.springframework.http.HttpStatus;
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
        return renderMfaPage(
                "TOTP 绑定",
                "MFA Setup",
                "绑定多因素认证",
                "首次登录需要完成 TOTP 绑定。请使用 Google Authenticator 或 Microsoft Authenticator 扫描二维码，再输入当前 6 位动态码。",
                """
                <div class="info-strip">
                    <div><strong>用户</strong><span>%s</span></div>
                    <div><strong>阶段</strong><span>首次绑定</span></div>
                    <div><strong>目的</strong><span>建立第二认证因子</span></div>
                </div>
                <div class="step-grid">
                    <article class="step-card">
                        <span>01</span>
                        <strong>扫描二维码</strong>
                        <p>使用认证器 App 扫描下方二维码，自动写入当前账号的 TOTP 种子。</p>
                    </article>
                    <article class="step-card">
                        <span>02</span>
                        <strong>确认时间同步</strong>
                        <p>若手机时间未自动同步，验证码会失效。请先打开系统时间自动校准。</p>
                    </article>
                    <article class="step-card">
                        <span>03</span>
                        <strong>输入当前验证码</strong>
                        <p>输入认证器当前显示的 6 位动态码，系统会立即完成绑定并继续授权。</p>
                    </article>
                </div>
                """.formatted(escapeHtml(user.getUsername())),
                """
                <div class="qr-panel">
                    <img alt="TOTP QR" src="data:image/png;base64,%s">
                </div>
                """.formatted(qrBase64),
                "/mfa/setup",
                "完成绑定并继续登录"
        );
    }

    @PostMapping("/setup")
    public void setup(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        HttpSession session = servletRequest.getSession(false);
        User user = pendingUser(session);
        if (!totpService.verifyCode(user.getTotpSecret(), request.code())) {
            writeBadRequest(servletResponse, "动态码错误，请确认使用当前二维码重新绑定，或检查手机时间是否已自动同步。");
            return;
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
        writeSuccessRedirect(session, servletRequest, servletResponse);
    }

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public String verifyPage(HttpServletRequest request) {
        User user = pendingUser(request.getSession(false));
        return renderMfaPage(
                "MFA 校验",
                "MFA Verify",
                "输入动态码继续授权",
                "该账号已经启用 TOTP。请输入认证器当前显示的 6 位动态码，以恢复待完成的登录会话并继续进入 IAM 或下游应用。",
                """
                <div class="info-strip">
                    <div><strong>用户</strong><span>%s</span></div>
                    <div><strong>阶段</strong><span>二次校验</span></div>
                    <div><strong>状态</strong><span>等待动态码</span></div>
                </div>
                <div class="step-grid">
                    <article class="step-card">
                        <span>01</span>
                        <strong>打开认证器</strong>
                        <p>找到当前账号对应的条目，确认验证码仍在有效时间窗口内。</p>
                    </article>
                    <article class="step-card">
                        <span>02</span>
                        <strong>输入 6 位动态码</strong>
                        <p>验证码通常每 30 秒刷新一次，过期后请等待下一轮再输入。</p>
                    </article>
                    <article class="step-card">
                        <span>03</span>
                        <strong>继续原授权流程</strong>
                        <p>校验通过后，系统会恢复之前的待完成登录态并自动跳转。</p>
                    </article>
                </div>
                """.formatted(escapeHtml(user.getUsername())),
                "",
                "/mfa/verify",
                "验证并继续登录"
        );
    }

    @PostMapping("/verify")
    public void verify(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        HttpSession session = servletRequest.getSession(false);
        User user = pendingUser(session);
        if (!totpService.verifyCode(user.getTotpSecret(), request.code())) {
            writeBadRequest(servletResponse, "动态码错误，请确认验证码仍在有效时间窗口内；必要时重新扫码绑定。");
            return;
        }
        writeSuccessRedirect(session, servletRequest, servletResponse);
    }

    private String renderMfaPage(String title, String eyebrow, String headline, String intro, String leadContent, String formLeadContent, String submitPath, String buttonText) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Lexend:wght@400;500;600;700&family=Source+Sans+3:wght@400;500;600;700&display=swap" rel="stylesheet">
                    <style>
                        :root {
                            --bg: oklch(0.986 0.004 240);
                            --surface: oklch(0.998 0.002 240 / 0.96);
                            --surface-strong: oklch(0.972 0.008 232);
                            --line: oklch(0.892 0.014 234);
                            --ink: oklch(0.2 0.03 255);
                            --ink-soft: oklch(0.45 0.02 245);
                            --primary: oklch(0.56 0.13 252);
                            --primary-deep: oklch(0.33 0.08 255);
                            --accent: oklch(0.72 0.13 52);
                            --danger: oklch(0.58 0.17 28);
                            --shadow: 0 24px 72px rgba(15, 23, 42, 0.1);
                        }

                        * { box-sizing: border-box; }

                        body {
                            margin: 0;
                            min-height: 100vh;
                            font-family: "Source Sans 3", "Microsoft YaHei", sans-serif;
                            color: var(--ink);
                            background:
                                radial-gradient(circle at 10%% 10%%, oklch(0.93 0.05 250 / 0.72), transparent 24%%),
                                radial-gradient(circle at 88%% 12%%, oklch(0.92 0.08 52 / 0.26), transparent 18%%),
                                linear-gradient(180deg, oklch(0.992 0.003 230), var(--bg));
                        }

                        body::before {
                            content: "";
                            position: fixed;
                            inset: 0;
                            pointer-events: none;
                            background:
                                linear-gradient(90deg, transparent 0, transparent calc(100%% - 1px), rgba(51, 65, 85, 0.05) calc(100%% - 1px)),
                                linear-gradient(180deg, transparent 0, transparent calc(100%% - 1px), rgba(51, 65, 85, 0.045) calc(100%% - 1px));
                            background-size: 88px 88px;
                            mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.22), transparent 76%%);
                        }

                        .shell {
                            position: relative;
                            z-index: 1;
                            max-width: 1040px;
                            margin: 0 auto;
                            min-height: 100vh;
                            display: grid;
                            grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
                            gap: 14px;
                            padding: 14px;
                            align-items: center;
                        }

                        .hero,
                        .panel {
                            border-radius: 30px;
                            overflow: hidden;
                            box-shadow: var(--shadow);
                        }

                        .hero {
                            position: relative;
                            padding: 22px;
                            display: grid;
                            gap: 14px;
                            align-content: start;
                            background: linear-gradient(135deg, var(--primary-deep), var(--primary) 62%%, var(--accent) 100%%);
                            color: oklch(0.975 0.007 95);
                        }

                        .hero::after {
                            content: "";
                            position: absolute;
                            right: -12%%;
                            bottom: -22%%;
                            width: 340px;
                            height: 340px;
                            border-radius: 50%%;
                            background: radial-gradient(circle, rgba(255,255,255,.2), transparent 64%%);
                        }

                        .eyebrow {
                            display: inline-flex;
                            width: fit-content;
                            padding: 8px 14px;
                            border-radius: 999px;
                            background: rgba(255,255,255,.12);
                            font: 700 12px/1 "Lexend", sans-serif;
                            letter-spacing: .14em;
                            text-transform: uppercase;
                        }

                        h1 {
                            margin: 0;
                            max-width: 12ch;
                            font: 700 clamp(1.9rem, 3.5vw, 3rem)/1.02 "Lexend", sans-serif;
                        }

                        .hero p {
                            margin: 0;
                            max-width: 48ch;
                            color: rgba(241, 246, 255, .82);
                            line-height: 1.58;
                            font-size: 0.95rem;
                        }

                        .info-strip,
                        .step-grid {
                            display: grid;
                            gap: 8px;
                        }

                        .info-strip {
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                        }

                        .info-strip div,
                        .step-card {
                            position: relative;
                            z-index: 1;
                            padding: 12px;
                            border-radius: 18px;
                            background: rgba(248, 251, 255, 0.08);
                            border: 1px solid rgba(248, 251, 255, 0.16);
                        }

                        .info-strip strong,
                        .step-card span {
                            display: block;
                            font-size: 12px;
                            letter-spacing: .08em;
                            text-transform: uppercase;
                            color: rgba(239, 245, 255, 0.62);
                        }

                        .info-strip span {
                            display: block;
                            margin-top: 4px;
                            font: 700 0.98rem/1.4 "Lexend", sans-serif;
                        }

                        .step-grid {
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                        }

                        .step-card strong {
                            display: block;
                            margin: 6px 0 4px;
                            font: 700 0.96rem/1.3 "Lexend", sans-serif;
                        }

                        .step-card p {
                            margin: 0;
                            color: rgba(241, 246, 255, .78);
                            line-height: 1.54;
                            font-size: 0.9rem;
                        }

                        .qr-panel {
                            display: grid;
                            place-items: center;
                            padding: 12px;
                            border-radius: 20px;
                            background: rgba(255,255,255,.92);
                        }

                        .qr-panel img {
                            width: min(100%%, 220px);
                            border-radius: 14px;
                            border: 1px solid rgba(71, 85, 105, 0.12);
                        }

                        .panel {
                            padding: 20px;
                            background: var(--surface);
                            border: 1px solid rgba(71, 85, 105, 0.12);
                        }

                        .panel h2 {
                            margin: 0;
                            font: 700 1.52rem/1.08 "Lexend", sans-serif;
                        }

                        .panel p {
                            margin: 6px 0 0;
                            color: var(--ink-soft);
                            line-height: 1.56;
                            font-size: 0.95rem;
                        }

                        .stack {
                            display: grid;
                            gap: 10px;
                            margin-top: 14px;
                        }

                        .field {
                            display: grid;
                            gap: 8px;
                        }

                        .field label {
                            font-size: 13px;
                            color: var(--ink-soft);
                        }

                        .input {
                            width: 100%%;
                            min-height: 44px;
                            padding: 11px 12px;
                            border-radius: 16px;
                            border: 1px solid rgba(71, 85, 105, 0.16);
                            background: rgba(255, 255, 255, 0.92);
                            color: var(--ink);
                            font-size: 1rem;
                            letter-spacing: .08em;
                        }

                        .input:focus-visible {
                            outline: 2px solid color-mix(in oklch, var(--primary) 72%%, white);
                            outline-offset: 2px;
                        }

                        .btn {
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 44px;
                            padding: 0 18px;
                            border-radius: 999px;
                            border: 1px solid transparent;
                            background: var(--ink);
                            color: oklch(0.982 0.004 95);
                            font-weight: 700;
                            cursor: pointer;
                        }

                        .notice {
                            display: none;
                            padding: 12px 14px;
                            border-radius: 18px;
                            line-height: 1.6;
                            background: color-mix(in oklch, var(--danger) 12%%, white);
                            color: oklch(0.42 0.14 28);
                        }

                        .notice.show {
                            display: block;
                        }

                        .subtle {
                            padding: 12px;
                            border-radius: 16px;
                            background: color-mix(in oklch, var(--surface-strong) 92%%, white);
                            border: 1px solid rgba(71, 85, 105, 0.1);
                            color: var(--ink-soft);
                            line-height: 1.56;
                            font-size: 0.92rem;
                        }

                        @media (max-width: 1080px) {
                            .shell { grid-template-columns: 1fr; }
                            .info-strip,
                            .step-grid { grid-template-columns: 1fr; }
                        }

                        @media (max-width: 720px) {
                            .shell { padding: 14px; }
                            .hero,
                            .panel { padding: 18px; border-radius: 22px; }
                            .btn { width: 100%%; }
                        }
                    </style>
                </head>
                <body>
                <main class="shell">
                    <section class="hero">
                        <div class="eyebrow">%s</div>
                        <h1>%s</h1>
                        <p>%s</p>
                        %s
                    </section>
                    <section class="panel">
                        <div>
                            <h2>完成当前认证步骤</h2>
                            <p>输入当前认证器生成的 6 位动态码。校验通过后，系统会恢复之前暂停的登录会话并自动跳转到原目标地址。</p>
                        </div>
                        <div class="stack">
                            <div id="feedback" class="notice" role="alert"></div>
                            %s
                            <div class="subtle">如果连续输入失败，请优先检查手机时间是否自动同步；若是首次绑定失败，可重新扫码后再次输入。</div>
                            <form class="stack" onsubmit="submitCode(event)">
                                <div class="field">
                                    <label for="code">动态码</label>
                                    <input class="input" id="code" type="text" inputmode="numeric" maxlength="6" placeholder="输入 6 位动态码" autocomplete="one-time-code" required>
                                </div>
                                <button class="btn" type="submit">%s</button>
                            </form>
                        </div>
                    </section>
                </main>
                <script>
                    async function submitCode(event) {
                        event.preventDefault();
                        const codeInput = document.getElementById('code');
                        const feedback = document.getElementById('feedback');
                        feedback.classList.remove('show');
                        const resp = await fetch('%s', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify({code: codeInput.value})
                        });
                        const data = await resp.json().catch(() => ({}));
                        if (data.redirectUrl) {
                            window.location = data.redirectUrl;
                            return;
                        }
                        feedback.textContent = data.message || '动态码校验失败，请重试';
                        feedback.classList.add('show');
                    }
                </script>
                </body>
                </html>
                """.formatted(title, eyebrow, headline, intro, leadContent, formLeadContent, buttonText, submitPath);
    }

    private void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + escapeJson(message) + "\"}");
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

    private void writeSuccessRedirect(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication authentication = (Authentication) session.getAttribute(MfaSessionKeys.PENDING_AUTHENTICATION);
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);
        Object redirectValue = session.getAttribute(MfaSessionKeys.POST_MFA_REDIRECT_URL);
        session.removeAttribute(MfaSessionKeys.PENDING_AUTHENTICATION);
        session.removeAttribute(MfaSessionKeys.POST_MFA_REDIRECT_URL);
        session.removeAttribute(MfaSessionKeys.MFA_MODE);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"redirectUrl\":\"" + escapeJson(redirectValue == null ? "/admin.html" : redirectValue.toString()) + "\"}");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
