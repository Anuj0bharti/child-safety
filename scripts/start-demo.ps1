param(
    [string]$ApiUrl = "http://127.0.0.1:8000"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $projectRoot "backend"
$dashboard = Join-Path $projectRoot "authority-dashboard"

Start-Process -FilePath "python" -ArgumentList "-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", "8000" -WorkingDirectory $backend -WindowStyle Hidden
Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev", "--", "--host", "127.0.0.1" -WorkingDirectory $dashboard -WindowStyle Hidden

Write-Host "Backend: $ApiUrl/docs"
Write-Host "Authority dashboard: http://127.0.0.1:5173"
Write-Host "Both processes run in the background. Use Task Manager or your terminal to stop them."
