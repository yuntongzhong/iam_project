param(
    [string]$AuthorName = $env:USERNAME,
    [switch]$Clean,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

function Copy-RepoItem {
    param(
        [Parameter(Mandatory)][string]$RelativePath,
        [Parameter(Mandatory)][string]$DestinationRoot
    )

    $source = Join-Path $script:RepoRoot $RelativePath
    if (-not (Test-Path $source)) {
        return
    }

    $destination = Join-Path $DestinationRoot $RelativePath
    $parent = Split-Path -Parent $destination
    if ($parent) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    Copy-Item -LiteralPath $source -Destination $destination -Recurse -Force
}

function Ensure-RequiredFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Hint
    )

    if (-not (Test-Path $Path)) {
        throw $Hint
    }
}

$paths = Get-PlatformPaths
$author = if ([string]::IsNullOrWhiteSpace($AuthorName)) { $env:USERNAME } else { $AuthorName.Trim() }
$bundleName = "$author-V05企业级IAM服务原型"
$distRoot = Join-Path $paths.RepoRoot "dist\submission"
$bundleRoot = Join-Path $distRoot $bundleName
$zipPath = Join-Path $distRoot "$bundleName.zip"

New-Item -ItemType Directory -Path $distRoot -Force | Out-Null
Remove-Item -LiteralPath $bundleRoot -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $zipPath -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null

$packageArgs = @{}
if ($Clean) {
    $packageArgs.Clean = $true
}
if ($SkipTests) {
    $packageArgs.SkipTests = $true
}

& (Join-Path $PSScriptRoot "package.ps1") @packageArgs

$requiredDocs = @(
    "README.md",
    "docs\architecture.md",
    "docs\demo-script.md",
    "docs\scoring-map.md",
    "docs\ai-notes.md",
    "docs\submission-checklist.md"
)
foreach ($doc in $requiredDocs) {
    Ensure-RequiredFile -Path (Join-Path $paths.RepoRoot $doc) -Hint "Missing required submission file: $doc"
}

$requiredVideo = Join-Path $paths.RepoRoot "submission-assets\video\demo-walkthrough.mp4"
Ensure-RequiredFile -Path $requiredVideo -Hint "Missing required demo video at submission-assets\video\demo-walkthrough.mp4"

$sourceItems = @(
    "README.md",
    "build.gradle",
    "gradle.properties",
    "settings.gradle",
    "gradlew",
    "gradlew.bat",
    "gradle",
    "docs",
    "scripts",
    "submission-assets",
    "iam-server\build.gradle",
    "iam-server\src",
    "demo-app-a\build.gradle",
    "demo-app-a\src",
    "demo-app-b\build.gradle",
    "demo-app-b\src"
)
foreach ($item in $sourceItems) {
    Copy-RepoItem -RelativePath $item -DestinationRoot $bundleRoot
}

$artifactRoot = Join-Path $bundleRoot "artifacts"
New-Item -ItemType Directory -Path $artifactRoot -Force | Out-Null
foreach ($app in Get-AppConfig) {
    Ensure-RequiredFile -Path $app.JarPath -Hint "Missing build artifact for $($app.Name): $($app.JarPath)"
    Copy-Item -LiteralPath $app.JarPath -Destination (Join-Path $artifactRoot ([System.IO.Path]::GetFileName($app.JarPath))) -Force
}

Compress-Archive -LiteralPath $bundleRoot -DestinationPath $zipPath -Force

Write-Host ""
Write-Host "Submission package created:"
Write-Host $zipPath
