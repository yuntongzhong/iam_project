# 评分项映射说明

## 1. 认证协议规范性（30 分）

- OAuth2 授权码模式：`/oauth2/authorize` + Demo App 标准客户端接入
- OIDC：客户端使用 `openid,profile`，IAM 发放 Claims
- PKCE：应用客户端默认强制开启
- Token 过期策略：客户端级可配置授权码、Access Token、Refresh Token TTL
- 授权状态持久化：`oauth2_authorization`、`oauth2_authorization_consent` 落 MySQL
- 密钥稳定性：JWT 签名密钥文件化持久化，避免重启后公钥漂移

## 2. 权限模型完整性（25 分）

- 用户、部门、角色、权限四层结构完整
- 权限分为功能权限与应用访问权限
- 用户最终权限 = 直属角色 + 所属部门及祖先部门继承角色
- IAM 控制台展示权限词典、Authority 映射和最终生效权限
- 用户支持手工维护与 CSV 批量导入

## 3. SSO 可用性（25 分）

- App A 登录成功后，同浏览器访问 App B 可直接复用 IAM 会话
- 首次 MFA 绑定后可恢复原始目标页面
- 统一退出链路会回收 IAM 与两个 Demo App 的本地会话
- Demo App 页面展示当前用户、Claims 与 `expiresAt`

## 4. 系统工程质量（20 分）

- 用户密码、客户端密钥统一使用 `BCrypt`
- MFA 使用 TOTP，与 Google Authenticator 兼容
- 连续密码错误 5 次自动锁定
- 审计日志记录用户、时间、IP、浏览器、目标系统与脱敏详情
- 构建、启动、打包、提交均提供一键脚本
- 核心风险点具备自动化测试：导入、TTL、审计目标、JWK 复用
