Set-StrictMode -Version Latest

$script:RepoRoot = Split-Path -Parent $PSScriptRoot
$script:RuntimeDir = Join-Path $script:RepoRoot ".runtime"
$script:LogsDir = Join-Path $script:RuntimeDir "logs"
$script:PidsDir = Join-Path $script:RuntimeDir "pids"
$script:ManagedMySqlPidFile = Join-Path $script:PidsDir "mysql-local.pid"
$script:ManagedMySqlLogFile = Join-Path $script:LogsDir "mysql-local.log"
$script:ManagedMySqlErrorLogFile = Join-Path $script:LogsDir "mysql-local.err.log"

function Get-PlatformPaths {
    [pscustomobject]@{
        RepoRoot = $script:RepoRoot
        RuntimeDir = $script:RuntimeDir
        LogsDir = $script:LogsDir
        PidsDir = $script:PidsDir
    }
}

function Get-JavaExecutable {
    if ($env:JAVA_HOME) {
        $javaFromHome = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $javaFromHome) {
            return $javaFromHome
        }
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        return $javaCommand.Source
    }

    return $null
}

function Get-JavaMajorVersion {
    param([Parameter(Mandatory)][string]$JavaExecutable)

    $escapedJavaExecutable = $JavaExecutable.Replace('"', '""')
    $versionOutput = cmd /d /c """$escapedJavaExecutable"" -version 2>&1" | Select-Object -First 1

    if (-not $versionOutput) {
        throw "Failed to query Java version from $JavaExecutable"
    }

    $versionText = [string]$versionOutput
    if ($versionText -match '"(?<version>\d+(?:\.\d+)*)') {
        $rawVersion = $Matches.version
        if ($rawVersion.StartsWith("1.")) {
            return [int]($rawVersion.Split(".")[1])
        }

        return [int]($rawVersion.Split(".")[0])
    }

    throw "Unable to parse Java version output: $versionText"
}

function Initialize-JavaEnvironment {
    param([int]$MinimumMajorVersion = 17)

    $javaExecutable = Get-JavaExecutable
    if (-not $javaExecutable) {
        throw "Java $MinimumMajorVersion or later is required. Install a JDK and ensure JAVA_HOME or PATH points to it."
    }

    $javaMajorVersion = Get-JavaMajorVersion -JavaExecutable $javaExecutable
    if ($javaMajorVersion -lt $MinimumMajorVersion) {
        throw "Java $MinimumMajorVersion or later is required, but found Java $javaMajorVersion at $javaExecutable"
    }

    $javaHome = Split-Path (Split-Path $javaExecutable -Parent) -Parent
    $javaBinDir = Join-Path $javaHome "bin"
    $pathEntries = @($env:PATH -split ';') | Where-Object { $_ }

    $env:JAVA_HOME = $javaHome
    if ($pathEntries -notcontains $javaBinDir) {
        $env:PATH = "$javaBinDir;$env:PATH"
    }

    [pscustomobject]@{
        JavaExecutable = $javaExecutable
        JavaHome = $javaHome
        JavaMajorVersion = $javaMajorVersion
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

function Get-ManagedMySqlPidFilePath {
    return $script:ManagedMySqlPidFile
}

function Get-ManagedMySqlLogFilePath {
    return $script:ManagedMySqlLogFile
}

function Get-ManagedMySqlErrorLogFilePath {
    return $script:ManagedMySqlErrorLogFile
}

function Get-MySqlEndpoint {
    param([Parameter(Mandatory)][string]$DbUrl)

    if ($DbUrl -match '^jdbc:mysql://(?<host>[^:/?]+)(?::(?<port>\d+))?/') {
        return [pscustomobject]@{
            Host = $Matches.host
            Port = if ($Matches.port) { [int]$Matches.port } else { 3306 }
        }
    }

    return $null
}

function Test-MySqlReachable {
    param([string]$DbHost = "127.0.0.1", [int]$Port = 3306)

    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $asyncResult = $client.BeginConnect($DbHost, $Port, $null, $null)
        $connected = $asyncResult.AsyncWaitHandle.WaitOne(1500, $false)
        if (-not $connected) {
            $client.Close()
            return $false
        }

        $client.EndConnect($asyncResult)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

function Find-MySqlService {
    Get-Service -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match 'mysql' -or $_.DisplayName -match 'mysql' } |
        Sort-Object Name |
        Select-Object -First 1
}

function Find-LocalMySqlInstall {
    $candidatePairs = @()

    if ($env:MYSQL_HOME) {
        $candidatePairs += [pscustomobject]@{
            MysqldPath = Join-Path $env:MYSQL_HOME "bin\mysqld.exe"
            DefaultsFile = Join-Path $env:MYSQL_HOME "my.ini"
        }
    }

    $candidatePairs += [pscustomobject]@{
        MysqldPath = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe"
        DefaultsFile = "C:\ProgramData\MySQL\MySQL Server 8.4\my.ini"
    }

    foreach ($candidate in $candidatePairs) {
        if ((Test-Path $candidate.MysqldPath) -and (Test-Path $candidate.DefaultsFile)) {
            return $candidate
        }
    }

    $mysqld = Get-ChildItem "C:\Program Files\MySQL" -Recurse -Filter "mysqld.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName |
        Select-Object -First 1
    if (-not $mysqld) {
        return $null
    }

    $defaultsFile = Get-ChildItem "C:\ProgramData\MySQL" -Recurse -Filter "my.ini" -ErrorAction SilentlyContinue |
        Sort-Object FullName |
        Select-Object -First 1
    if (-not $defaultsFile) {
        return $null
    }

    return [pscustomobject]@{
        MysqldPath = $mysqld.FullName
        DefaultsFile = $defaultsFile.FullName
    }
}

function Start-ManagedMySqlProcess {
    param(
        [Parameter(Mandatory)][string]$MysqldPath,
        [Parameter(Mandatory)][string]$DefaultsFile
    )

    Remove-Item (Get-ManagedMySqlLogFilePath) -Force -ErrorAction SilentlyContinue
    Remove-Item (Get-ManagedMySqlErrorLogFilePath) -Force -ErrorAction SilentlyContinue

    $arguments = @(
        "--defaults-file=""$DefaultsFile""",
        "--console"
    )

    $process = Start-Process -FilePath $MysqldPath `
        -ArgumentList $arguments `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Get-ManagedMySqlLogFilePath) `
        -RedirectStandardError (Get-ManagedMySqlErrorLogFilePath) `
        -PassThru

    Set-Content -LiteralPath (Get-ManagedMySqlPidFilePath) -Value $process.Id -Encoding ascii
    return $process
}

function Ensure-MySqlReady {
    param([Parameter(Mandatory)][string]$DbUrl)

    $endpoint = Get-MySqlEndpoint -DbUrl $DbUrl
    if ($null -eq $endpoint) {
        return
    }

    $localHosts = @("127.0.0.1", "localhost")
    if ($localHosts -notcontains $endpoint.Host.ToLowerInvariant()) {
        return
    }

    if (Test-MySqlReachable -DbHost $endpoint.Host -Port $endpoint.Port) {
        return
    }

    $mysqlService = Find-MySqlService
    if ($mysqlService) {
        if ($mysqlService.Status -ne 'Running') {
            Write-Host "MySQL service '$($mysqlService.Name)' is not running. Starting it ..."
            Start-Service -Name $mysqlService.Name
        }

        for ($i = 0; $i -lt 20; $i++) {
            if (Test-MySqlReachable -DbHost $endpoint.Host -Port $endpoint.Port) {
                return
            }
            Start-Sleep -Milliseconds 500
        }
    }

    $mysqlInstall = Find-LocalMySqlInstall
    if ($mysqlInstall) {
        Write-Host "Local MySQL is not listening on $($endpoint.Host):$($endpoint.Port). Starting mysqld ..."
        Start-ManagedMySqlProcess -MysqldPath $mysqlInstall.MysqldPath -DefaultsFile $mysqlInstall.DefaultsFile | Out-Null

        for ($i = 0; $i -lt 20; $i++) {
            if (Test-MySqlReachable -DbHost $endpoint.Host -Port $endpoint.Port) {
                return
            }
            Start-Sleep -Milliseconds 500
        }

        $logFile = Get-ManagedMySqlLogFilePath
        $logHint = if (Test-Path $logFile) { " Last log: $logFile" } else { "" }
        throw "MySQL did not start listening on $($endpoint.Host):$($endpoint.Port).$logHint"
    }

    throw "MySQL is required at $($endpoint.Host):$($endpoint.Port), but no running MySQL instance or local install was found."
}
