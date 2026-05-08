$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\.."

$appName = "campus-trade-hub"
$pidPath = Join-Path (Get-Location) "run\$appName.pid"

if (-not (Test-Path $pidPath)) {
    Write-Host "[status] $appName is not running (no pid file)."
    exit 1
}

$processId = Get-Content $pidPath -ErrorAction SilentlyContinue
if (-not $processId) {
    Write-Host "[status] pid file is empty."
    exit 1
}

$proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
if ($proc) {
    Write-Host "[status] $appName is running (PID: $processId)"
    exit 0
}

Write-Host "[status] pid file exists but process is not running."
exit 1
