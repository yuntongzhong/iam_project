# IAM Platform Demo

多模块仓库：

- `iam-server`: OAuth2 Authorization Server、RBAC、MFA、审计日志、管理控制台
- `demo-app-a`: 端口 `8081` 的 OAuth2 客户端
- `demo-app-b`: 端口 `8082` 的 OAuth2 客户端

## 环境要求

- Java 17
- MySQL 8
- Gradle Wrapper
- PowerShell 7（Win11 推荐使用 `pwsh`）

## Win11 快速启动

1. 确认 MySQL 服务已启动，并已创建数据库 `iam_platform`
2. 在 Win11 下直接运行一键脚本：
   - 打包: [package.cmd](/D:/codex/iam_project/scripts/package.cmd)
   - 提交打包: [package-submission.cmd](/D:/codex/iam_project/scripts/package-submission.cmd)
   - 启动: [start-services.cmd](/D:/codex/iam_project/scripts/start-services.cmd)
   - 状态: [status-services.cmd](/D:/codex/iam_project/scripts/status-services.cmd)
   - 恢复演示账号: [reset-demo-accounts.cmd](/D:/codex/iam_project/scripts/reset-demo-accounts.cmd)
   - 关闭: [stop-services.cmd](/D:/codex/iam_project/scripts/stop-services.cmd)
3. 或者在 PowerShell 7 中执行：

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\package.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-submission.ps1 -AuthorName <姓名>
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-services.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\status-services.ps1 -Detailed
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\reset-demo-accounts.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop-services.ps1
```

4. 启动完成后访问：
   - IAM 管理台: [http://127.0.0.1:8080/admin.html](http://127.0.0.1:8080/admin.html)
   - Demo A: [http://127.0.0.1:8081](http://127.0.0.1:8081)
   - Demo B: [http://127.0.0.1:8082](http://127.0.0.1:8082)

## 数据库连接参数

```powershell
$env:IAM_DB_URL="jdbc:mysql://127.0.0.1:3306/iam_platform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
$env:IAM_DB_USERNAME="root"
$env:IAM_DB_PASSWORD="zyt@360728"
```

说明：

- 启动脚本默认连接 `root / zyt@360728 / iam_platform`
- 如需覆盖默认值，可设置 `IAM_DB_PASSWORD`，或在 `start-services.ps1` 调用时传入 `-DbPassword`
- `.cmd` 包装器会优先调用 `pwsh`，找不到时再回退到 `powershell`
- `.cmd` 包装器会透传参数，可直接执行 `status-services.cmd -Detailed`
- `reset-demo-accounts` 会把 `admin`、`alice`、`bob` 恢复到默认密码、`ACTIVE` 状态、失败次数清零，并清空 TOTP 绑定
- 如 Jar 不存在，会自动执行 `.\gradlew.bat build`
- 也可以直接执行 `scripts\package.cmd` 一键重新打包
- 也可以直接执行 `scripts\package-submission.cmd -AuthorName <姓名>` 生成最终提交 zip
- 运行日志输出到 [/.runtime/logs](/D:/codex/iam_project/.runtime/logs)，每个服务一个 `.log`
- PID 文件输出到 [/.runtime/pids](/D:/codex/iam_project/.runtime/pids)，记录的是实际监听端口的 Java 进程 PID

## 手动启动（排障备用）

如果你要单独调试某个服务，可在 PowerShell 7 中先设置数据库环境变量，再分别执行：

```powershell
.\gradlew :iam-server:bootRun
.\gradlew :demo-app-a:bootRun
.\gradlew :demo-app-b:bootRun
```

## 默认演示账号

- `admin / Admin#2026!Secure`
- `alice / Alice#2026!Secure`
- `bob / Bob#2026!Audit`

## 默认客户端密钥

- `app-a / AppA#2026!ClientSecret`
- `app-b / AppB#2026!ClientSecret`

首次登录会进入 TOTP 绑定页，后续登录需要输入动态码。

## 构建与测试

```powershell
.\gradlew test
.\gradlew build
```

## 提交答辩包

- 评分映射文档： [scoring-map.md](/D:/codex/iam_project/docs/scoring-map.md)
- 提交清单： [submission-checklist.md](/D:/codex/iam_project/docs/submission-checklist.md)
- 截图目录： [submission-assets/screenshots/README.md](/D:/codex/iam_project/submission-assets/screenshots/README.md)
- 视频目录： [submission-assets/video/README.md](/D:/codex/iam_project/submission-assets/video/README.md)
- 一键打包命令：

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-submission.ps1 -AuthorName 张三
```

## 评分项映射

- 认证协议规范性：`spring-boot-starter-oauth2-authorization-server` + 授权码模式 + PKCE
- 权限模型完整性：用户/角色/权限/部门树/部门继承角色
- SSO 可用性：同浏览器访问 Demo A 后再访问 Demo B 复用 IAM 登录会话
- 系统工程质量：BCrypt、TOTP、多次失败锁定、审计脱敏、基础测试与文档
