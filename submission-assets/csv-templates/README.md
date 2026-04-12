# CSV 导入演示模板

- 成功演示文件：`01-users-import-success.csv`
- 错误演示文件：`02-users-import-errors.csv`

使用建议：

- 先上传 `02-users-import-errors.csv`，演示预检逐行报错与“整批不落库”。
- 再上传 `01-users-import-success.csv`，演示预检通过、确认导入、审计留痕。
- 两个文件都采用系统要求的 UTF-8 CSV 表头：
  `username,email,phone,departmentPath,roleCodes,status,password`

注意：

- 成功文件里的用户名、邮箱、手机号是演示专用的新值，当前项目环境可直接导入。
- 如果你在同一套数据库里已经导入过一次成功文件，再次导入会走 `username` 的更新逻辑，不会再是“新增用户”演示。
