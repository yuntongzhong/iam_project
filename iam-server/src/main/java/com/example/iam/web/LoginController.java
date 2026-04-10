package com.example.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String loginPage(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }

        String message = "";
        String tone = "info";

        if (request.getParameter("error") != null) {
            HttpSession session = request.getSession(false);
            AuthenticationException exception = session == null
                    ? null
                    : (AuthenticationException) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            message = switch (exception == null ? "" : exception.getMessage()) {
                case "User account is locked" -> "账号已锁定。连续输错密码次数过多，请先重置演示账号或由管理员解锁。";
                case "User account is disabled" -> "账号已被禁用。请联系管理员恢复状态后再登录。";
                default -> "用户名或密码错误。请重新输入，或使用 README 中的默认演示账号。";
            };
            tone = "error";
        } else if (request.getParameter("logout") != null) {
            message = "你已退出本地登录会话。若仍在其他客户端，请通过单点退出链路统一回收。";
        }

        String csrfField = csrfToken == null
                ? ""
                : """
                <input type="hidden" name="%s" value="%s">
                """.formatted(csrfToken.getParameterName(), csrfToken.getToken());

        String notice = message.isBlank()
                ? ""
                : """
                <div class="notice %s" role="alert">%s</div>
                """.formatted(tone, escapeHtml(message));

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>IAM 统一登录</title>
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
                            --primary: oklch(0.48 0.11 240);
                            --primary-deep: oklch(0.28 0.05 248);
                            --accent: oklch(0.74 0.1 84);
                            --danger: oklch(0.58 0.17 28);
                            --shadow: 0 30px 90px rgba(15, 23, 42, 0.1);
                        }

                        * { box-sizing: border-box; }

                        body {
                            margin: 0;
                            min-height: 100vh;
                            font-family: "Source Sans 3", "Microsoft YaHei", sans-serif;
                            color: var(--ink);
                            background:
                                radial-gradient(circle at 10%% 10%%, oklch(0.93 0.04 235 / 0.75), transparent 24%%),
                                radial-gradient(circle at 88%% 12%%, oklch(0.95 0.06 84 / 0.32), transparent 18%%),
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
                            max-width: 1320px;
                            margin: 0 auto;
                            min-height: 100vh;
                            display: grid;
                            grid-template-columns: minmax(0, 1.05fr) minmax(380px, 0.95fr);
                            gap: 22px;
                            padding: 24px;
                            align-items: stretch;
                        }

                        .hero,
                        .panel {
                            border-radius: 34px;
                            overflow: hidden;
                            box-shadow: var(--shadow);
                        }

                        .hero {
                            position: relative;
                            display: grid;
                            align-content: space-between;
                            gap: 24px;
                            padding: 34px;
                            background:
                                linear-gradient(135deg, var(--primary-deep), var(--primary) 64%%, oklch(0.55 0.12 220) 100%%);
                            color: oklch(0.975 0.007 95);
                        }

                        .hero::after {
                            content: "";
                            position: absolute;
                            right: -12%%;
                            bottom: -22%%;
                            width: 360px;
                            height: 360px;
                            border-radius: 50%%;
                            background: radial-gradient(circle, rgba(255,255,255,.2), transparent 64%%);
                        }

                        .eyebrow {
                            display: inline-flex;
                            padding: 8px 14px;
                            border-radius: 999px;
                            background: rgba(255,255,255,.12);
                            font: 700 12px/1 "Lexend", sans-serif;
                            letter-spacing: .14em;
                            text-transform: uppercase;
                        }

                        h1 {
                            margin: 16px 0 0;
                            max-width: 10ch;
                            font: 700 clamp(2.4rem, 4.5vw, 4.6rem)/1.02 "Lexend", sans-serif;
                        }

                        .hero p {
                            margin: 16px 0 0;
                            max-width: 58ch;
                            color: rgba(241, 246, 255, .82);
                            line-height: 1.82;
                            font-size: 1.02rem;
                        }

                        .hero-grid {
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 12px;
                        }

                        .hero-card {
                            position: relative;
                            z-index: 1;
                            padding: 16px;
                            border-radius: 24px;
                            background: rgba(248, 251, 255, 0.08);
                            border: 1px solid rgba(248, 251, 255, 0.16);
                        }

                        .hero-card strong {
                            display: block;
                            font: 700 1.2rem/1.2 "Lexend", sans-serif;
                            margin-top: 10px;
                        }

                        .hero-card span {
                            font-size: 12px;
                            letter-spacing: .08em;
                            text-transform: uppercase;
                            color: rgba(239, 245, 255, 0.64);
                        }

                        .panel {
                            padding: 30px;
                            background: var(--surface);
                            border: 1px solid rgba(71, 85, 105, 0.12);
                        }

                        .panel h2 {
                            margin: 0;
                            font: 700 2rem/1.08 "Lexend", sans-serif;
                        }

                        .panel p {
                            margin: 10px 0 0;
                            color: var(--ink-soft);
                            line-height: 1.72;
                        }

                        .stack {
                            display: grid;
                            gap: 16px;
                            margin-top: 22px;
                        }

                        .field {
                            display: grid;
                            gap: 8px;
                        }

                        .field label,
                        .helper-title {
                            font-size: 13px;
                            color: var(--ink-soft);
                        }

                        .input {
                            width: 100%%;
                            min-height: 50px;
                            padding: 12px 14px;
                            border-radius: 18px;
                            border: 1px solid rgba(71, 85, 105, 0.16);
                            background: rgba(255, 255, 255, 0.92);
                            color: var(--ink);
                        }

                        .input:focus-visible {
                            outline: 2px solid color-mix(in oklch, var(--primary) 72%%, white);
                            outline-offset: 2px;
                        }

                        .btn {
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 48px;
                            padding: 0 18px;
                            border-radius: 999px;
                            border: 1px solid transparent;
                            background: var(--ink);
                            color: oklch(0.982 0.004 95);
                            font-weight: 700;
                            cursor: pointer;
                            text-decoration: none;
                        }

                        .btn.secondary {
                            background: transparent;
                            color: var(--ink);
                            border-color: rgba(71, 85, 105, 0.14);
                        }

                        .notice {
                            padding: 14px 16px;
                            border-radius: 18px;
                            line-height: 1.7;
                            background: color-mix(in oklch, var(--primary) 10%%, white);
                            color: var(--ink);
                        }

                        .notice.error {
                            background: color-mix(in oklch, var(--danger) 12%%, white);
                            color: oklch(0.42 0.14 28);
                        }

                        .credential-grid {
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 12px;
                        }

                        .credential-card {
                            padding: 16px;
                            border-radius: 22px;
                            background: color-mix(in oklch, var(--surface-strong) 92%%, white);
                            border: 1px solid rgba(71, 85, 105, 0.1);
                        }

                        .credential-card strong {
                            display: block;
                            font: 700 1rem/1.3 "Lexend", sans-serif;
                            margin-bottom: 8px;
                        }

                        .credential-card code {
                            display: inline-block;
                            padding: 6px 10px;
                            margin-top: 8px;
                            border-radius: 999px;
                            background: color-mix(in oklch, var(--primary) 10%%, white);
                            color: var(--primary-deep);
                        }

                        .footer-links {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 12px;
                            margin-top: 8px;
                        }

                        .footer-links a {
                            color: var(--ink-soft);
                            text-decoration: none;
                        }

                        @media (max-width: 1080px) {
                            .shell { grid-template-columns: 1fr; }
                            .hero-grid,
                            .credential-grid { grid-template-columns: 1fr; }
                        }

                        @media (max-width: 720px) {
                            .shell { padding: 14px; }
                            .hero,
                            .panel { padding: 22px; border-radius: 24px; }
                            .btn { width: 100%%; }
                        }
                    </style>
                </head>
                <body>
                <main class="shell">
                    <section class="hero">
                        <div>
                            <div class="eyebrow">Identity Gateway</div>
                            <h1>统一身份认证入口</h1>
                            <p>这个入口同时服务 IAM 管理台与两个接入方应用。用户名密码验证通过后，系统会继续执行 TOTP 绑定或动态码校验，以完成完整的认证链路。</p>
                        </div>
                        <div class="hero-grid">
                            <article class="hero-card">
                                <span>协议</span>
                                <strong>OAuth2 / OIDC</strong>
                            </article>
                            <article class="hero-card">
                                <span>安全</span>
                                <strong>BCrypt + MFA</strong>
                            </article>
                            <article class="hero-card">
                                <span>会话</span>
                                <strong>SSO / SLO</strong>
                            </article>
                        </div>
                    </section>
                    <section class="panel">
                        <div>
                            <h2>登录并继续授权流程</h2>
                            <p>登录后会根据账号状态自动进入 MFA 绑定页或动态码输入页。该页面也可用于答辩时演示统一身份入口与默认演示账号。</p>
                        </div>
                        <div class="stack">
                            %s
                            <form method="post" action="/login" class="stack">
                                %s
                                <div class="field">
                                    <label for="username">用户名</label>
                                    <input class="input" id="username" name="username" type="text" autocomplete="username" placeholder="请输入用户名" required>
                                </div>
                                <div class="field">
                                    <label for="password">密码</label>
                                    <input class="input" id="password" name="password" type="password" autocomplete="current-password" placeholder="请输入密码" required>
                                </div>
                                <button class="btn" type="submit">进入 IAM 并继续认证</button>
                            </form>
                            <div class="stack">
                                <div class="helper-title">默认演示账号</div>
                                <div class="credential-grid">
                                    <article class="credential-card">
                                        <strong>管理员</strong>
                                        <div>admin</div>
                                        <code>Admin#2026!Secure</code>
                                    </article>
                                    <article class="credential-card">
                                        <strong>研发用户</strong>
                                        <div>alice</div>
                                        <code>Alice#2026!Secure</code>
                                    </article>
                                    <article class="credential-card">
                                        <strong>审计用户</strong>
                                        <div>bob</div>
                                        <code>Bob#2026!Audit</code>
                                    </article>
                                </div>
                            </div>
                            <div class="footer-links">
                                <a href="http://127.0.0.1:8081/">查看 Demo App A</a>
                                <a href="http://127.0.0.1:8082/">查看 Demo App B</a>
                            </div>
                        </div>
                    </section>
                </main>
                </body>
                </html>
                """.formatted(notice, csrfField);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
