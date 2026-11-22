@echo off
setlocal EnableExtensions EnableDelayedExpansion

title GRCONE Test Environment

set "PROJECT_NAME=grcone-test"
set "COMPOSE_ENV="
set "COMPOSE_FILE=compose.test.yml"
set "APP_JAR=grcone-app.jar"
set "SELECTED_JAR_FILE=.grcone-selected-jar.txt"
set "RESET_PROJECT_FILE=.grcone-test-reset-project"
set "MODE=up"
set "RESET_DATA=false"
set "DETACH=false"
set "JAR_SOURCE="
set "JAR_SOURCE_NAME="

cd /d "%~dp0"

call :parse_args %*
if errorlevel 1 exit /b %ERRORLEVEL%

if /I "%MODE%"=="up" if not defined JAR_SOURCE (
    set "JAR_SOURCE=%APP_JAR%"
    set "JAR_SOURCE_NAME=%APP_JAR%"
)

call :main
exit /b %ERRORLEVEL%

:parse_args
if "%~1"=="" exit /b 0

:parse_loop
if "%~1"=="" exit /b 0

if /I "%~1"=="--help" (
    set "MODE=help"
    shift
    goto :parse_loop
)
if /I "%~1"=="help" (
    set "MODE=help"
    shift
    goto :parse_loop
)
if /I "%~1"=="/?" (
    set "MODE=help"
    shift
    goto :parse_loop
)
if /I "%~1"=="--check-config" (
    set "MODE=check_config"
    shift
    goto :parse_loop
)
if /I "%~1"=="--check" (
    set "MODE=check"
    shift
    goto :parse_loop
)
if /I "%~1"=="--check-schema-copy" (
    set "MODE=schema_copy_check"
    shift
    goto :parse_loop
)

if /I "%~1"=="--status" (
    set "MODE=status"
    shift
    goto :parse_loop
)
if /I "%~1"=="status" (
    set "MODE=status"
    shift
    goto :parse_loop
)
if /I "%~1"=="--ps" (
    set "MODE=status"
    shift
    goto :parse_loop
)
if /I "%~1"=="--logs" (
    set "MODE=logs"
    shift
    goto :parse_loop
)
if /I "%~1"=="logs" (
    set "MODE=logs"
    shift
    goto :parse_loop
)
if /I "%~1"=="--down" (
    set "MODE=down"
    shift
    goto :parse_loop
)
if /I "%~1"=="down" (
    set "MODE=down"
    shift
    goto :parse_loop
)
if /I "%~1"=="--reset" (
    set "RESET_DATA=true"
    shift
    goto :parse_loop
)
if /I "%~1"=="reset" (
    set "RESET_DATA=true"
    shift
    goto :parse_loop
)
if /I "%~1"=="--detach" (
    set "DETACH=true"
    shift
    goto :parse_loop
)
if /I "%~1"=="detach" (
    set "DETACH=true"
    shift
    goto :parse_loop
)

if defined JAR_SOURCE (
    echo ERROR: Only one jar file can be selected.
    echo First jar:  %JAR_SOURCE%
    echo Second arg: %~1
    exit /b 1
)

if exist "%~1" (
    set "JAR_SOURCE=%~1"
    set "JAR_SOURCE_NAME=%~nx1"
    shift
    goto :parse_loop
)

echo ERROR: Unknown argument or file not found: %~1
exit /b 1

:main
if /I "%MODE%"=="help" (
    call :usage
    exit /b 0
)

if /I not "%MODE%"=="up" if /I "%DETACH%"=="true" (
    echo ERROR: --detach is only valid when starting the environment.
    exit /b 1
)
if /I not "%MODE%"=="up" if /I "%RESET_DATA%"=="true" (
    echo ERROR: --reset is only valid when starting the environment.
    exit /b 1
)
if /I "%MODE%"=="check_config" if defined JAR_SOURCE (
    echo ERROR: --check-config does not accept a JAR path.
    exit /b 1
)
if /I "%MODE%"=="check" if defined JAR_SOURCE (
    echo ERROR: --check uses the installed grcone-app.jar.
    exit /b 1
)

if not exist "%COMPOSE_FILE%" (
    echo ERROR: Compose file not found: %CD%\%COMPOSE_FILE%
    exit /b 1
)

call :check_docker
if errorlevel 1 exit /b %ERRORLEVEL%
call :select_test_installation
if errorlevel 1 exit /b %ERRORLEVEL%

if /I "%MODE%"=="check_config" (
    call :compose_check_config
    exit /b !ERRORLEVEL!
)
if /I "%MODE%"=="check" (
    call :run_db_check --stage-db-check
    exit /b !ERRORLEVEL!
)
if /I "%MODE%"=="schema_copy_check" (
    call :run_db_check --schema-copy-check
    exit /b !ERRORLEVEL!
)

if /I "%MODE%"=="status" (
    call :compose_status
    exit /b !ERRORLEVEL!
)
if /I "%MODE%"=="down" (
    call :compose_down
    exit /b !ERRORLEVEL!
)
if /I "%MODE%"=="logs" (
    call :compose_logs
    exit /b !ERRORLEVEL!
)
if /I not "%MODE%"=="up" exit /b 1

if /I not "%RESET_DATA%"=="true" if exist "%RESET_PROJECT_FILE%.pending" (
    echo ERROR: A previous reset was interrupted. Run --reset again to finish it.
    exit /b 1
)

call :validate_jar
if errorlevel 1 exit /b %ERRORLEVEL%

if /I "%RESET_DATA%"=="true" (
    call :manage_test_installation reset
    if errorlevel 1 exit /b !ERRORLEVEL!
    call :use_grcone_database
) else if not exist "%RESET_PROJECT_FILE%" (
    call :manage_test_installation recover
    if errorlevel 1 exit /b !ERRORLEVEL!
    if exist "%RESET_PROJECT_FILE%" call :use_grcone_database
)

call :prepare_dirs
if errorlevel 1 exit /b %ERRORLEVEL%

call :prepare_jar
if errorlevel 1 exit /b %ERRORLEVEL%

echo.
echo ============================================================
echo Starting GRCONE test environment
echo ============================================================
echo.
call :print_jar_info
if /I "%DETACH%"=="true" (echo Run mode:          DETACHED) else (echo Run mode:          ATTACHED)
echo Oracle data:       Docker named volume managed by Compose
echo MinIO data:        %CD%\data\minio
echo Application logs:  %CD%\data\app\logs
echo.

if /I "%DETACH%"=="true" (
    call :start_detached
    exit /b !ERRORLEVEL!
)

call :start_attached
exit /b %ERRORLEVEL%

:manage_test_installation
set "RESET_RUNNER_FILE=%~f0"
set "TEST_INSTALLATION_ACTION=%~1"
powershell -NoProfile -Command ^
  "$parts = (Get-Content -LiteralPath $env:RESET_RUNNER_FILE -Raw) -split '(?m)^# Test reset PowerShell\r?$', 2; & ([scriptblock]::Create($parts[1]))"
exit /b %ERRORLEVEL%

:use_grcone_database
set "DB_USERNAME=GRCONE"
set "DB_SCHEMA=GRCONE"
set "DB_URL=jdbc:oracle:thin:@//oracle:1521/FREEPDB1"
for /F "tokens=1,* delims==" %%a in ('findstr /r /x /c:"APP_PORT=[0-9][0-9]*" /c:"ORACLE_PORT=[0-9][0-9]*" /c:"MINIO_API_PORT=[0-9][0-9]*" /c:"MINIO_CONSOLE_PORT=[0-9][0-9]*" "%RESET_PROJECT_FILE%"') do set "%%a=%%b"
exit /b 0

:prepare_dirs
if not exist "data" mkdir "data" >nul 2>&1
if not exist "data\minio" mkdir "data\minio" >nul 2>&1
if not exist "data\app" mkdir "data\app" >nul 2>&1
if not exist "data\app\logs" mkdir "data\app\logs" >nul 2>&1
exit /b 0

:validate_jar
if /I not "%JAR_SOURCE:~-4%"==".jar" (
    echo ERROR: The selected file is not a .jar file: %JAR_SOURCE%
    exit /b 1
)
if not exist "%JAR_SOURCE%" (
    echo ERROR: Jar file not found: %JAR_SOURCE%
    exit /b 1
)
for %%F in ("%JAR_SOURCE%") do set "JAR_SOURCE_ATTRIBUTES=%%~aF"
if /I "%JAR_SOURCE_ATTRIBUTES:~0,1%"=="d" (
    echo ERROR: Expected a JAR file, found a directory: %JAR_SOURCE%
    exit /b 1
)
exit /b 0

:prepare_jar
for %%F in ("%JAR_SOURCE%") do set "JAR_SOURCE_FULL=%%~fF"
for %%F in ("%APP_JAR%") do set "APP_JAR_FULL=%%~fF"

echo.
echo Selected jar: %JAR_SOURCE%
echo Executable runtime jar: %APP_JAR%

if /I "!JAR_SOURCE_FULL!"=="!APP_JAR_FULL!" (
    echo Selected jar is already the executable runtime jar.
) else (
    echo Copying selected jar to runtime file...
    copy /Y "%JAR_SOURCE%" "%CD%\%APP_JAR%" >nul
    if errorlevel 1 exit /b 1
)

> "%SELECTED_JAR_FILE%" echo %JAR_SOURCE_NAME%
exit /b 0

:load_selected_jar_name
set "SELECTED_JAR_NAME=%APP_JAR%"
if exist "%SELECTED_JAR_FILE%" set /p SELECTED_JAR_NAME=<"%SELECTED_JAR_FILE%"
if not defined SELECTED_JAR_NAME set "SELECTED_JAR_NAME=%APP_JAR%"
exit /b 0

:print_jar_info
call :load_selected_jar_name
echo Executable jar name: %SELECTED_JAR_NAME%
echo Runtime mounted jar: %APP_JAR%
echo Runtime jar path:   %CD%\%APP_JAR%
if exist "%APP_JAR%" (
    for %%F in ("%APP_JAR%") do echo Runtime jar size:   %%~zF bytes
) else (
    echo Runtime jar status: NOT FOUND
)
exit /b 0

:check_docker
echo Checking Docker CLI...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker CLI is not available.
    exit /b 1
)

echo Checking Docker Compose plugin...
docker compose version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker Compose v2 plugin is not available.
    exit /b 1
)

if /I "%MODE%"=="check_config" exit /b 0

echo Checking Docker engine...
docker info >nul 2>&1
if not errorlevel 1 (
    echo Docker engine is ready.
    exit /b 0
)

echo Docker engine is not ready. Trying to start Docker Desktop...
if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
) else (
    echo ERROR: Docker Desktop.exe was not found.
    exit /b 1
)

for /L %%i in (1,1,120) do (
    docker info >nul 2>&1
    if not errorlevel 1 (
        echo Docker engine is ready.
        exit /b 0
    )
    timeout /t 2 /nobreak >nul
)

echo ERROR: Docker engine is not ready.
exit /b 1

:select_test_installation
set "RESET_PROJECT="
set "RESET_PROJECT_SOURCE=%RESET_PROJECT_FILE%"
if /I "%RESET_DATA%"=="true" if exist "%RESET_PROJECT_FILE%.pending" set "RESET_PROJECT_SOURCE=%RESET_PROJECT_FILE%.pending"
if exist "%RESET_PROJECT_SOURCE%" (
    for /F "delims=" %%p in ('findstr /r /x "[a-z0-9][a-z0-9_-]*" "%RESET_PROJECT_SOURCE%"') do set "RESET_PROJECT=%%p"
    if not defined RESET_PROJECT (
        echo ERROR: Invalid generated Test project file: %RESET_PROJECT_SOURCE%
        exit /b 1
    )
)
if defined GRCONE_TEST_PROJECT (
    set "PROJECT_NAME=%GRCONE_TEST_PROJECT%"
) else if defined RESET_PROJECT (
    set "PROJECT_NAME=!RESET_PROJECT!"
) else if /I not "%MODE%"=="check_config" (
    set "EXISTING_TEST_PROJECT="
    for %%p in (grcone-test digigrcone-test grcpc-test) do (
        docker volume inspect "%%p_oracle-data" >nul 2>&1
        if not errorlevel 1 (
            if defined EXISTING_TEST_PROJECT (
                echo ERROR: Multiple Test volumes exist. Select one with GRCONE_TEST_PROJECT.
                exit /b 1
            )
            set "EXISTING_TEST_PROJECT=%%p"
        )
    )
    if not defined EXISTING_TEST_PROJECT (
        for %%p in (digigrcone-test grcpc-test) do (
            docker container inspect "%%p-oracle" >nul 2>&1
            if not errorlevel 1 (
                if /I not "%RESET_DATA%"=="true" (
                    echo ERROR: Found a previous Test Oracle container with a different storage layout. Verify its mount before upgrading.
                    exit /b 1
                )
                if defined EXISTING_TEST_PROJECT (
                    echo ERROR: Multiple Test projects exist. Select one with GRCONE_TEST_PROJECT.
                    exit /b 1
                )
                set "EXISTING_TEST_PROJECT=%%p"
            )
        )
        if /I not "%RESET_DATA%"=="true" if exist "data\oracle\." (
            for /F "delims=" %%f in ('dir /b /a "data\oracle" 2^>nul') do (
                echo ERROR: Found legacy Oracle files in data/oracle. Migrate the existing bind mount before starting this version.
                exit /b 1
            )
        )
    )
    if defined EXISTING_TEST_PROJECT set "PROJECT_NAME=!EXISTING_TEST_PROJECT!"
)
set "GRCONE_TEST_PROJECT=%PROJECT_NAME%"
if "%PROJECT_NAME%"=="grcpc-test" (
    set "GRCONE_TEST_DB_USER=GRCPC"
    set "GRCONE_TEST_DB_PASSWORD=grcpc"
)
if "%PROJECT_NAME%"=="digigrcone-test" (
    set "GRCONE_TEST_DB_USER=DIGIGRCONE"
    set "GRCONE_TEST_DB_PASSWORD=digigrcone"
)
if exist test.env (
    if exist .env (set "COMPOSE_ENV=--env-file .env --env-file test.env") else (set "COMPOSE_ENV=--env-file test.env")
)
if "%RESET_PROJECT_SOURCE%"=="%RESET_PROJECT_FILE%" if "%PROJECT_NAME%"=="%RESET_PROJECT%" call :use_grcone_database
exit /b 0

:compose_check_config
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" config --quiet
if errorlevel 1 exit /b %ERRORLEVEL%
echo Test configuration is valid. Oracle connectivity was not checked.
exit /b 0

:run_db_check
if not exist "%APP_JAR%" (
    echo ERROR: Runtime JAR is missing: %APP_JAR%
    exit /b 1
)
powershell -NoProfile -Command ^
  "$ErrorActionPreference = 'Stop'; Add-Type -AssemblyName System.IO.Compression.FileSystem; $jar = [IO.Compression.ZipFile]::OpenRead((Join-Path (Get-Location).Path $env:APP_JAR)); try { if (-not $jar.GetEntry('BOOT-INF/classes/com/digiaudit/grcone/runtime/StageDatabaseCommand.class')) { exit 1 } } finally { $jar.Dispose() }"
if errorlevel 1 (
    echo ERROR: Runtime JAR lacks the Oracle check commands. Install the updated application JAR.
    exit /b 1
)
if /I "%MODE%"=="check" if exist "%RESET_PROJECT_FILE%.pending" (
    echo ERROR: A previous reset was interrupted. Run --reset again to finish it.
    exit /b 1
)
if /I "%MODE%"=="check" if not exist "%RESET_PROJECT_FILE%" (
    call :manage_test_installation recover
    if errorlevel 1 exit /b !ERRORLEVEL!
    if exist "%RESET_PROJECT_FILE%" call :use_grcone_database
)
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" run --rm --no-deps --entrypoint java app -jar grcone-app.jar %~1
exit /b %ERRORLEVEL%

:is_environment_running
set "RUNNING_CONTAINER="
for /F "delims=" %%i in ('docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" ps --status running -q 2^>nul') do (
    set "RUNNING_CONTAINER=%%i"
    goto :is_environment_running_done
)
:is_environment_running_done
if defined RUNNING_CONTAINER exit /b 0
exit /b 1

:start_attached
echo Starting in ATTACHED mode.
if defined APP_PORT echo Application host port: %APP_PORT%
echo Press Ctrl+C to stop the environment.
echo.
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" up --force-recreate --remove-orphans app
set "COMPOSE_EXIT_CODE=%ERRORLEVEL%"
echo.
echo Attached session ended. Ensuring GRCONE services are stopped...
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" stop >nul 2>&1
echo GRCONE test environment is stopped. Persistent data was preserved.
exit /b %COMPOSE_EXIT_CODE%

:start_detached
echo Starting in DETACHED mode...
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" up -d --force-recreate --remove-orphans app
if errorlevel 1 exit /b %ERRORLEVEL%
call :wait_for_app_port
if errorlevel 1 exit /b %ERRORLEVEL%
echo.
echo GRCONE test environment is running in DETACHED mode.
exit /b 0

:wait_for_app_port
set "GRCONE_TEST_APP_PORT="
for /F "tokens=2 delims=:" %%p in ('docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" port app 8080') do set "GRCONE_TEST_APP_PORT=%%p"
if not defined GRCONE_TEST_APP_PORT exit /b 1
echo Waiting for the application on port %GRCONE_TEST_APP_PORT%...
powershell -NoProfile -Command ^
  "$url = 'http://127.0.0.1:' + $env:GRCONE_TEST_APP_PORT + '/api/setup/status'; for ($i = 0; $i -lt 180; $i++) { try { $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { exit 0 } } catch {}; Start-Sleep -Seconds 2 }; exit 1" >nul 2>&1
if not errorlevel 1 (
    echo Application is ready.
    exit /b 0
)
echo ERROR: Application did not become ready on port %GRCONE_TEST_APP_PORT%. See --logs.
exit /b 1

:compose_status
echo.
echo ============================================================
echo GRCONE Test Environment Status
echo ============================================================
echo.
call :print_jar_info
echo.
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" ps --all
echo.
echo Docker Compose volumes:
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" config --volumes
exit /b %ERRORLEVEL%

:compose_down
echo Stopping GRCONE test environment ^(persistent data will be preserved^)...
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" down --remove-orphans
exit /b %ERRORLEVEL%

:compose_logs
docker compose %COMPOSE_ENV% -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" logs -f
exit /b %ERRORLEVEL%

:usage
echo.
echo ============================================================
echo GRCONE Test Environment Runner
echo ============================================================
echo.
echo Default: ATTACHED mode using .\grcone-app.jar
echo.
echo   grcone-app.test.bat
echo   grcone-app.test.bat grcone-app.jar
echo       Equivalent. Press Ctrl+C to stop.
echo.
echo   grcone-app.test.bat --detach
echo       Start in background.
echo.
echo   grcone-app.test.bat --reset
echo       Show a destructive-reset warning and require y/yes confirmation.
echo       If confirmed, remove Oracle named volume, MinIO data and logs,
echo       then start attached with a fresh GRCONE Oracle schema.
echo       The launcher remembers the chosen project and schema for future starts.
echo.
echo   grcone-app.test.bat --reset --detach
echo       Same confirmation, then reset all test data and start detached.
echo.
echo   grcone-app.test.bat grcone-app-1.1.0.jar --reset
echo       Same confirmation, then reset and use another jar.
echo.
echo   grcone-app.test.bat --check-config
echo       Validate Docker Compose settings without connecting to Oracle.
echo   grcone-app.test.bat --check
echo       Check a running Test Oracle using a temporary table.
echo   grcone-app.test.bat --check-schema-copy
echo   grcone-app.test.bat --status
echo   grcone-app.test.bat --logs
echo   grcone-app.test.bat --down
echo   grcone-app.test.bat --help
echo.
echo --down preserves persistent data.
echo --reset destroys persistent test data only after explicit confirmation.
echo.
exit /b 0

# Test reset PowerShell
$ErrorActionPreference = 'Stop'

function Get-LocalPath([string]$Path) {
    if ($Path -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-z])/(.*)$') {
        $Path = $Matches[1] + ':\' + $Matches[2]
    }
    if ($Path -notmatch '^(?:[a-z]:[\\/]|\\\\[^\\]+\\[^\\]+)') {
        throw "Cannot verify the Windows path: $Path"
    }
    [IO.Path]::GetFullPath($Path)
}

function Assert-ResetTarget($Target) {
    $root = Get-LocalPath $Target.Root
    $path = Get-LocalPath $Target.Path
    $prefix = $root.TrimEnd('\') + '\'
    if (-not $path.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase) -or
        $path.TrimEnd('\') -eq $root.TrimEnd('\')) {
        throw "Test data directory is outside its deployment: $path"
    }
    while ($path) {
        $item = Get-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        if ($item -and (-not $item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint))) {
            throw "Unsafe Test data directory: $path"
        }
        $path = [IO.Path]::GetDirectoryName($path)
    }
}

function Get-PreviousTest($Containers) {
    $directories = @(
        foreach ($container in $Containers) {
            $labels = $container.Config.Labels
            $root = Get-LocalPath $labels.'com.docker.compose.project.working_dir'
            $configFile = Join-Path $root 'compose.test.yml'
            $files = $labels.'com.docker.compose.project.config_files' -split ',' | ForEach-Object { Get-LocalPath $_ }
            if ($configFile -notin $files) {
                throw "Docker project $env:PROJECT_NAME belongs to a different deployment."
            }
            $root
        }
    ) | Select-Object -Unique
    if (@($directories).Count -ne 1) { throw 'The existing Test containers use different installation directories.' }
    $recordedRoot = $directories | Select-Object -First 1
    $dataRoot = $recordedRoot
    if (-not (Test-Path -LiteralPath $recordedRoot -PathType Container)) {
        # Docker may still have the paths from before the GRCONE repository rename.
        $renamedRoot = $recordedRoot -replace '(?<=\\)digiaudit-(?:grcpc|digigrcone)(?=\\|$)', 'digiaudit-grcone'
        if ($renamedRoot -ne $recordedRoot -and (Test-Path -LiteralPath (Join-Path $renamedRoot 'compose.test.yml') -PathType Leaf)) {
            $dataRoot = $renamedRoot
        }
    }
    $targets = @(
        foreach ($container in $Containers) {
            $service = $container.Config.Labels.'com.docker.compose.service'
            foreach ($mount in $container.Mounts) {
                if ($mount.Type -ne 'bind') { continue }
                if (($service -eq 'minio' -and $mount.Destination -eq '/data') -or
                    ($service -eq 'oracle' -and $mount.Destination -eq '/opt/oracle/oradata') -or
                    ($service -eq 'app' -and $mount.Destination -match '^/opt/(?:grcpc|digigrcone|grcone)/logs$')) {
                    $target = @{ Root = $recordedRoot; Path = (Get-LocalPath $mount.Source) }
                    Assert-ResetTarget $target
                    $relative = $target.Path.Substring($recordedRoot.TrimEnd('\').Length + 1)
                    @{ Root = $dataRoot; Path = (Join-Path $dataRoot $relative) }
                }
            }
        }
        @{ Root = $dataRoot; Path = (Join-Path $dataRoot 'data\oracle') }
    )
    [pscustomobject]@{ RecordedRoot = $recordedRoot; DataRoot = $dataRoot; Targets = $targets }
}

function Get-TestPorts($Config) {
    $requested = [ordered]@{
        APP_PORT = ($Config.services.app.ports | Where-Object target -eq 8080).published
        ORACLE_PORT = ($Config.services.oracle.ports | Where-Object target -eq 1521).published
        MINIO_API_PORT = ($Config.services.minio.ports | Where-Object target -eq 9000).published
        MINIO_CONSOLE_PORT = ($Config.services.minio.ports | Where-Object target -eq 9001).published
    }
    $listeners = @()
    try {
        foreach ($key in $requested.Keys) {
            $port = [int]$requested[$key]
            $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Any, $port)
            $listener.ExclusiveAddressUse = $true
            try {
                $listener.Start()
            } catch [Net.Sockets.SocketException] {
                $listener.Stop()
                $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Any, 0)
                $listener.ExclusiveAddressUse = $true
                $listener.Start()
            }
            $listeners += $listener
            $selected = $listener.LocalEndpoint.Port
            if ($port -ne $selected) { Write-Host "$key uses available port $selected." }
            "$key=$selected"
        }
    } finally {
        foreach ($listener in $listeners) { $listener.Stop() }
    }
}

function Save-TestSettings($Marker, $Project, $Ports) {
    $next = "$Marker.next"
    [IO.File]::WriteAllLines($next, @($Project) + @($Ports))
    if (Test-Path -LiteralPath $Marker) {
        [IO.File]::Replace($next, $Marker, [NullString]::Value)
    } else {
        [IO.File]::Move($next, $Marker)
    }
}

function Read-TestCompose($Arguments) {
    $config = docker @Arguments config --format json | Out-String | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or -not $config) { throw 'Could not read the Test Compose configuration.' }
    $config
}

function Restore-TestSettings($Containers, $Compose, $Marker, $Project) {
    $services = @{}
    foreach ($container in $Containers) {
        $services[$container.Config.Labels.'com.docker.compose.service'] = $container
    }
    if (@($services.app.Config.Env) -cnotcontains 'DB_SCHEMA=GRCONE' -and
        @($services.oracle.Config.Env) -cnotcontains 'APP_USER=GRCONE') { return }

    if (($services.app -and @($services.app.Config.Env) -cnotcontains 'DB_SCHEMA=GRCONE') -or
        ($services.oracle -and @($services.oracle.Config.Env) -cnotcontains 'APP_USER=GRCONE')) {
        throw 'The existing Test containers disagree on the Oracle schema. Recover the original Test settings before starting.'
    }
    $env:DB_USERNAME = 'GRCONE'
    $env:DB_SCHEMA = 'GRCONE'
    $env:DB_URL = 'jdbc:oracle:thin:@//oracle:1521/FREEPDB1'
    $config = Read-TestCompose $Compose

    $required = @{
        app = @('DB_USERNAME', 'DB_PASSWORD', 'DB_URL', 'GRCONE_MINIO_ACCESS_KEY', 'GRCONE_MINIO_SECRET_KEY')
        oracle = @('APP_USER_PASSWORD')
        minio = @('MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD')
    }
    foreach ($service in $required.Keys) {
        if (-not $services[$service]) { continue }
        foreach ($key in $required[$service]) {
            $value = $config.services.$service.environment.$key
            if ($services[$service].Config.Env -cnotcontains "$key=$value") {
                throw 'The saved Test settings differ from the existing containers. Restore the original Test env files before starting.'
            }
        }
    }

    if ($services.minio) {
        $current = $services.minio.Mounts | Where-Object Destination -eq '/data'
        $configured = $config.services.minio.volumes | Where-Object target -eq '/data'
        if ($current.Type -ne 'bind' -or (Get-LocalPath $current.Source) -ne (Get-LocalPath $configured.source)) {
            throw 'The existing Test uses a different MinIO directory. Restore its Test settings before starting.'
        }
    }
    $bindings = [ordered]@{
        APP_PORT = $services.app.HostConfig.PortBindings.'8080/tcp'
        ORACLE_PORT = $services.oracle.HostConfig.PortBindings.'1521/tcp'
        MINIO_API_PORT = $services.minio.HostConfig.PortBindings.'9000/tcp'
        MINIO_CONSOLE_PORT = $services.minio.HostConfig.PortBindings.'9001/tcp'
    }
    $ports = foreach ($key in $bindings.Keys) {
        $port = ($bindings[$key] | Select-Object -First 1).HostPort
        if ($port -match '^\d+$') { "$key=$port" }
    }
    Save-TestSettings $Marker $Project $ports
    Write-Host "Recovered GRCONE settings from Docker Test project $Project."
}

try {
    $project = $env:PROJECT_NAME
    if ($project -notmatch '^[a-z0-9][a-z0-9_-]*$') { throw 'Invalid Test project name.' }
    $currentRoot = (Get-Item -LiteralPath '.').FullName
    $marker = Join-Path $currentRoot $env:RESET_PROJECT_FILE
    $pendingFile = "$marker.pending"
    $extra = @($env:COMPOSE_ENV -split ' ' | Where-Object { $_ })
    $compose = @('compose') + $extra + @('-f', $env:COMPOSE_FILE, '-p', $project)

    $ids = @(docker ps --all --quiet --filter "label=com.docker.compose.project=$project")
    if ($LASTEXITCODE -ne 0) { throw 'Could not identify existing Test containers.' }
    $previous = $null
    $containers = @()
    if ($ids.Count) {
        $containers = docker container inspect @ids | Out-String | ConvertFrom-Json
        if ($LASTEXITCODE -ne 0 -or -not $containers) { throw 'Could not inspect existing Test containers.' }
        $previous = Get-PreviousTest $containers
    }

    if ($env:TEST_INSTALLATION_ACTION -eq 'recover') {
        if ($previous -and $previous.RecordedRoot -ne $currentRoot) {
            throw "Start the existing Test from $($previous.RecordedRoot)."
        }
        if ($previous) {
            Restore-TestSettings $containers $compose $marker $project
        } elseif ($env:PACKAGE_START -eq 'true' -and $project -eq 'grcone-test' -and
                  -not (Test-Path -LiteralPath 'test.env') -and -not (Test-Path -LiteralPath '.env')) {
            docker volume inspect "$($project)_oracle-data" *> $null
            if ($LASTEXITCODE -ne 0) {
                Save-TestSettings $marker $project @(Get-TestPorts (Read-TestCompose $compose))
            }
        }
        exit 0
    }

    $config = Read-TestCompose $compose
    $minio = $config.services.minio.volumes | Where-Object { $_.type -eq 'bind' -and $_.target -eq '/data' }
    $logs = $config.services.app.volumes | Where-Object { $_.type -eq 'bind' -and $_.target -eq '/opt/grcone/logs' }
    if (-not ($minio.source -and $logs.source)) { throw 'Could not identify the Test bind mounts.' }
    $targets = @(
        @{ Root = $currentRoot; Path = (Get-LocalPath $minio.source) }
        @{ Root = $currentRoot; Path = (Get-LocalPath $logs.source) }
        @{ Root = $currentRoot; Path = (Join-Path $currentRoot 'data\oracle') }
    )
    if ($previous) { $targets += $previous.Targets }
    $plan = $null
    if (Test-Path -LiteralPath $pendingFile -PathType Leaf) {
        $saved = @(Get-Content -LiteralPath $pendingFile -Encoding UTF8)
        if ($saved[0] -ne $project) { throw 'The pending reset belongs to a different Test project.' }
        if ($saved.Count -gt 1) {
            $plan = $saved[1] | ConvertFrom-Json
            if ($plan.Directory -ne $currentRoot -or ($previous -and $plan.PreviousDirectory -ne $previous.RecordedRoot)) {
                throw 'The Test installation changed since the previous reset attempt.'
            }
            $targets += @($plan.Targets)
        }
    }
    $targets = @($targets | Sort-Object -Property { $_.Path } -Unique)
    foreach ($target in $targets) { Assert-ResetTarget $target }

    Write-Host "Docker Test project: $project"
    if ($previous) {
        Write-Host "Previous Test installation: $($previous.RecordedRoot)"
        if ($previous.DataRoot -ne $previous.RecordedRoot) {
            Write-Host "Relocated Test data found at: $($previous.DataRoot)"
        }
    }
    Write-Host "New Test installation: $currentRoot"
    Write-Host 'Reset permanently deletes the Test Oracle volume and these existing data directories:' -ForegroundColor Yellow
    foreach ($target in $targets) {
        if (Test-Path -LiteralPath $target.Path) { Write-Host ('  ' + $target.Path) }
    }
    $question = 'Delete this Test and start again with schema GRCONE? [y/N]'
    if ([Console]::IsInputRedirected) {
        Write-Host $question
        $answer = [Console]::ReadLine()
    } else {
        $answer = Read-Host $question
    }
    if ($answer -notin @('y', 'yes')) {
        Write-Host 'Reset cancelled. No persistent data was changed.'
        exit 2
    }

    $recordedDirectory = $null
    if ($previous) { $recordedDirectory = $previous.RecordedRoot }
    elseif ($plan) { $recordedDirectory = $plan.PreviousDirectory }
    $plan = @{
        Directory = $currentRoot
        PreviousDirectory = $recordedDirectory
        Targets = $targets
    }
    [IO.File]::WriteAllLines($pendingFile, @($project, ($plan | ConvertTo-Json -Depth 4 -Compress)))
    docker @compose down -v --remove-orphans
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    foreach ($target in $targets) {
        Assert-ResetTarget $target
        if (Test-Path -LiteralPath $target.Path) { Remove-Item -LiteralPath $target.Path -Recurse -Force }
    }
    Save-TestSettings $marker $project @(Get-TestPorts $config)
    Remove-Item -LiteralPath $pendingFile
} catch {
    [Console]::Error.WriteLine('ERROR: ' + $_.Exception.Message)
    exit 1
}
