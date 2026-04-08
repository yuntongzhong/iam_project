Set-StrictMode -Version Latest

$script:RepoRoot = Split-Path -Parent $PSScriptRoot
$script:RuntimeDir = Join-Path $script:RepoRoot ".runtime"
$script:LogsDir = Join-Path $script:RuntimeDir "logs"
$script:PidsDir = Join-Path $script:RuntimeDir "pids"

function Get-PlatformPaths {
    [pscustomobject]@{
        RepoRoot = $script:RepoRoot
        RuntimeDir = $script:RuntimeDir
        LogsDir = $script:LogsDir
        PidsDir = $script:PidsDir
    }
}

function Ensure-RuntimeDirs {
    New-Item -ItemType Directory -Path $script:LogsDir -Force | Out-Null
    New-Item -ItemType Directory -Path $script:PidsDir -Force | Out-Null
}

function Get-AppConfig {
    @(
        @{
            Name = "iam-server"
            Port = 8080
            JarPath = Join-Path $script:RepoRoot "iam-server\build\libs\iam-server-0.0.1-SNAPSHOT.jar"
            LogFile = Join-Path $script:LogsDir "iam-server.log"
            ExtraArgs = @()
        },
        @{
            Name = "demo-app-a"
            Port = 8081
            JarPath = Join-Path $script:RepoRoot "demo-app-a\build\libs\demo-app-a-0.0.1-SNAPSHOT.jar"
            LogFile = Join-Path $script:LogsDir "demo-app-a.log"
            ExtraArgs = @()
        },
        @{
            Name = "demo-app-b"
            Port = 8082
            JarPath = Join-Path $script:RepoRoot "demo-app-b\build\libs\demo-app-b-0.0.1-SNAPSHOT.jar"
            LogFile = Join-Path $script:LogsDir "demo-app-b.log"
            ExtraArgs = @()
        }
    )
}

function Get-PidFilePath {
    param([string]$AppName)

    Join-Path $script:PidsDir "$AppName.pid"
}

function Test-PortListening {
    param([int]$Port)

    $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Get-ListeningPid {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($null -eq $connection) {
        return $null
    }

    return $connection.OwningProcess
}

function Write-PidFile {
    param(
        [string]$AppName,
        [int]$ProcessId
    )

    if ($ProcessId -le 0) {
        return
    }

    $pidFile = Get-PidFilePath -AppName $AppName
    Set-Content -Path $pidFile -Value $ProcessId -Encoding ascii
}

function Update-PidFileFromPort {
    param($App)

    $listeningPid = Get-ListeningPid -Port $App.Port
    if ($null -ne $listeningPid -and $listeningPid -gt 0) {
        Write-PidFile -AppName $App.Name -ProcessId $listeningPid
    }
}

function Get-ServiceStatus {
    param($App)

    $listeningPid = Get-ListeningPid -Port $App.Port
    $pidFile = Get-PidFilePath -AppName $App.Name
    $pidFileValue = $null

    if (Test-Path $pidFile) {
        $rawValue = Get-Content $pidFile | Select-Object -First 1
        if ($rawValue -match '^[1-9]\d*$') {
            $pidFileValue = $rawValue
        }
    }

    [pscustomobject]@{
        Name = $App.Name
        Port = $App.Port
        Status = if ($null -ne $listeningPid) { "RUNNING" } else { "STOPPED" }
        ListeningPid = $listeningPid
        PidFileValue = $pidFileValue
        Url = "http://127.0.0.1:$($App.Port)"
        LogFile = $App.LogFile
    }
}

function Show-ServiceStatus {
    param([switch]$Detailed)

    Write-Host ""
    Write-Host "Service status:"

    foreach ($app in Get-AppConfig) {
        $status = Get-ServiceStatus -App $app
        if ($Detailed) {
            Write-Host ("- {0}: {1} (port={2}, pid={3}, pidFile={4})" -f
                $status.Name,
                $status.Status,
                $status.Port,
                ($(if ($null -ne $status.ListeningPid) { $status.ListeningPid } else { "-" })),
                ($(if ($null -ne $status.PidFileValue) { $status.PidFileValue } else { "-" })))
        } else {
            Write-Host ("- {0}: {1} ({2})" -f $status.Name, $status.Status, $status.Url)
        }
    }

    $paths = Get-PlatformPaths
    Write-Host ""
    Write-Host "Logs: $($paths.LogsDir)"
    Write-Host "PIDs: $($paths.PidsDir)"
}
