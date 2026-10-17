param()

# Chạy toàn bộ DMS trên máy dùng Docker Desktop: PostgreSQL + Keycloak trong container,
# backend (JAR) và frontend (Vite) chạy trên máy. Một lệnh duy nhất, chạy lại nhiều lần an toàn.
# Các bước: tạo DMS/.env (secret ngẫu nhiên) -> docker compose up -> backend migrate ->
# nạp sample-data.sql + sample-accounts.sql -> cấp tài khoản Keycloak -> chạy backend và frontend.
# Dừng bằng stop-local.ps1 (tiến trình) rồi `docker compose stop` trong thư mục DMS (container).

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$backendDirectory = Join-Path $projectDirectory 'backend'
$frontendDirectory = Join-Path $projectDirectory 'frontend'
$runDirectory = Join-Path $backendDirectory 'data\local-run'
$composeFile = Join-Path $projectDirectory 'docker-compose.yml'
$envFile = Join-Path $projectDirectory '.env'
$backendJar = Join-Path $backendDirectory 'target\dms-backend-0.0.1-SNAPSHOT.jar'
$viteScript = Join-Path $frontendDirectory 'node_modules\vite\bin\vite.js'
$credentialFile = Join-Path $runDirectory 'keycloak-docker-accounts.local'

# Các file log, PID, secret nằm trong backend/data và DMS/.env, đã được Git bỏ qua.
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

function Test-PortListening([int] $Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function New-LocalSecret {
    $bytes = New-Object byte[] 24
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return 'Dms1!' + [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function Get-ManagedProcess([string] $Name, [string] $ExpectedCommand) {
    $pidFile = Join-Path $runDirectory "$Name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) {
        return $null
    }
    $processId = 0
    if (-not [int]::TryParse((Get-Content -LiteralPath $pidFile -Raw).Trim(), [ref] $processId)) {
        throw "Invalid PID file: $pidFile"
    }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
    if ($process -and $process.CommandLine -like "*$ExpectedCommand*") {
        return $process
    }
    Remove-Item -LiteralPath $pidFile
    return $null
}

function Stop-ManagedProcess([string] $Name, [string] $ExpectedCommand) {
    $process = Get-ManagedProcess $Name $ExpectedCommand
    if (-not $process) {
        return
    }
    $managed = Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
    if ($managed) {
        Stop-Process -Id $process.ProcessId
        $managed.WaitForExit()
    }
    Remove-Item -LiteralPath (Join-Path $runDirectory "$Name.pid") -ErrorAction SilentlyContinue
}

function Wait-HttpReady([string] $Url, [int] $Seconds, [string] $Log, $Process) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        if ($Process -and $Process.HasExited) {
            throw "Process exited before $Url became ready. Read log: $Log"
        }
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    } while ((Get-Date) -lt $deadline)
    throw "Service did not become ready: $Url. Read log: $Log"
}

function Assert-Native([string] $What) {
    if ($LASTEXITCODE -ne 0) {
        throw "$What failed (exit code $LASTEXITCODE)."
    }
}

# 1. Điều kiện cần: Docker daemon, Java, Node.
$previousPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
docker info *> $null
$dockerReady = $LASTEXITCODE -eq 0
$ErrorActionPreference = $previousPreference
if (-not $dockerReady) {
    throw 'Docker is not running. Start Docker Desktop, wait until it is ready, then run this script again.'
}
$javaCandidate = (Get-Command java.exe -ErrorAction Stop).Source
$nodeExecutable = (Get-Command node.exe -ErrorAction Stop).Source
# Oracle javapath có thể trỏ vào trình trung gian; lấy java.exe thật để PID lưu lại đúng backend.
$javaPropertiesLog = Join-Path $runDirectory 'java-properties.log'
$javaCheck = Start-Process -FilePath $javaCandidate -ArgumentList @('-XshowSettings:properties', '-version') `
    -WindowStyle Hidden -Wait -PassThru `
    -RedirectStandardOutput (Join-Path $runDirectory 'java-version.log') -RedirectStandardError $javaPropertiesLog
if ($javaCheck.ExitCode -ne 0) {
    throw "Cannot run Java. Read $javaPropertiesLog"
}
$javaHomeMatch = Select-String -LiteralPath $javaPropertiesLog -Pattern '^\s*java\.home\s*=\s*(.+)\s*$'
if (-not $javaHomeMatch) {
    throw "Cannot determine the JDK directory. Read $javaPropertiesLog"
}
$javaExecutable = Join-Path $javaHomeMatch.Matches[0].Groups[1].Value.Trim() 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Java executable is missing: $javaExecutable"
}

# 2. DMS/.env: tạo secret ngẫu nhiên lần đầu, giữ nguyên các lần sau (Keycloak đã gắn với secret này).
if (-not (Test-Path -LiteralPath $envFile)) {
    $lines = @(
        'DB_NAME=dms',
        'DB_USERNAME=dms',
        'DB_PASSWORD=dms',
        'DB_PORT=5432',
        "KEYCLOAK_DB_PASSWORD=$(New-LocalSecret)",
        'KC_BOOTSTRAP_ADMIN_USERNAME=keycloak-admin',
        "KC_BOOTSTRAP_ADMIN_PASSWORD=$(New-LocalSecret)",
        "DMS_KEYCLOAK_ADMIN_CLIENT_SECRET=$(New-LocalSecret)",
        'DMS_KEYCLOAK_INITIAL_ADMIN_USERNAME=admin',
        "DMS_KEYCLOAK_INITIAL_ADMIN_PASSWORD=$(New-LocalSecret)"
    )
    [System.IO.File]::WriteAllLines($envFile, $lines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "Created $envFile with random secrets (Git ignores this file)."
}
$settings = @{}
foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^([A-Z_]+)=(.*)$') {
        $settings[$matches[1]] = $matches[2]
    }
}
foreach ($required in 'DB_NAME', 'DB_USERNAME', 'DB_PASSWORD', 'DB_PORT', 'DMS_KEYCLOAK_ADMIN_CLIENT_SECRET',
        'DMS_KEYCLOAK_INITIAL_ADMIN_USERNAME', 'DMS_KEYCLOAK_INITIAL_ADMIN_PASSWORD') {
    if (-not $settings.ContainsKey($required) -or [string]::IsNullOrWhiteSpace($settings[$required])) {
        throw "Missing $required in $envFile. Add it, or delete the file so it is generated again (only if the Docker volumes are also removed)."
    }
}

# 3. Build backend/frontend nếu chưa có.
if (-not (Test-Path -LiteralPath $backendJar)) {
    Write-Host 'Building backend JAR (first run, a few minutes)...'
    Push-Location $backendDirectory
    try {
        & .\mvnw.cmd -DskipTests clean package
        Assert-Native 'Maven build'
    } finally {
        Pop-Location
    }
}
if (-not (Test-Path -LiteralPath $viteScript)) {
    Write-Host 'Installing frontend dependencies (first run)...'
    Push-Location $frontendDirectory
    try {
        & npm.cmd ci
        Assert-Native 'npm ci'
    } finally {
        Pop-Location
    }
}

# 4. Container PostgreSQL + Keycloak. Dừng backend cũ (nếu có) để chạy lại đúng thứ tự các bước.
Stop-ManagedProcess 'backend' $backendJar
if (Test-PortListening 8080) {
    throw 'Port 8080 is occupied by a process this script did not start. Stop it and run again.'
}
Write-Host 'Starting PostgreSQL and Keycloak containers...'
docker compose -f $composeFile --project-directory $projectDirectory up -d
Assert-Native 'docker compose up'
$dbHealthDeadline = (Get-Date).AddSeconds(90)
do {
    $healthy = docker inspect --format '{{.State.Health.Status}}' dms-postgres
    if ($healthy -eq 'healthy') {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $dbHealthDeadline)
if ($healthy -ne 'healthy') {
    throw 'PostgreSQL container did not become healthy. Run: docker compose logs postgres'
}
Write-Host 'Waiting for Keycloak (first start imports the realm, up to a few minutes)...'
Wait-HttpReady 'http://127.0.0.1:8180/realms/dms/.well-known/openid-configuration' 300 'docker compose logs keycloak' $null

# 5. Biến môi trường cho backend và frontend (Spring không tự đọc .env của Compose).
$env:KEYCLOAK_URL = 'http://localhost:8180'
$env:KEYCLOAK_REALM = 'dms'
$env:VITE_KEYCLOAK_URL = 'http://localhost:8180'
$env:VITE_KEYCLOAK_REALM = 'dms'
$env:DB_URL = "jdbc:postgresql://127.0.0.1:$($settings['DB_PORT'])/$($settings['DB_NAME'])"
$env:DB_USERNAME = $settings['DB_USERNAME']
$env:DB_PASSWORD = $settings['DB_PASSWORD']
$env:DMS_KEYCLOAK_ADMIN_CLIENT_SECRET = $settings['DMS_KEYCLOAK_ADMIN_CLIENT_SECRET']
$env:DMS_KEYCLOAK_INITIAL_ADMIN_USERNAME = $settings['DMS_KEYCLOAK_INITIAL_ADMIN_USERNAME']
$env:DMS_KEYCLOAK_INITIAL_ADMIN_PASSWORD = $settings['DMS_KEYCLOAK_INITIAL_ADMIN_PASSWORD']
$env:DMS_KEYCLOAK_CREDENTIAL_FILE = $credentialFile
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SERVER_PORT = '8080'

function Start-Backend([bool] $Bootstrap) {
    $env:DMS_KEYCLOAK_BOOTSTRAP_ENABLED = $Bootstrap.ToString().ToLowerInvariant()
    $log = Join-Path $runDirectory 'backend.log'
    $process = Start-Process -FilePath $javaExecutable -ArgumentList @('-jar', "`"$backendJar`"") `
        -WorkingDirectory $backendDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $log -RedirectStandardError (Join-Path $runDirectory 'backend-error.log')
    Set-Content -LiteralPath (Join-Path $runDirectory 'backend.pid') -Value $process.Id -Encoding ASCII
    Wait-HttpReady 'http://127.0.0.1:8080/actuator/health' 240 $log $process
}

# Nạp file SQL trong container bằng psql -f: tránh pipe PowerShell làm hỏng tiếng Việt có dấu.
function Invoke-SampleSql([string] $File) {
    $name = Split-Path -Leaf $File
    docker compose -f $composeFile --project-directory $projectDirectory cp $File "postgres:/tmp/$name"
    Assert-Native "docker cp $name"
    docker compose -f $composeFile --project-directory $projectDirectory exec -T -e PGCLIENTENCODING=UTF8 postgres `
        psql -U $settings['DB_USERNAME'] -d $settings['DB_NAME'] -v ON_ERROR_STOP=1 -f "/tmp/$name"
    Assert-Native "psql $name"
}

# 6. Lần 1: backend tạo lược đồ bằng Flyway (chưa cấp tài khoản), rồi nạp dữ liệu mẫu.
Write-Host 'Starting backend to apply database migrations...'
Start-Backend $false
Stop-ManagedProcess 'backend' $backendJar
Invoke-SampleSql (Join-Path $PSScriptRoot 'sample-data.sql')
Invoke-SampleSql (Join-Path $PSScriptRoot 'sample-accounts.sql')

# 7. Lần 2: cấp Keycloak cho admin và mọi tài khoản chưa liên kết (mật khẩu tạm ngẫu nhiên, ghi vào
# tệp local). Chỉ chạy khi chưa có tệp cấp; bootstrap từ chối ghi đè tệp cũ.
if (-not (Test-Path -LiteralPath $credentialFile)) {
    Write-Host 'Provisioning Keycloak accounts...'
    Start-Backend $true
    Stop-ManagedProcess 'backend' $backendJar
} else {
    Write-Host "Credential file already exists, skipping provisioning: $credentialFile"
}

# 8. Lần 3: chạy bình thường, không bật cờ cấp tài khoản.
Write-Host 'Starting backend...'
Start-Backend $false

$frontendProcess = Get-ManagedProcess 'frontend' $viteScript
if (-not $frontendProcess) {
    if (Test-PortListening 5173) {
        throw 'Port 5173 is occupied by a process this script did not start.'
    }
    $frontendProcess = Start-Process -FilePath $nodeExecutable -ArgumentList @(
        "`"$viteScript`"", '--host', '127.0.0.1', '--port', '5173', '--strictPort'
    ) -WorkingDirectory $frontendDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runDirectory 'frontend.log') `
        -RedirectStandardError (Join-Path $runDirectory 'frontend-error.log')
    Set-Content -LiteralPath (Join-Path $runDirectory 'frontend.pid') -Value $frontendProcess.Id -Encoding ASCII
}
Wait-HttpReady 'http://127.0.0.1:5173/login' 30 (Join-Path $runDirectory 'frontend-error.log') $frontendProcess

Write-Host ''
Write-Host 'DMS is running.'
Write-Host 'Frontend: http://localhost:5173/login'
Write-Host 'Backend:  http://localhost:8080/actuator/health'
Write-Host 'Keycloak: http://localhost:8180/admin/ (user keycloak-admin, password: KC_BOOTSTRAP_ADMIN_PASSWORD in DMS\.env)'
Write-Host "Temporary passwords for admin and sample accounts: $credentialFile"
Write-Host 'Everyone must change the temporary password at first login. Delete that file with Remove-Item -LiteralPath once handed over.'
