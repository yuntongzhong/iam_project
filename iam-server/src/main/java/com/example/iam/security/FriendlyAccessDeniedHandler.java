package com.example.iam.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FriendlyAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");

        if (isApiRequest(request)) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"message":"当前账号没有访问该资源的权限。"}
                    """);
            return;
        }

        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.getWriter().write("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 无权限</title>
                    <meta name="description" content="IAM 无权限提示页，用于向当前用户展示访问被拒绝的原因与后续处理建议。">
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
                            --danger: oklch(0.58 0.17 28);
                            --danger-deep: oklch(0.38 0.11 26);
                            --amber: oklch(0.72 0.13 52);
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
                                radial-gradient(circle at 10% 10%, oklch(0.94 0.05 250 / 0.65), transparent 24%),
                                radial-gradient(circle at 88% 12%, oklch(0.93 0.08 52 / 0.2), transparent 18%),
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
                            grid-template-columns: minmax(0, 1.04fr) minmax(320px, 0.96fr);
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
                            background: linear-gradient(135deg, var(--danger-deep), var(--danger) 62%, var(--amber) 100%);
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
                            font: 700 clamp(2rem, 4vw, 3.4rem)/1.04 "Lexend", sans-serif;
                        }

                        .hero p {
                            margin: 0;
                            max-width: 48ch;
                            color: rgba(241, 246, 255, .84);
                            line-height: 1.7;
                        }

                        .signal-grid {
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 10px;
                        }

                        .signal-card,
                        .tip {
                            padding: 14px;
                            border-radius: 20px;
                            background: rgba(248, 251, 255, 0.1);
                            border: 1px solid rgba(248, 251, 255, 0.16);
                        }

                        .signal-card span {
                            display: block;
                            font-size: 12px;
                            letter-spacing: .08em;
                            text-transform: uppercase;
                            color: rgba(239, 245, 255, 0.66);
                        }

                        .signal-card strong {
                            display: block;
                            margin-top: 8px;
                            font: 700 0.98rem/1.35 "Lexend", sans-serif;
                        }

                        .panel {
                            padding: 24px;
                            background: var(--surface);
                            border: 1px solid rgba(71, 85, 105, 0.12);
                            display: grid;
                            gap: 16px;
                            align-content: start;
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

                        .tips {
                            display: grid;
                            gap: 12px;
                        }

                        .tip {
                            background: color-mix(in oklch, var(--surface-strong) 90%, white);
                            border: 1px solid rgba(71, 85, 105, 0.08);
                        }

                        .tip strong {
                            display: block;
                            margin-bottom: 6px;
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
                            .signal-grid {
                                grid-template-columns: 1fr;
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
                        <div class="eyebrow">403 Forbidden</div>
                        <h1>当前账号没有进入 IAM 控制台的权限</h1>
                        <p>系统已拒绝这次访问请求。这里不会展示任何底层异常或堆栈信息；如果你需要进入控制台，请联系管理员为当前账号分配对应的后台查看权限。</p>
                        <div class="signal-grid">
                            <article class="signal-card">
                                <span>当前状态</span>
                                <strong>你仍然处于已登录状态</strong>
                            </article>
                            <article class="signal-card">
                                <span>常见原因</span>
                                <strong>缺少后台读取权限或对应角色</strong>
                            </article>
                            <article class="signal-card">
                                <span>安全策略</span>
                                <strong>异常细节不会在页面暴露</strong>
                            </article>
                        </div>
                    </section>
                    <section class="panel">
                        <div>
                            <h2>你现在可以怎么做</h2>
                            <p>如果你只是访问业务客户端，不一定需要进入 IAM 控制台；如果你确实需要后台治理能力，请让管理员为当前账号分配合适的角色与权限。</p>
                        </div>
                        <section class="tips">
                            <article class="tip">
                                <strong>返回业务应用</strong>
                                <span>继续访问 Demo App A 或 Demo App B，不影响普通业务演示。</span>
                            </article>
                            <article class="tip">
                                <strong>联系管理员</strong>
                                <span>常见需要补充的能力包括用户、部门、角色、应用或审计读取权限。</span>
                            </article>
                            <article class="tip">
                                <strong>如需切换账号</strong>
                                <span>可以先退出当前登录，再用具备后台权限的账号重新进入控制台。</span>
                            </article>
                        </section>
                        <div class="actions">
                            <a class="btn primary" href="http://127.0.0.1:8081/">打开 Demo App A</a>
                            <a class="btn" href="http://127.0.0.1:8082/">打开 Demo App B</a>
                            <a class="btn" href="/logout">退出登录</a>
                        </div>
                    </section>
                </main>
                </body>
                </html>
                """);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/") || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }
}
