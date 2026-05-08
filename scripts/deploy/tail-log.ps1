$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\.."

$logPath = Join-Path (Get-Location) "logs\campus-trade-hub.out.log"

if (-not (Test-Path $logPath)) {
    throw "Log file not found: $logPath"
}

Get-Content -Path $logPath -Tail 200 -Wait
