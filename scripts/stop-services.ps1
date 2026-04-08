param()

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

$paths = Get-PlatformPaths
Ensure-RuntimeDirs

function Stop-ByPidFile {
    param($App)

    $pidFile = Get-PidFilePath -AppName $App.Name
    if (-not (Test-Path $pidFile)) {
        return
    }

    $pidValue = Get-Content $pidFile | Select-Object -First 1
    if ($pidValue -match '^[1-9]\d*$') {
        $process = Get-Process -Id ([int]$pidValue) -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $process.Id -Force
            Write-Host "Stopped $($App.Name), PID=$($process.Id)"
        }
    }

    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

function Stop-ByPort {
    param($App)

    $connections = Get-NetTCPConnection -LocalPort $App.Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        if ($connection.OwningProcess -le 0) {
            continue
        }
        $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $process.Id -Force
            Write-Host "Stopped $($App.Name) via port $($App.Port), PID=$($process.Id)"
        }
    }
}

function Remove-StalePidFile {
    param($App)

    if (-not (Test-PortListening -Port $App.Port)) {
        Remove-Item (Get-PidFilePath -AppName $App.Name) -Force -ErrorAction SilentlyContinue
    }
}

foreach ($app in Get-AppConfig) {
    Stop-ByPidFile -App $app
    Start-Sleep -Milliseconds 300
    Stop-ByPort -App $app
    Start-Sleep -Milliseconds 300
    Remove-StalePidFile -App $app
}

Write-Host ""
Write-Host "All services stopped."
Write-Host "Logs are kept in $($paths.LogsDir)"
Show-ServiceStatus -Detailed
