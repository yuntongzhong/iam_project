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
                    <style>
                        body { margin: 0; min-height: 100vh; display: grid; place-items: center; font-family: "Segoe UI", "Microsoft YaHei", sans-serif; background: linear-gradient(135deg, #eff6ff, #f8fafc); color: #0f172a; }
                        .card { width: min(560px, calc(100vw - 32px)); background: #fff; border-radius: 28px; padding: 32px; box-shadow: 0 24px 56px rgba(15, 23, 42, .12); }
                        .hint { margin-top: 12px; color: #475569; line-height: 1.7; }
                        .spinner { width: 44px; height: 44px; border-radius: 999px; border: 4px solid #dbeafe; border-top-color: #2563eb; animation: spin 1s linear infinite; margin-bottom: 20px; }
                        .fallback { margin-top: 20px; }
                        a { color: #1d4ed8; text-decoration: none; font-weight: 700; }
                        iframe { display: none; width: 0; height: 0; border: 0; }
                        @keyframes spin { to { transform: rotate(360deg); } }
                    </style>
                </head>
                <body>
                <div class="card">
                    <div class="spinner"></div>
                    <div style="font-size:12px; letter-spacing:.08em; color:#475569; font-weight:700;">IAM SINGLE LOGOUT</div>
                    <h1 style="margin:10px 0 8px;">正在清理所有客户端会话</h1>
                    <p class="hint">IAM 已销毁全局登录态，当前页面正在通知 Demo App A 和 Demo App B 清理各自的本地 Session。完成后会自动跳转到退出完成页。</p>
                    <div class="fallback">如果页面没有自动跳转，请点击 <a id="continueLink" href="%s">继续</a>。</div>
                </div>
                """.formatted(escapeHtml(finalRedirect)));

        for (String logoutUri : logoutUris) {
            html.append("<iframe src=\"")
                    .append(escapeHtml(logoutUri))
                    .append("\" loading=\"eager\"></iframe>");
        }

        html.append("""
                <script>
                window.setTimeout(function () {
                    window.location.replace(%s);
                }, 900);
                </script>
                </body>
                </html>
                """.formatted(toJsString(finalRedirect)));

        return org.springframework.http.ResponseEntity.ok(html.toString());
    }

    public String resolvePostLogoutRedirect(String requestedRedirect) {
        if (requestedRedirect != null && appSecurityProperties.getAllowedPostLogoutRedirects().contains(requestedRedirect)) {
            return requestedRedirect;
        }
        return "/login?logout";
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
