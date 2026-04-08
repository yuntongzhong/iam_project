# 系统架构设计说明

## 模块划分

- `iam-server`
  - 认证中心：Spring Authorization Server，负责授权码、PKCE、OIDC、Token 签发
  - 用户目录：`User`、`Department`、`Role`、`Permission`、`AppClient`
  - 安全增强：TOTP、多次失败锁定、审计日志
  - 管理台：REST API + `admin.html`
- `demo-app-a` / `demo-app-b`
  - 标准 OAuth2 Client
  - 根路径展示当前登录用户 Claims

## 核心设计决策

1. 使用多模块单仓，保证答辩时一套命令可构建全部系统。
2. 使用 JPA 持久化 `AppClient`，再适配成 `RegisteredClientRepository`，避免客户端配置只停留在内存。
3. 使用 `Department -> Role` 继承链表达部门权限继承，用户权限 = 直属角色 + 部门及祖先部门角色。
4. 用户密码和客户端密钥全部走 `BCryptPasswordEncoder`。
5. MFA 放在用户名密码成功之后、最终授权之前，通过 Session 暂存待完成认证，保证 OAuth2 授权前必须完成 TOTP。
6. 审计日志通过 AOP 和认证事件双通道落库，兼顾后台操作和登录过程。

## 认证与 SSO 流程

1. Demo App 重定向到 IAM 的 `/oauth2/authorize`
2. 未登录时 IAM 进入表单登录
3. 密码通过后进入 TOTP 绑定/校验
4. MFA 完成后恢复登录态，继续原始授权请求
5. IAM 发放授权码，客户端换取 Token
6. 同浏览器访问另一 Demo App 时，IAM 会话仍有效，因此无需再次输入密码

## 数据模型

- `User`: 用户主体、状态、失败次数、TOTP 开关与密钥
- `Department`: 无限级树结构
- `Role` / `Permission`: 标准 RBAC
- `AppClient`: OAuth2 客户端注册信息
- `AuditLog`: 管理操作与认证过程审计
