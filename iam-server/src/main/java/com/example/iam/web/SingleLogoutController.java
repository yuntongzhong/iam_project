package com.example.iam.web;

import com.example.iam.config.AppSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SingleLogoutController {

    private final AppSecurityProperties appSecurityProperties;

    @GetMapping(value = "/slo/front-channel", produces = MediaType.TEXT_HTML_VALUE)
    public org.springframework.http.ResponseEntity<String> frontChannelLogout(
            @RequestParam(name = "post_logout_redirect_uri", required = false) String requestedRedirect
    ) {
        String finalRedirect = resolvePostLogoutRedirect(requestedRedirect);
        List<String> logoutUris = appSecurityProperties.getFrontChannelLogoutUris();

        StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>正在完成单点登出</title>
                    <meta name="description" content="IAM 单点登出中转页，用于通知各客户端清理本地会话并在完成后跳转到退出结果页。">
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Lexend:wght@400;500;600;700&family=Source+Sans+3:wght@400;500;600;700&display=swap" rel="stylesheet">
                    <style>
                        :root {
                            --bg: oklch(0.986 0.004 240);
                            --surface: oklch(0.998 0.002 240 / 0.96);
                            --surface-strong: oklch(0.972 0.008 232);
                            --ink: oklch(0.2 0.03 255);
                            --ink-soft: oklch(0.45 0.02 245);
                            --primary: oklch(0.56 0.13 252);
                            --primary-deep: oklch(0.33 0.08 255);
                            --accent: oklch(0.72 0.13 52);
                            --shadow: 0 24px 72px rgba(15, 23, 42, 0.1);
                        }

                        * { box-sizing: border-box; }

                        body {
                            margin: 0;
                            min-height: 100vh;
                            display: grid;
                            place-items: center;
                            padding: 20px;
                            font-family: "Source Sans 3", "Microsoft YaHei", sans-serif;
                            color: var(--ink);
                            background:
                                radial-gradient(circle at 10% 10%, oklch(0.93 0.05 250 / 0.72), transparent 24%),
                                radial-gradient(circle at 88% 12%, oklch(0.92 0.08 52 / 0.26), transparent 18%),
                                linear-gradient(180deg, oklch(0.992 0.003 230), var(--bg));
                        }

                        body::before {
                            content: "";
                            position: fixed;
                            inset: 0;
                            pointer-events: none;
                            background:
                                linear-gradient(90deg, transparent 0, transparent calc(100% - 1px), rgba(51, 65, 85, 0.05) calc(100% - 1px)),
                                linear-gradient(180deg, transparent 0, transparent calc(100% - 1px), rgba(51, 65, 85, 0.045) calc(100% - 1px));
                            background-size: 88px 88px;
                            mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.2), transparent 72%);
                        }

                        .shell {
                            position: relative;
                            z-index: 1;
                            width: min(980px, 100%);
                            display: grid;
                            grid-template-columns: minmax(0, 1.02fr) minmax(320px, 0.98fr);
                            gap: 18px;
                        }

                        .hero,
                        .panel {
                            border-radius: 30px;
                            overflow: hidden;
                            box-shadow: var(--shadow);
                        }

                        .hero {
                            position: relative;
                            padding: 28px;
                            display: grid;
                            gap: 16px;
                            align-content: start;
                            background: linear-gradient(135deg, var(--primary-deep), var(--primary) 62%, var(--accent) 100%);
                            color: oklch(0.976 0.006 95);
                        }

                        .hero::after {
                            content: "";
                            position: absolute;
                            right: -12%;
                            bottom: -22%;
                            width: 320px;
                            height: 320px;
                            border-radius: 50%;
                            background: radial-gradient(circle, rgba(255,255,255,.18), transparent 64%);
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
                            max-width: 11ch;
                            font: 700 clamp(2rem, 4vw, 3.2rem)/1.04 "Lexend", sans-serif;
                        }

                        .hero p {
                            margin: 0;
                            max-width: 48ch;
                            color: rgba(241, 246, 255, .84);
                            line-height: 1.7;
                        }

                        .status-grid {
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 10px;
                        }

                        .status-card {
                            padding: 14px;
                            border-radius: 20px;
                            background: rgba(248, 251, 255, 0.1);
                            border: 1px solid rgba(248, 251, 255, 0.16);
                        }

                        .status-card span {
                            display: block;
                            font-size: 12px;
                            letter-spacing: .08em;
                            text-transform: uppercase;
                            color: rgba(239, 245, 255, 0.66);
                        }

                        .status-card strong {
                            display: block;
                            margin-top: 8px;
                            font: 700 0.96rem/1.35 "Lexend", sans-serif;
                        }

                        .panel {
                            padding: 24px;
                            background: var(--surface);
                            border: 1px solid rgba(71, 85, 105, 0.12);
                            display: grid;
                            gap: 16px;
                            align-content: start;
                        }

                        .spinner {
                            width: 44px;
                            height: 44px;
                            border-radius: 999px;
                            border: 4px solid color-mix(in oklch, var(--primary) 16%, white);
                            border-top-color: var(--primary);
                            animation: spin 1s linear infinite;
                        }

                        .panel h2 {
                            margin: 0;
                            font: 700 1.68rem/1.08 "Lexend", sans-serif;
                        }

                        .panel p {
                            margin: 0;
                            color: var(--ink-soft);
                            line-height: 1.68;
                        }

                        .hint {
                            padding: 14px;
                            border-radius: 18px;
                            background: color-mix(in oklch, var(--surface-strong) 92%, white);
                            border: 1px solid rgba(71, 85, 105, 0.08);
                        }

                        .fallback a {
                            color: var(--primary-deep);
                            font-weight: 700;
                            text-decoration: none;
                        }

                        iframe { display: none; width: 0; height: 0; border: 0; }

                        @keyframes spin { to { transform: rotate(360deg); } }

                        @media (max-width: 960px) {
                            .shell,
                            .status-grid {
                                grid-template-columns: 1fr;
                            }
                        }

                        @media (max-width: 720px) {
                            .hero,
                            .panel {
                                padding: 20px;
                                border-radius: 24px;
                            }
                        }
                    </style>
                </head>
                <body>
                <main class="shell">
                    <section class="hero">
                        <div class="eyebrow">IAM Single Logout</div>
                        <h1>正在清理所有客户端会话</h1>
                        <p>IAM 已销毁全局登录态，当前页面正在通知 Demo App A 和 Demo App B 清理各自的本地 Session。完成后会自动跳转到退出完成页。</p>
                        <div class="status-grid">
                            <article class="status-card">
                                <span>第 1 步</span>
                                <strong>销毁 IAM 全局会话</strong>
                            </article>
                            <article class="status-card">
                                <span>第 2 步</span>
                                <strong>通知下游客户端同步退出</strong>
                            </article>
                            <article class="status-card">
                                <span>第 3 步</span>
                                <strong>跳转到退出完成页</strong>
                            </article>
                        </div>
                    </section>
                    <section class="panel">
                        <div class="spinner" aria-hidden="true"></div>
                        <div>
                            <h2>单点退出流程正在进行</h2>
                            <p>这一页是 SLO 的中转页，用来承接全局退出与前端通知客户端的动作，保证多个接入方的本地登录态能够一起回收。</p>
                        </div>
                        <div class="hint">如果页面短时间内没有自动跳转，通常说明浏览器还在等待前端通知完成；你也可以手动继续进入退出完成页。</div>
                        <div class="fallback">如果页面没有自动跳转，请点击 <a id="continueLink" href="__FINAL_REDIRECT_HTML__">继续</a>。</div>
                    </section>
                """.replace("__FINAL_REDIRECT_HTML__", escapeHtml(finalRedirect)));

        for (String logoutUri : logoutUris) {
            html.append("<iframe src=\"")
                    .append(escapeHtml(logoutUri))
                    .append("\" loading=\"eager\"></iframe>");
        }

        html.append("""
                </main>
                <script>
                window.setTimeout(function () {
                    window.location.replace(__FINAL_REDIRECT_JS__);
                }, 900);
                </script>
                </body>
                </html>
                """.replace("__FINAL_REDIRECT_JS__", toJsString(finalRedirect)));

        return org.springframework.http.ResponseEntity.ok(html.toString());
    }

    @GetMapping(value = "/logged-out", produces = MediaType.TEXT_HTML_VALUE)
    public org.springframework.http.ResponseEntity<String> loggedOutPage() {
        return org.springframework.http.ResponseEntity.ok("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>IAM 已退出</title>
                    <meta name="description" content="IAM 单点退出完成页，用于展示全局会话已回收，并给出重新进入系统的入口。">
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Lexend:wght@400;500;600;700&family=Source+Sans+3:wght@400;500;600;700&display=swap" rel="stylesheet">
                    <style>
                        :root {
                            --bg: oklch(0.986 0.004 240);
                            --surface: oklch(0.998 0.002 240 / 0.96);
                            --surface-strong: oklch(0.972 0.008 232);
                            --ink: oklch(0.2 0.03 255);
                            --ink-soft: oklch(0.45 0.02 245);
                            --primary: oklch(0.56 0.13 252);
                            --primary-deep: oklch(0.33 0.08 255);
                            --accent: oklch(0.72 0.13 52);
                            --shadow: 0 24px 72px rgba(15, 23, 42, 0.1);
                        }

                        * { box-sizing: border-box; }

                        body {
                            margin: 0;
                            min-height: 100vh;
                            padding: 20px;
                            font-family: "Source Sans 3", "Microsoft YaHei", sans-serif;
                            color: var(--ink);
                            background:
                                radial-gradient(circle at 10% 10%, oklch(0.93 0.05 250 / 0.72), transparent 24%),
                                radial-gradient(circle at 88% 12%, oklch(0.92 0.08 52 / 0.26), transparent 18%),
                                linear-gradient(180deg, oklch(0.992 0.003 230), var(--bg));
                        }

                        body::before {
                            content: "";
                            position: fixed;
                            inset: 0;
                            pointer-events: none;
                            background:
                                linear-gradient(90deg, transparent 0, transparent calc(100% - 1px), rgba(51, 65, 85, 0.05) calc(100% - 1px)),
                                linear-gradient(180deg, transparent 0, transparent calc(100% - 1px), rgba(51, 65, 85, 0.045) calc(100% - 1px));
                            background-size: 88px 88px;
                            mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.2), transparent 72%);
                        }

                        .shell {
                            position: relative;
                            z-index: 1;
                            width: min(980px, 100%);
                            min-height: calc(100vh - 40px);
                            margin: 0 auto;
                            display: grid;
                            grid-template-columns: minmax(0, 1.04fr) minmax(320px, 0.96fr);
                            gap: 18px;
                            align-items: center;
                        }

                        .hero,
                        .panel {
                            overflow: hidden;
                            border-radius: 30px;
                            box-shadow: var(--shadow);
                        }

                        .hero {
                            position: relative;
                            padding: 28px;
                            display: grid;
                            gap: 16px;
                            align-content: start;
                            background: linear-gradient(135deg, var(--primary-deep), var(--primary) 62%, var(--accent) 100%);
                            color: oklch(0.976 0.006 95);
                        }

                        .hero::after {
                            content: "";
                            position: absolute;
                            right: -12%;
                            bottom: -22%;
                            width: 320px;
                            height: 320px;
                            border-radius: 50%;
                            background: radial-gradient(circle, rgba(255,255,255,.18), transparent 64%);
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
                            max-width: 10ch;
                            font: 700 clamp(2rem, 4vw, 3.2rem)/1.04 "Lexend", sans-serif;
                        }

                        .hero p {
                            margin: 0;
                            max-width: 48ch;
                            color: rgba(241, 246, 255, .84);
                            line-height: 1.7;
                        }

                        .status-grid {
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 10px;
                        }

                        .status-card {
                            padding: 14px;
                            border-radius: 20px;
                            background: rgba(248, 251, 255, 0.1);
                            border: 1px solid rgba(248, 251, 255, 0.16);
                        }

                        .status-card span {
                            display: block;
                            font-size: 12px;
                            letter-spacing: .08em;
                            text-transform: uppercase;
                            color: rgba(239, 245, 255, 0.66);
                        }

                        .status-card strong {
                            display: block;
                            margin-top: 8px;
                            font: 700 0.96rem/1.35 "Lexend", sans-serif;
                        }

                        .panel {
                            padding: 24px;
                            display: grid;
                            gap: 16px;
                            align-content: start;
                            background: var(--surface);
                            border: 1px solid rgba(71, 85, 105, 0.12);
                        }

                        .panel h2 {
                            margin: 0;
                            font: 700 1.68rem/1.08 "Lexend", sans-serif;
                        }

                        .panel p,
                        .tip {
                            margin: 0;
                            color: var(--ink-soft);
                            line-height: 1.68;
                        }

                        .tip {
                            padding: 14px;
                            border-radius: 18px;
                            background: color-mix(in oklch, var(--surface-strong) 92%, white);
                            border: 1px solid rgba(71, 85, 105, 0.08);
                        }

                        .tip strong {
                            display: block;
                            margin-bottom: 6px;
                            color: var(--ink);
                            font-family: "Lexend", sans-serif;
                        }

                        .actions {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 12px;
                        }

                        .btn {
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 44px;
                            padding: 0 18px;
                            border-radius: 999px;
                            text-decoration: none;
                            border: 1px solid rgba(71, 85, 105, 0.14);
                            background: white;
                            color: var(--ink);
                            font-weight: 700;
                            transition: transform .18s ease, border-color .18s ease, background-color .18s ease;
                        }

                        .btn:hover { transform: translateY(-1px); }

                        .btn.primary {
                            background: var(--ink);
                            border-color: var(--ink);
                            color: oklch(0.98 0.004 95);
                        }

                        @media (max-width: 960px) {
                            .shell,
                            .status-grid {
                                grid-template-columns: 1fr;
                            }

                            .shell {
                                min-height: auto;
                            }
                        }

                        @media (max-width: 720px) {
                            .hero,
                            .panel {
                                padding: 20px;
                                border-radius: 24px;
                            }

                            .btn {
                                width: 100%;
                            }
                        }
                    </style>
                </head>
                <body>
                <main class="shell">
                    <section class="hero">
                        <div class="eyebrow">IAM Sign Out</div>
                        <h1>统一身份会话已经退出</h1>
                        <p>IAM 的全局登录态已经回收。之后再次进入控制台或下游客户端时，浏览器会重新走统一登录与授权流程。</p>
                        <div class="status-grid">
                            <article class="status-card">
                                <span>步骤 1</span>
                                <strong>IAM 会话已销毁</strong>
                            </article>
                            <article class="status-card">
                                <span>步骤 2</span>
                                <strong>前端登出通知已发出</strong>
                            </article>
                            <article class="status-card">
                                <span>步骤 3</span>
                                <strong>等待重新进入系统</strong>
                            </article>
                        </div>
                    </section>
                    <section class="panel">
                        <div>
                            <h2>接下来可以做什么</h2>
                            <p>这个结果页只负责承接 IAM 控制台自己的退出链路，不会再错误地回到一个看起来像“还没退出完”的中间状态。</p>
                        </div>
                        <div class="tip">
                            <strong>重新登录 IAM</strong>
                            回到统一登录入口，重新演示账号密码、MFA 和目标页面恢复流程。
                        </div>
                        <div class="tip">
                            <strong>进入 Demo App A</strong>
                            从首个接入方重新触发 OAuth2 授权码与 PKCE 登录链路。
                        </div>
                        <div class="tip">
                            <strong>进入 Demo App B</strong>
                            继续演示第二个客户端如何复用统一身份中心的登录结果。
                        </div>
                        <div class="actions">
                            <a class="btn primary" href="/login">重新登录 IAM</a>
                            <a class="btn" href="http://127.0.0.1:8081/">打开 Demo App A</a>
                            <a class="btn" href="http://127.0.0.1:8082/">打开 Demo App B</a>
                        </div>
                    </section>
                </main>
                </body>
                </html>
                """);
    }

    public String resolvePostLogoutRedirect(String requestedRedirect) {
        if (requestedRedirect != null && appSecurityProperties.getAllowedPostLogoutRedirects().contains(requestedRedirect)) {
            return requestedRedirect;
        }
        return "/logged-out";
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String toJsString(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                + "'";
    }
}
