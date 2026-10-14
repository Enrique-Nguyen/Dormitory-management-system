param()

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$backendDirectory = Join-Path $projectDirectory 'backend'
$runDirectory = Join-Path $backendDirectory 'data\local-run'
$postgresDirectory = Join-Path $backendDirectory 'data\local-postgres'

function Stop-ManagedProcess([string] $Name, [string] $ExpectedCommand) {
    $pidFile = Join-Path $runDirectory "$Name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) {
        return
    }
    $processId = 0
    if (-not [int]::TryParse((Get-Content -LiteralPath $pidFile -Raw).Trim(), [ref] $processId)) {
        throw "Invalid PID file: $pidFile"
    }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
    # Chỉ dừng tiến trình của DMS nếu PID và đường dẫn lệnh vẫn khớp.
    if ($process) {
        if ($process.CommandLine -notlike "*$ExpectedCommand*") {
            throw "PID $processId belongs to another process. Refusing to stop it."
        }
        $managedProcess = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($managedProcess) {
            Stop-Process -Id $processId
            $managedProcess.WaitForExit()
        }
    }
    Remove-Item -LiteralPath $pidFile
}

Stop-ManagedProcess 'frontend' (Join-Path $projectDirectory 'frontend\node_modules\vite\bin\vite.js')
Stop-ManagedProcess 'backend' (Join-Path $backendDirectory 'target\dms-backend-0.0.1-SNAPSHOT.jar')
Stop-ManagedProcess 'keycloak' (Join-Path $backendDirectory 'data\keycloak-runtime')

# Dừng riêng cluster của DMS; giữ nguyên toàn bộ dữ liệu cho lần chạy sau.
$pgControl = Join-Path $postgresDirectory 'runtime\bin\pg_ctl.exe'
$pgData = Join-Path $postgresDirectory 'pgdata'
if ((Test-Path -LiteralPath $pgControl) -and (Test-Path -LiteralPath (Join-Path $pgData 'PG_VERSION'))) {
    & $pgControl -D $pgData status *> $null
    if ($LASTEXITCODE -eq 0) {
        $postgresProcess = Start-Process -FilePath $pgControl -ArgumentList @(
            '-D', "`"$pgData`"", '-m', 'fast', '-w', 'stop'
        ) -WindowStyle Hidden -PassThru
        $postgresProcess.WaitForExit()
        if ($postgresProcess.ExitCode -ne 0) {
            throw 'PostgreSQL could not stop cleanly. Check backend/data/local-run/postgres.log.'
        }
    }
}
Write-Host 'DMS stopped. Database files are preserved.'
