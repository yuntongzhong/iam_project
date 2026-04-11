param(
    [switch]$Clean,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

$paths = Get-PlatformPaths
$javaInfo = Initialize-JavaEnvironment -MinimumMajorVersion 17

$gradleArgs = @()
if ($Clean) {
    $gradleArgs += "clean"
}
$gradleArgs += "build"
if ($SkipTests) {
    $gradleArgs += "-x"
    $gradleArgs += "test"
}

Write-Host ("Using Java {0}: {1}" -f $javaInfo.JavaMajorVersion, $javaInfo.JavaExecutable)
Write-Host ("Running .\gradlew.bat {0}" -f ($gradleArgs -join " "))

Push-Location $paths.RepoRoot
try {
    & .\gradlew.bat @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "Build artifacts:"
foreach ($app in Get-AppConfig) {
    if (Test-Path $app.JarPath) {
        $jar = Get-Item $app.JarPath
        Write-Host ("- {0}: {1} ({2:N0} bytes)" -f $app.Name, $jar.FullName, $jar.Length)
    } else {
        Write-Host ("- {0}: missing expected jar at {1}" -f $app.Name, $app.JarPath)
    }
}
