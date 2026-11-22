# Stage environment with external Oracle

Put `grcone-app.stage.sh`, `compose.stage.yml`, `grcone-app.jar`, and `stage.env`
in the same deployment directory, with the proxy script at
`docker/nginx/start.sh`. Load the Java and MinIO images before starting, and
load the Nginx image when using the bundled proxy.
Set `PUBLIC_HOST` to the server's IP address or domain. For HTTPS, set
`PUBLIC_SCHEME=https` and mount the certificate and key with `NGINX_TLS_DIR`.
See the [Docker environments guide](README.md#stage-and-production-with-external-oracle).
Create `stage.env` with `--init-env` if needed;
the template is `docker/env/stage.env.example`. Keep the template in that path
when copying the full repository, or copy it beside the runner as
`stage.env.example` for a portable deployment bundle.
Build and deploy a new application JAR containing `StageDatabaseCommand` before
using `--check` or `--reset`; the runner rejects older JARs.

## Update an existing installation

Keep the deployment directory, `stage.env` and `data/minio` from the current
installation. Back up Oracle and MinIO before updating. Copy the new JAR as
`grcone-app.jar`, the Stage runner, `compose.stage.yml` and the proxy script
into that directory. The runner recognizes an existing Stage Compose project
in the same directory and recreates the app and proxy. The configured Oracle
credentials and schema are used as-is. Legacy MinIO keys are accepted when
the corresponding `GRCONE_MINIO_*` key is absent. The default lifecycle mode
is `VALIDATE`, which reads the current policy without rewriting it.

```bash
bash ./grcone-app.stage.sh --check-config
bash ./grcone-app.stage.sh --detach
bash ./grcone-app.stage.sh --logs
```

The Java app logs `Oracle target:` before Spring initializes its database
connections. A different configured Flyway URL is reported separately. The
Java maintenance commands report their target before asking JDBC to connect.
Both paths omit credentials and JDBC URL options.

### Start without the bundled Nginx

Set this in the existing `stage.env`:

```dotenv
STAGE_NGINX_ENABLED=false
```

Start with the usual command:

```bash
bash ./grcone-app.stage.sh
```

This stops and removes any bundled Nginx container in the selected Stage
project. The runner supplies the direct port settings to Compose, so the bundle
uses one Compose file. Java serves HTTP on `APP_PORT` (default `8080`) and the MinIO API
uses `STAGE_MINIO_API_PORT` (default `9000`). Signed document URLs use
`http://PUBLIC_HOST:STAGE_MINIO_API_PORT` by default. An explicit
`GRCONE_MINIO_PUBLIC_ENDPOINT` still takes precedence.

For HTTPS behind your own reverse proxy, terminate TLS there and configure a
browser-accessible MinIO URL. Set `STAGE_DIRECT_BIND_ADDRESS=127.0.0.1` if a
proxy on the same host should be the only entry point to both ports. Set
`STAGE_NGINX_ENABLED=true` to return to the bundled proxy. The default is `true`
when the setting is absent. The `--no-nginx` argument remains available as a
one-time override.

To change the Oracle schema name as part of a separate database operation,
have the Oracle administrator export the current schema and import it into a
new user in the same PDB with Data Pump `REMAP_SCHEMA`. Stop application
writes for the final copy. Compare tables and row contents, Flyway history,
constraints and sequences before changing `DB_USERNAME`, `DB_PASSWORD` and
`DB_SCHEMA` together. Retain the source schema until the new application and
an existing attachment have been verified. The startup above does not
transfer schemas.

## New installation and maintenance

Run these commands from the deployment directory:

```bash
./grcone-app.stage.sh --check-config
./grcone-app.stage.sh --check
./grcone-app.stage.sh --reset --detach
./grcone-app.stage.sh --logs
```

`--check-config` validates Compose and `stage.env` only. `--check` starts a
one-shot Java process in the stage container. It connects with the same Oracle
settings as the app and reads the database identity, PDB, service and server
from the current session. It also reads the current PDB's `OPEN_MODE` when
the account can query `V$PDBS`. If access to that view is unavailable, the
check reports it and continues. The command then creates a uniquely named
`GRCONE_CHECK_*` table, inserts, reads, updates, reads, deletes, verifies the
deletion, and drops the table. The release ZIP uses its bundled JAR for
`--check`, so this check can run before updating the running application.
It drops the table in a `finally` block if a CRUD step fails. A failed drop is
reported as a failure and may require manual cleanup.
The command prints each SQL statement, its bound test values, affected row
counts, read values, and the final table removal. It does not print database
credentials. Because the check container is removed when it exits, save its
console output if you need to retain the log:

```bash
set -o pipefail
./grcone-app.stage.sh --check 2>&1 | tee stage-check.log
```

`--reset` requires an interactive `RESET` confirmation. It stops the stage
containers, uses Flyway `clean` on **all objects in `DB_SCHEMA`**, runs all
packaged migrations from the beginning, and starts the containers. This
permanently deletes the schema's tables and data, including Flyway history.
It does not delete MinIO files. `DB_SCHEMA` must equal the connected Oracle
user's own current schema; Oracle-maintained schemas are refused. Use a dedicated schema
and stop other processes connected to it before initialization. If a migration
fails, the app is not started and the Flyway error appears in the command output.
`--init` is a compatibility alias for `--reset`.

The Java 21 `jre` container does not include `jshell`. The one-shot command in
the packaged JAR uses its bundled Oracle JDBC driver and Flyway libraries, so
the stage host and container need no JDK or separate JDBC download.
