param(
    [switch]$Detailed
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

Ensure-RuntimeDirs
Show-ServiceStatus -Detailed:$Detailed
