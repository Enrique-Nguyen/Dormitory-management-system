param(
    [switch] $ProvisionAccounts,
    [switch] $DependenciesOnly
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$backendDirectory = Join-Path $projectDirectory 'backend'
$frontendDirectory = Join-Path $projectDirectory 'frontend'
$runDirectory = Join-Path $backendDirectory 'data\local-run'
$postgresDirectory = Join-Path $backendDirectory 'data\local-postgres'
$pgControl = Join-Path $postgresDirectory 'runtime\bin\pg_ctl.exe'
$pgData = Join-Path $postgresDirectory 'pgdata'
$backendJar = Join-Path $backendDirectory 'target\dms-backend-0.0.1-SNAPSHOT.jar'
$viteScript = Join-Path $frontendDirectory 'node_modules\vite\bin\vite.js'

# Các file log, PID và CSDL nằm trong backend/data, đã được Git bỏ qua.
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

function Test-PortListening([int] $Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
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

function Wait-HttpReady([string] $Url, [int] $Seconds, [string] $ErrorLog) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    } while ((Get-Date) -lt $deadline)
    throw "Service did not become ready: $Url. Read log: $ErrorLog"
}

function Start-LocalKeycloak([string] $JavaExecutable) {
    $backendDirectory = Join-Path $projectDirectory 'backend'
    $runDirectory = Join-Path $backendDirectory 'data\local-run'
    $runtimeRoot = Join-Path $backendDirectory 'data\keycloak-runtime'
    $version = '26.8.1'
    $runtimeDirectory = Join-Path $runtimeRoot "keycloak-$version"
    $secretsFile = Join-Path $runDirectory 'keycloak.env.local'
    New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
    New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

    if (-not (Test-Path -LiteralPath (Join-Path $runtimeDirectory 'bin\kc.bat'))) {
        $package = Join-Path $runtimeRoot "keycloak-$version.zip"
        Write-Host "Downloading official Keycloak $version..."
        Invoke-WebRequest -Uri "https://github.com/keycloak/keycloak/releases/download/$version/keycloak-$version.zip" `
            -OutFile $package -UseBasicParsing
        Expand-Archive -LiteralPath $package -DestinationPath $runtimeRoot -Force
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

    # Đây là cấu hình riêng của máy, không có mật khẩu mặc định trong repository.
    if (-not (Test-Path -LiteralPath $secretsFile)) {
        $lines = @(
            "DMS_KEYCLOAK_ADMIN_CLIENT_SECRET=$(New-LocalSecret)",
            'KC_BOOTSTRAP_ADMIN_USERNAME=keycloak-admin',
            "KC_BOOTSTRAP_ADMIN_PASSWORD=$(New-LocalSecret)",
            'DMS_KEYCLOAK_INITIAL_ADMIN_USERNAME=admin',
            "DMS_KEYCLOAK_INITIAL_ADMIN_PASSWORD=$(New-LocalSecret)"
        )
        [System.IO.File]::WriteAllLines($secretsFile, $lines, (New-Object System.Text.UTF8Encoding($false)))
    }
    foreach ($line in Get-Content -LiteralPath $secretsFile) {
        if ($line -match '^([A-Z_]+)=(.*)$') {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
        }
    }

    $listener = Get-NetTCPConnection -LocalPort 8180 -State Listen -ErrorAction SilentlyContinue
    if ($listener) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener[0].OwningProcess)"
        if ($process.CommandLine -notlike "*$runtimeDirectory*") {
            throw 'Port 8180 belongs to another service. Refusing to use it.'
        }
        return
    }

    $env:KC_DB = 'postgres'
    $env:KC_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/keycloak'
    $env:KC_DB_USERNAME = 'dms'
    $env:KC_DB_PASSWORD = 'dms'
    $importDirectory = Join-Path $runtimeDirectory 'data\import'
    New-Item -ItemType Directory -Path $importDirectory -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectDirectory 'keycloak\dms-realm.json') `
        -Destination (Join-Path $importDirectory 'dms-realm.json') -Force

    # Dùng java.exe thật để PID thuộc đúng máy chủ Keycloak, không thuộc cmd/kc.bat trung gian.
    $keycloakArguments = @(
        '-Xms64m', '-Xmx512m', '-Dfile.encoding=UTF-8',
        '--add-opens=java.base/java.lang=ALL-UNNAMED', '--enable-native-access=ALL-UNNAMED',
        '-Djava.util.logging.manager=org.jboss.logmanager.LogManager',
        '-Djava.util.concurrent.ForkJoinPool.common.threadFactory=io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory',
        "`"-Dkc.home.dir=$runtimeDirectory`"", "`"-Djboss.server.config.dir=$runtimeDirectory\conf`"",
        '-cp', "`"$runtimeDirectory\lib\quarkus-run.jar`"", 'io.quarkus.bootstrap.runner.QuarkusEntryPoint',
        'start-dev', '--http-host=127.0.0.1', '--http-port=8180', '--hostname=http://localhost:8180', '--import-realm'
    )
    $process = Start-Process -FilePath $JavaExecutable -ArgumentList $keycloakArguments `
        -WorkingDirectory $runtimeDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runDirectory 'keycloak.log') `
        -RedirectStandardError (Join-Path $runDirectory 'keycloak-error.log')
    Set-Content -LiteralPath (Join-Path $runDirectory 'keycloak.pid') -Value $process.Id -Encoding ASCII

    $deadline = (Get-Date).AddMinutes(5)
    do {
        if ($process.HasExited) {
            if ($process.ExitCode -eq 10) {
                # kc.bat cũng khởi động lại khi Quarkus vừa build cấu hình lần đầu (exit 10).
                $keycloakArguments = @('-Dkc.config.built=true') + $keycloakArguments
                $process = Start-Process -FilePath $JavaExecutable -ArgumentList $keycloakArguments `
                    -WorkingDirectory $runtimeDirectory -WindowStyle Hidden -PassThru `
                    -RedirectStandardOutput (Join-Path $runDirectory 'keycloak.log') `
                    -RedirectStandardError (Join-Path $runDirectory 'keycloak-error.log')
                Set-Content -LiteralPath (Join-Path $runDirectory 'keycloak.pid') -Value $process.Id -Encoding ASCII
                continue
            }
            throw "Keycloak exited. Read $runDirectory\keycloak.log and keycloak-error.log"
        }
        try {
            $response = Invoke-WebRequest -Uri 'http://127.0.0.1:8180/realms/dms/.well-known/openid-configuration' `
                -UseBasicParsing -TimeoutSec 10
            if ($response.StatusCode -eq 200) {
                Write-Host 'Keycloak ready: http://localhost:8180'
                return
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    } while ((Get-Date) -lt $deadline)
    throw "Keycloak startup timed out. Read $runDirectory\keycloak.log"
}

# Bản PostgreSQL local được chuẩn bị riêng trên máy này; không dùng CSDL kiểm thử.
if (-not (Test-Path -LiteralPath $pgControl) -or -not (Test-Path -LiteralPath (Join-Path $pgData 'PG_VERSION'))) {
    throw 'Local PostgreSQL is not prepared. Follow README.md to use Docker or installed PostgreSQL.'
}
if (-not $DependenciesOnly -and -not (Test-Path -LiteralPath $backendJar)) {
    throw 'Backend JAR is missing. Build first: cd DMS/backend; .\mvnw.cmd -DskipTests clean package'
}
if (-not $DependenciesOnly -and -not (Test-Path -LiteralPath $viteScript)) {
    throw 'Frontend dependencies are missing. Run npm.cmd ci in DMS/frontend.'
}
$javaCandidate = (Get-Command java.exe -ErrorAction Stop).Source
# Oracle javapath có thể trỏ vào trình trung gian. Chạy java.exe thật của JDK
# để PID lưu lại chính là backend, giúp script dừng đúng tiến trình.
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
$jdkDirectory = $javaHomeMatch.Matches[0].Groups[1].Value.Trim()
$javaExecutable = Join-Path $jdkDirectory 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Java executable is missing: $javaExecutable"
}
if (-not $DependenciesOnly) {
    $nodeExecutable = (Get-Command node.exe -ErrorAction Stop).Source
}

& $pgControl -D $pgData status *> $null
$postgresRunning = $LASTEXITCODE -eq 0
if (-not $postgresRunning) {
    if (Test-PortListening 5432) {
        throw 'Port 5432 is occupied by another process. Stop it or use the manual database configuration.'
    }
    # pg_ctl tách PostgreSQL thành tiến trình nền; không mở cửa sổ mới.
    $postgresLog = Join-Path $runDirectory 'postgres.log'
    $postgresProcess = Start-Process -FilePath $pgControl -ArgumentList @(
        '-D', "`"$pgData`"", '-l', "`"$postgresLog`"",
        '-o', '"-h 127.0.0.1 -p 5432"', '-w', 'start'
    ) -WindowStyle Hidden -PassThru
    # Chờ pg_ctl kết thúc, không chờ tiến trình PostgreSQL con chạy lâu dài.
    $postgresProcess.WaitForExit()
    if ($postgresProcess.ExitCode -ne 0) {
        throw "PostgreSQL failed to start. Read $postgresLog"
    }
}

# DMS/.env chỉ dành cho Docker Compose; Spring nhận cấu hình từ các biến này.
Start-LocalKeycloak -JavaExecutable $javaExecutable
$env:KEYCLOAK_URL = 'http://localhost:8180'
$env:KEYCLOAK_REALM = 'dms'
$env:VITE_KEYCLOAK_URL = 'http://localhost:8180'
$env:VITE_KEYCLOAK_REALM = 'dms'
$env:DMS_KEYCLOAK_BOOTSTRAP_ENABLED = $ProvisionAccounts.IsPresent.ToString().ToLowerInvariant()
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5432/dms'
$env:DB_USERNAME = 'dms'
$env:DB_PASSWORD = 'dms'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SERVER_PORT = '8080'

# Khi chạy backend bằng IntelliJ, chỉ chuẩn bị PostgreSQL và Keycloak,
# giữ cổng 8080 trống để tiến trình của IDE sử dụng.
if ($DependenciesOnly) {
    Write-Host 'PostgreSQL ready: localhost:5432 (databases: dms and keycloak)'
    Write-Host 'Keycloak ready: http://localhost:8180'
    Write-Host 'Dependencies are running. Start DmsApplication from IntelliJ.'
    Write-Host 'For account administration, set DMS_KEYCLOAK_ADMIN_CLIENT_SECRET in the IDE run configuration from backend/data/local-run/keycloak.env.local.'
    return
}

$backendProcess = Get-ManagedProcess 'backend' $backendJar
if (-not $backendProcess) {
    if (Test-PortListening 8080) {
        throw 'Port 8080 is occupied. The script will not stop a process it did not start.'
    }
    $backendLog = Join-Path $runDirectory 'backend.log'
    $backendErrorLog = Join-Path $runDirectory 'backend-error.log'
    $backendProcess = Start-Process -FilePath $javaExecutable -ArgumentList @('-jar', "`"$backendJar`"") `
        -WorkingDirectory $backendDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $backendLog -RedirectStandardError $backendErrorLog
    Set-Content -LiteralPath (Join-Path $runDirectory 'backend.pid') -Value $backendProcess.Id -Encoding ASCII
}
Wait-HttpReady 'http://127.0.0.1:8080/actuator/health' 120 (Join-Path $runDirectory 'backend.log')

$frontendProcess = Get-ManagedProcess 'frontend' $viteScript
if (-not $frontendProcess) {
    if (Test-PortListening 5173) {
        throw 'Port 5173 is occupied. The script will not stop a process it did not start.'
    }
    $frontendProcess = Start-Process -FilePath $nodeExecutable -ArgumentList @(
        "`"$viteScript`"", '--host', '127.0.0.1', '--port', '5173', '--strictPort'
    ) -WorkingDirectory $frontendDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runDirectory 'frontend.log') `
        -RedirectStandardError (Join-Path $runDirectory 'frontend-error.log')
    Set-Content -LiteralPath (Join-Path $runDirectory 'frontend.pid') -Value $frontendProcess.Id -Encoding ASCII
}
Wait-HttpReady 'http://127.0.0.1:5173/login' 20 (Join-Path $runDirectory 'frontend-error.log')

Write-Host 'DMS is running.'
Write-Host 'Frontend: http://localhost:5173/login'
Write-Host 'Backend:  http://localhost:8080/actuator/health'
Write-Host 'Keycloak: http://localhost:8180/admin/'
Write-Host "Logs:     $runDirectory"
