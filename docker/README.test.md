# GRCONE Portable Test Runner

This runner is for Windows machines where you already have a built Spring Boot
boot jar and want to run the full test infrastructure with Docker Compose.

It starts:

- Java 21 runtime for `grcone-app`
- Oracle Free test database
- MinIO object storage

## Fresh Windows package

Extract the fresh Test ZIP and double-click `grcone-app.test.fresh.bat`.
Docker Desktop with Linux containers is required. The launcher creates
installation-specific settings, credentials, available local ports, and a
Compose project under its package directory. It initializes Oracle with the
`GRCONE` schema, uses a dedicated Oracle volume and MinIO directory, and opens
the application after its setup endpoint responds.

Run the same BAT from the same directory to reopen the installation. Keep the
generated `.grcone-fresh-test` directory beside it to retain the connection
settings and MinIO data. This launcher is intended for a fresh Test rather than
an upgrade of an existing Test deployment.

## Run

Put your boot jar next to `grcone-app.test.bat` and `compose.test.yml` with this
exact name:

```text
grcone-app.jar
```

Then run from that directory in PowerShell:

```powershell
.\grcone-app.test.bat
```

You can also pass a custom jar path:

```powershell
.\grcone-app.test.bat "path\to\grcone-app-version.jar"
```

The runner copies the selected jar to `grcone-app.jar` next to the BAT file and
starts `compose.test.yml`. It reads an existing `.env` followed by `test.env`
when present.

## Update an existing installation

Back up the Oracle volume and MinIO data. Copy the new JAR, runner and
`compose.test.yml` into the current deployment directory. The runner looks
for the existing `grcone-test`, `digigrcone-test` or `grcpc-test` Oracle
volume and reuses the associated Compose project, credentials and MinIO
directory. Custom Oracle settings should be kept in the existing env file.
When multiple supported volumes exist, set `GRCONE_TEST_PROJECT` in the
shell to select the deployment. Legacy MinIO settings are accepted and the
default lifecycle mode is `VALIDATE`.

```powershell
.\grcone-app.test.bat --detach
```

Use the same runner for status and logs so the existing project is selected.
For a separate Oracle schema copy, use the procedure in the
[Stage upgrade guide](README.stage.md#update-an-existing-installation).

To start Compose directly with the example settings, use
`docker compose --env-file docker/env/test.env.example -f compose.test.yml up -d`
with the JAR already in place. Set `PUBLIC_HOST` to the test server's IP
address or hostname if the product manager connects from another machine. This
sets the address used by the browser for signed downloads.

## Useful commands

```powershell
# Start in attached mode; Ctrl+C stops the environment and preserves data
.\grcone-app.test.bat

# Start in detached mode
.\grcone-app.test.bat --detach

# Stop containers and preserve Oracle/MinIO data
.\grcone-app.test.bat --down

# Show status or follow logs
.\grcone-app.test.bat --status
.\grcone-app.test.bat --logs
```

`--reset` requires explicit confirmation and permanently removes the Oracle
named volume, MinIO object data, and application logs before starting again.
It is not needed to resolve a port conflict.

## Endpoints and data

- Application: <http://localhost:8080>
- Oracle: `localhost:1521`, service `FREEPDB1`, user `GRCONE`, password `grcone`
- MinIO API: <http://localhost:9000> by default
- MinIO Console: <http://localhost:9001> by default
- MinIO user/password: `minioadmin` / `minioadmin`
- Oracle data: Docker named volume managed by Compose
- MinIO object data: `data/minio` next to the runner
- Application logs: `data/app/logs` next to the runner

## Windows cannot bind MinIO port 9000

If Docker reports `ports are not available` and
`listen tcp 0.0.0.0:9000: bind: An attempt was made to access a socket in a way
forbidden by its access permissions`, Windows has refused the host port used
by MinIO. An excluded/reserved port range or another process may be responsible;
the error alone does not identify which.

On the affected laptop, use these read-only PowerShell commands to inspect
listeners and excluded TCP port ranges:

```powershell
Get-NetTCPConnection -LocalPort 9000 -ErrorAction SilentlyContinue
netsh interface ipv4 show excludedportrange protocol=tcp
```

Choose available host ports outside the excluded ranges. For example, if
`19000` and `19001` are available, run from the package directory:

```powershell
.\grcone-app.test.bat --down
$env:MINIO_API_PORT = '19000'
$env:MINIO_CONSOLE_PORT = '19001'
.\grcone-app.test.bat
```

The application still opens at <http://localhost:8080>. MinIO API and Console
move to <http://localhost:19000> and <http://localhost:19001>. Compose also
updates the application's public MinIO endpoint, so browser-facing presigned
download URLs use the selected API port.

These PowerShell variables last for the current session. For subsequent starts
by double-clicking the BAT file, create a `.env` text file next to
`compose.test.yml` containing:

```dotenv
MINIO_API_PORT=19000
MINIO_CONSOLE_PORT=19001
```

Compose loads that `.env` automatically. Variables in the calling shell take
precedence. With no overrides, the API and Console keep ports `9000` and `9001`.
Keep this local file limited to the laptop's port settings.

## Notes

- The application connects to Oracle using the Compose service name `oracle`.
- Internal MinIO connections remain `http://minio:9000`; only published host
  ports and the public download endpoint change.
- Flyway is enabled and uses the Oracle migrations packaged inside the boot jar.
