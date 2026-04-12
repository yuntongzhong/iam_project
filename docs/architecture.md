# 系统架构设计说明

## 模块划分

- `iam-server`
  - 认证中心：Spring Authorization Server，负责授权码、PKCE、OIDC、Token 签发
  - 用户目录：`User`、`Department`、`Role`、`Permission`、`AppClient`
  - 安全增强：TOTP、多次失败锁定、审计日志、JWT 签名密钥持久化
  - 协议状态：OAuth2 授权记录与同意记录落 MySQL，重启后不丢失
  - 管理台：REST API + `admin.html`
- `demo-app-a` / `demo-app-b`
  - 标准 OAuth2 Client
  - 根路径展示当前登录用户 Claims 与过期时间

## 关键能力补齐

1. 用户目录支持手工录入与 CSV 批量导入，导入前会先做逐行预检，输出错误和警告。
2. 客户端注册不只维护回调地址和 PKCE，还显式维护授权码、Access Token、Refresh Token 的 TTL 与 Refresh Token 轮换策略。
3. 登录审计补齐“谁 / 何时 / 从哪 / 登录什么”四要素，可识别 IAM 控制台、`app-a`、`app-b` 三类目标系统。
4. 提交阶段提供一键打包脚本，把 Jar、源码、文档、截图目录和演示视频目录组装成统一答辩包。

## 核心设计决策

1. 使用多模块单仓，保证答辩时一套命令可构建全部系统。
2. 使用 JPA 持久化 `AppClient`，再适配成 `RegisteredClientRepository`，避免客户端配置只停留在内存。
3. 使用 `Department -> Role` 继承链表达部门权限继承，用户权限 = 直属角色 + 部门及祖先部门角色。
4. 用户密码和客户端密钥全部走 `BCryptPasswordEncoder`。
5. MFA 放在用户名密码成功之后、最终授权之前，通过 Session 暂存待完成认证，保证 OAuth2 授权前必须完成 TOTP。
6. 审计日志通过 AOP 和认证事件双通道落库，兼顾后台操作和登录过程。
7. JWT 签名密钥优先读取外部路径，默认落到 `.runtime/keys/iam-signing.jwk.json`，避免每次重启都更换签名公钥。
8. `oauth2_authorization` 与 `oauth2_authorization_consent` 表在启动时自动初始化，确保授权状态具备持久化能力。

## 认证与 SSO 流程

1. Demo App 重定向到 IAM 的 `/oauth2/authorize`
2. 未登录时 IAM 进入表单登录
3. 密码通过后进入 TOTP 绑定/校验
4. MFA 完成后恢复登录态，继续原始授权请求
5. IAM 发放授权码，客户端换取 Token
6. 同浏览器访问另一 Demo App 时，IAM 会话仍有效，因此无需再次输入密码

## 令牌与协议策略

- 授权模式：`authorization_code + refresh_token`
- PKCE：默认强制开启
- Token 策略：客户端级可配置
  - 授权码默认 5 分钟
  - Access Token 默认 30 分钟
  - Refresh Token 默认 480 分钟
  - Refresh Token 默认轮换，不复用旧值

## 数据模型

- `User`: 用户主体、状态、失败次数、TOTP 开关与密钥
- `Department`: 无限级树结构
- `Role` / `Permission`: 标准 RBAC
- `AppClient`: OAuth2 客户端注册信息
- `AuditLog`: 管理操作与认证过程审计，附带目标系统、IP、浏览器信息
