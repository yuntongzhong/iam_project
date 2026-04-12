# 提交清单

## 必交内容

- 可运行 Jar
  - `iam-server`
  - `demo-app-a`
  - `demo-app-b`
- 源码快照
- 系统架构设计说明
- 答辩演示脚本
- 评分项映射说明
- AI 辅助编程说明
- 截图目录
- 演示视频
- 运行说明

## 推荐截图文件名

- `01-console-overview.png`
- `02-import-preview.png`
- `03-import-commit.png`
- `04-totp-setup.png`
- `05-app-a-success.png`
- `06-app-b-sso.png`
- `07-login-locked.png`
- `08-audit-targets.png`

## 视频要求

- 文件名：`demo-walkthrough.mp4`
- 内容建议：
  - 控制台总览
  - CSV 导入
  - App A 登录 + TOTP
  - App B SSO
  - 异常登录锁定
  - 审计日志筛选
  - 单点退出

## 打包前确认

1. `scripts\package.cmd` 已通过
2. `scripts\start-services.cmd` 可完整启动
3. `scripts\package-submission.cmd -AuthorName <姓名>` 可成功生成 zip
4. 演示视频已经替换占位文件
