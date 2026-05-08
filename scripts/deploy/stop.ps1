$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\.."

$appName = "campus-trade-hub"
$pidPath = Join-Path (Get-Location) "run\$appName.pid"

if (-not (Test-Path $pidPath)) {
    Write-Host "[stop] No pid file. $appName may not be running."
    exit 0
}

$processId = Get-Content $pidPath -ErrorAction SilentlyContinue
if (-not $processId) {
    Remove-Item $pidPath -ErrorAction SilentlyContinue
    Write-Host "[stop] Empty pid file removed."
    exit 0
}

$proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
if (-not $proc) {
    Remove-Item $pidPath -ErrorAction SilentlyContinue
    Write-Host "[stop] Process not found. pid file removed."
    exit 0
}

Stop-Process -Id $processId -Force
Remove-Item $pidPath -ErrorAction SilentlyContinue
Write-Host "[stop] Stopped $appName (PID: $processId)"
