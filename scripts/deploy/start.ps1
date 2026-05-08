$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\.."

$appName = "campus-trade-hub"
$jarPath = Join-Path (Get-Location) "target\campus-trade-hub-1.0.0.jar"
$pidPath = Join-Path (Get-Location) "run\$appName.pid"
$logDir = Join-Path (Get-Location) "logs"
$logPath = Join-Path $logDir "$appName.out.log"
$errorLogPath = Join-Path $logDir "$appName.err.log"

if (-not $env:DB_URL) {
    $env:DB_URL = "jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=ershoupingtai;encrypt=false;trustServerCertificate=true"
}

if (-not $env:DB_USERNAME) {
    $env:DB_USERNAME = "sa"
}

if (-not $env:DB_PASSWORD) {
    $env:DB_PASSWORD = "204729"
}

if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = "campus-trade-platform-secret-key-2024-spring-boot"
}

if (-not $env:ADMIN_USERNAME) {
    $env:ADMIN_USERNAME = "admin"
}

if (-not $env:ADMIN_PASSWORD) {
    $env:ADMIN_PASSWORD = "admin123"
}

if (-not $env:SERVER_PORT) {
    $env:SERVER_PORT = "8081"
}

if (-not (Test-Path $jarPath)) {
    throw "Jar not found: $jarPath. Please run scripts/deploy/build.ps1 first."
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

if (-not (Test-Path (Split-Path $pidPath))) {
    New-Item -ItemType Directory -Path (Split-Path $pidPath) | Out-Null
}

if (Test-Path $pidPath) {
    $oldPid = Get-Content $pidPath -ErrorAction SilentlyContinue
    if ($oldPid) {
        $proc = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "[start] $appName already running (PID: $oldPid)"
            exit 0
        }
    }
    Remove-Item $pidPath -ErrorAction SilentlyContinue
}

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    throw "Java not found in PATH"
}

Write-Host "[start] Starting $appName ..."
$argumentList = "-Dspring.profiles.active=prod -jar `"$jarPath`""
$proc = Start-Process -FilePath $javaCmd.Source -ArgumentList $argumentList -RedirectStandardOutput $logPath -RedirectStandardError $errorLogPath -PassThru

Set-Content -Path $pidPath -Value $proc.Id
Write-Host "[start] Started. PID=$($proc.Id)"
Write-Host "[start] Log: $logPath"
Write-Host "[start] Error log: $errorLogPath"
