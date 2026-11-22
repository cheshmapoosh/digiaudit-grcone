param([switch]$CheckConfig)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

function New-InstallationSettings {
    $settings = [ordered]@{
        Project = 'grcone-test-fresh-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
        DB_PASSWORD = 'Gr9_' + [guid]::NewGuid().ToString('N').Substring(0, 24)
        ORACLE_SYS_PASSWORD = 'Or9_' + [guid]::NewGuid().ToString('N').Substring(0, 24)
        MINIO_ROOT_USER = 'test-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
        MINIO_ROOT_PASSWORD = [guid]::NewGuid().ToString('N')
    }

    $listeners = @()
    try {
        foreach ($key in @('APP_PORT', 'ORACLE_PORT', 'MINIO_API_PORT', 'MINIO_CONSOLE_PORT')) {
            $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
            $listeners += $listener
            $listener.Start()
            $settings[$key] = $listener.LocalEndpoint.Port
        }
    } finally {
        foreach ($listener in $listeners) { $listener.Stop() }
    }
    return $settings
}

function Test-DockerEngine {
    try {
        docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    }
}

function Wait-ForDockerEngine {
    if (Test-DockerEngine) { return }

    $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path -LiteralPath $desktop -PathType Leaf)) {
        throw 'Start the Docker engine and run the launcher again.'
    }
    Write-Host 'Starting Docker Desktop...'
    Start-Process -FilePath $desktop -WindowStyle Hidden
    $deadline = [DateTime]::UtcNow.AddMinutes(3)
    do {
        Start-Sleep -Seconds 2
        if (Test-DockerEngine) { return }
    } while ([DateTime]::UtcNow -lt $deadline)
    throw 'Docker Desktop did not become ready.'
}

function Wait-ForApplication([string]$Url) {
    $deadline = [DateTime]::UtcNow.AddMinutes(8)
    Write-Host "Waiting for GRCONE at $Url. First startup may take several minutes."
    do {
        try {
            $status = Invoke-RestMethod -Uri "$Url/api/setup/status" -TimeoutSec 3
            if ($status.initialized -is [bool]) { return }
        } catch {
            # The web server may still be starting while Flyway initializes the schema.
        }
        Start-Sleep -Seconds 3
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "GRCONE did not become ready at $Url."
}

foreach ($file in @('grcone-app.jar', 'compose.test.yml')) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "Missing package file: $file" }
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker Desktop with Docker Compose v2 is required.'
}

$stateDir = Join-Path $PSScriptRoot '.grcone-fresh-test'
$envFile = Join-Path $stateDir 'test.env'
[void][IO.Directory]::CreateDirectory($stateDir)
$lock = [IO.File]::Open((Join-Path $stateDir 'launcher.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
try {
    if (Test-Path -LiteralPath $envFile -PathType Leaf) {
        $settings = ConvertFrom-StringData (Get-Content -LiteralPath $envFile -Raw)
    } else {
        $settings = New-InstallationSettings
        $tempFile = Join-Path $stateDir ('settings-' + [guid]::NewGuid().ToString('N') + '.tmp')
        $lines = @($settings.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" })
        [IO.File]::WriteAllLines($tempFile, [string[]]$lines)
        [IO.File]::Move($tempFile, $envFile)
    }

    $project = $settings.Project
    if ($project -cnotmatch '^grcone-test-fresh-[0-9a-f]{12}$') {
        throw 'The fresh Test configuration has an invalid project name.'
    }

    foreach ($key in @('APP_PORT', 'ORACLE_PORT', 'MINIO_API_PORT', 'MINIO_CONSOLE_PORT')) {
        $port = 0
        if (-not [int]::TryParse($settings[$key], [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
            throw "Invalid port in the fresh Test configuration: $key"
        }
        [Environment]::SetEnvironmentVariable($key, [string]$port, 'Process')
    }
    foreach ($key in @('DB_PASSWORD', 'ORACLE_SYS_PASSWORD', 'MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD')) {
        if ([string]::IsNullOrWhiteSpace($settings[$key])) { throw "Missing setting: $key" }
        [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
    }

    $env:GRCONE_TEST_PROJECT = $project
    $env:GRCONE_TEST_DATA_DIR = "./.grcone-fresh-test/$project"
    $env:GRCONE_TEST_BIND_PREFIX = '127.0.0.1:'
    $env:DB_USERNAME = 'GRCONE'
    $env:DB_SCHEMA = 'GRCONE'
    $env:DB_URL = 'jdbc:oracle:thin:@//oracle:1521/FREEPDB1'
    $env:GRCONE_MINIO_ACCESS_KEY = $env:MINIO_ROOT_USER
    $env:GRCONE_MINIO_SECRET_KEY = $env:MINIO_ROOT_PASSWORD
    $env:GRCONE_MINIO_PUBLIC_ENDPOINT = "http://localhost:$($env:MINIO_API_PORT)"
    $env:GRCONE_MINIO_BUCKET = 'grc-documents'
    $env:GRCONE_MINIO_LIFECYCLE_MODE = 'VALIDATE'

    $compose = @('compose', '--env-file', $envFile, '-f', 'compose.test.yml', '-p', $project)
    $url = "http://localhost:$($env:APP_PORT)"

    if ($CheckConfig) {
        docker @compose config --quiet
        if ($LASTEXITCODE -ne 0) { throw 'The Docker Compose configuration is invalid.' }
        Write-Host "Configuration verified for $project at $url"
        return
    }

    $dockerHost = $env:DOCKER_HOST
    if ($env:DOCKER_CONTEXT -or -not $dockerHost) {
        $dockerHost = docker context inspect --format '{{.Endpoints.docker.Host}}'
        if ($LASTEXITCODE -ne 0) { throw 'Could not identify the active Docker context.' }
    }
    if ($dockerHost -notmatch '^npipe:/{4}\./pipe/' -and
        $dockerHost -notmatch '^tcp://(localhost|127\.0\.0\.1|\[::1\]):') {
        throw 'Select a local Windows Docker context before starting fresh Test.'
    }

    Wait-ForDockerEngine
    docker @compose up -d app
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose could not start $project." }
    try {
        Wait-ForApplication $url
    } catch {
        docker @compose logs --tail 30 app
        throw
    }
    Write-Host "GRCONE Test is ready at $url"
    Start-Process $url
} finally {
    $lock.Dispose()
}
