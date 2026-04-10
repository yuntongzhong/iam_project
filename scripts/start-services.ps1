param(
    [string]$DbUrl = "jdbc:mysql://127.0.0.1:3306/iam_platform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
    [string]$DbUsername = "root",
    [string]$DbPassword = $(if ($env:IAM_DB_PASSWORD) { $env:IAM_DB_PASSWORD } else { "zyt@360728" }),
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

$paths = Get-PlatformPaths
Ensure-RuntimeDirs

function Ensure-BuildArtifacts {
    if ($SkipBuild) {
        return
    }

    $apps = Get-AppConfig
    $buildRequired = $false

    foreach ($app in $apps) {
        if (-not (Test-Path $app.JarPath)) {
            $buildRequired = $true
            break
        }

        $jarTime = (Get-Item $app.JarPath).LastWriteTime
        $projectDir = Split-Path (Split-Path $app.JarPath -Parent) -Parent
        $sourceCandidates = @(
            (Join-Path $projectDir "src"),
            (Join-Path $projectDir "build.gradle")
        ) | Where-Object { Test-Path $_ }

        foreach ($candidate in $sourceCandidates) {
            $newerInput = Get-ChildItem -Path $candidate -Recurse -File |
                Where-Object { $_.LastWriteTime -gt $jarTime } |
                Select-Object -First 1
            if ($newerInput) {
                $buildRequired = $true
                break
            }
        }

        if ($buildRequired) {
            break
        }
    }

    if ($buildRequired) {
        Write-Host "Jar files are missing or outdated. Running .\gradlew.bat build ..."
        Push-Location $paths.RepoRoot
        try {
            & .\gradlew.bat build
        } finally {
            Pop-Location
        }
    }
}

function Start-App {
    param($App)

    if (Test-PortListening -Port $App.Port) {
        Update-PidFileFromPort -App $App
        Write-Host "$($App.Name) is already listening on port $($App.Port). Skipping."
        return
    }

    if (-not (Test-Path $App.JarPath)) {
        throw "Jar not found: $($App.JarPath)"
    }

    Remove-Item $App.LogFile -Force -ErrorAction SilentlyContinue

    $args = @("-jar", $App.JarPath)
    $args += "--logging.file.name=$($App.LogFile)"
    if ($App.Name -eq "iam-server") {
        $args += "--spring.datasource.url=$DbUrl"
        $args += "--spring.datasource.username=$DbUsername"
        $args += "--spring.datasource.password=$DbPassword"
    }
    $args += $App.ExtraArgs

    $process = Start-Process -FilePath "java" `
        -ArgumentList $args `
        -WorkingDirectory $paths.RepoRoot `
        -PassThru

    Write-PidFile -AppName $App.Name -ProcessId $process.Id
    Write-Host "Started $($App.Name), PID=$($process.Id)"
}

Ensure-BuildArtifacts

foreach ($app in Get-AppConfig) {
    Start-App -App $app
}

Start-Sleep -Seconds 3

foreach ($app in Get-AppConfig) {
    $ready = $false
    for ($i = 0; $i -lt 20; $i++) {
        if (Test-PortListening -Port $app.Port) {
            $ready = $true
            break
        }
        Start-Sleep -Milliseconds 500
    }

    if ($ready) {
        Update-PidFileFromPort -App $app
        $status = "RUNNING"
    } else {
        $status = "NOT_READY"
    }
    Write-Host ("- {0}: {1} (http://127.0.0.1:{2})" -f $app.Name, $status, $app.Port)
}

Write-Host ""
Write-Host "Detailed status:"
Show-ServiceStatus -Detailed
