param(
    [string]$DbUsername = "root",
    [string]$DbPassword = $(if ($env:IAM_DB_PASSWORD) { $env:IAM_DB_PASSWORD } else { "zyt@360728" }),
    [string]$DatabaseName = "iam_platform",
    [string]$MysqlExePath = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $MysqlExePath)) {
    throw "mysql.exe not found: $MysqlExePath"
}

$sql = @'
UPDATE users
SET password = '$2a$10$wC2RDXuhnQZMxO2QuLu5h.xLfi1QUlrdHPyHz71lQ98csMD2kTakG',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    is_totp_enabled = 0,
    totp_secret = NULL
WHERE username = 'admin';

UPDATE users
SET password = '$2a$10$2rntC79ADCNlP6sQcVeyyOBHp5b/eU5DthoH1tqXr83BWYbA9b5Oq',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    is_totp_enabled = 0,
    totp_secret = NULL
WHERE username = 'alice';

UPDATE users
SET password = '$2a$10$8Q9FEE0rhQ4COUa/DKVGheZ18bfjxnZom99yoHTjjDfm.fzRKTlxe',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    is_totp_enabled = 0,
    totp_secret = NULL
WHERE username = 'bob';

SELECT username, status, failed_login_attempts, is_totp_enabled
FROM users
WHERE username IN ('admin', 'alice', 'bob')
ORDER BY username;
'@

& $MysqlExePath "-u$DbUsername" "-p$DbPassword" "-D" $DatabaseName "-e" $sql

Write-Host ""
Write-Host "Demo accounts restored:"
Write-Host "- admin / Admin#2026!Secure"
Write-Host "- alice / Alice#2026!Secure"
Write-Host "- bob / Bob#2026!Audit"
