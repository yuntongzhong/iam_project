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
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Lexend:wght@400;500;600;700&family=Source+Sans+3:wght@400;500;600;700&display=swap" rel="stylesheet">
                    <style>
                        :root {
                            --bg: oklch(0.986 0.004 240);
                            --surface: oklch(0.998 0.002 240 / 0.96);
                            --line: oklch(0.892 0.014 234);
                            --ink: oklch(0.2 0.03 255);
                            --ink-soft: oklch(0.45 0.02 245);
                            --danger: oklch(0.58 0.17 28);
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
                        .card {
                            width: min(760px, 100%);
                            padding: 28px;
                            border-radius: 30px;
                            background: var(--surface);
                            border: 1px solid rgba(71, 85, 105, 0.12);
                            box-shadow: var(--shadow);
                        }
                        .eyebrow {
                            display: inline-flex;
                            padding: 8px 14px;
                            border-radius: 999px;
                            background: color-mix(in oklch, var(--danger) 10%, white);
                            color: color-mix(in oklch, var(--danger) 88%, black);
                            font: 700 12px/1 "Lexend", sans-serif;
                            letter-spacing: .14em;
                            text-transform: uppercase;
                        }
                        h1 {
                            margin: 18px 0 10px;
                            font: 700 clamp(2rem, 4vw, 3.4rem)/1.03 "Lexend", sans-serif;
                        }
                        p {
                            margin: 0;
                            color: var(--ink-soft);
                            line-height: 1.72;
                            font-size: 1rem;
                        }
                        .tips {
                            display: grid;
                            gap: 12px;
                            margin-top: 20px;
                            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                        }
                        .tip {
                            padding: 14px;
                            border-radius: 18px;
                            background: color-mix(in oklch, var(--amber) 10%, white);
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
                            margin-top: 22px;
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
                        }
                    </style>
                </head>
                <body>
                <main class="card">
                    <div class="eyebrow">403 Forbidden</div>
                    <h1>当前账号没有进入 IAM 控制台的权限</h1>
                    <p>系统已拒绝这次访问请求。这里不会展示任何底层异常或堆栈信息；如果你需要进入控制台，请联系管理员为当前账号分配对应的后台查看权限。</p>
                    <section class="tips">
                        <article class="tip">
                            <strong>你仍然已登录</strong>
                            <span>这表示认证成功了，但当前身份不具备控制台访问授权。</span>
                        </article>
                        <article class="tip">
                            <strong>可能缺少权限</strong>
                            <span>常见为用户、部门、角色、应用或审计读取权限未被授予。</span>
                        </article>
                        <article class="tip">
                            <strong>可返回业务应用</strong>
                            <span>如果你只是访问客户端业务页面，不一定需要进入 IAM 控制台。</span>
                        </article>
                    </section>
                    <div class="actions">
                        <a class="btn" href="http://127.0.0.1:8081/">打开 Demo App A</a>
                        <a class="btn" href="http://127.0.0.1:8082/">打开 Demo App B</a>
                        <a class="btn" href="/logout">退出登录</a>
                    </div>
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
