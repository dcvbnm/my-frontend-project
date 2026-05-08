$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\.."

Write-Host "[build] Checking Maven..."
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    throw "Maven (mvn) not found in PATH"
}

Write-Host "[build] Packaging application..."
mvn -DskipTests clean package

if ($LASTEXITCODE -ne 0) {
    throw "Build failed"
}

Write-Host "[build] Success"
