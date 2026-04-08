# AI 辅助编程说明

## 使用方式

- 先从赛题原文抽取明确功能边界和评分项
- 将需求拆为工程初始化、数据建模、协议实现、安全增强、管理台、Demo 客户端、文档七个阶段
- 对 OAuth2 / PKCE / Spring Authorization Server 的关键实现细节先做核对，再写代码
- 先搭建可编译骨架，再通过 `gradlew test` 反向驱动修正库 API 和配置问题

## AI 贡献点

- 生成多模块 Gradle 结构与 Spring Boot 配置
- 生成实体类、Repository、Service、Controller、Security 配置
- 生成 TOTP、异常登录检测、审计脱敏、管理台页面
- 生成 Demo 客户端与答辩文档草案

## 人工决策点

- 明确用 `Department -> Role` 表达继承，而不是在 SQL 层硬编码权限展开
- 确定客户端采用显式端点配置，避免测试阶段依赖 OIDC 发现
- 保持外部 MySQL 8 为正式运行方案，同时用 H2 支撑测试
