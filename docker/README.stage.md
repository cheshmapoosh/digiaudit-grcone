# Stage environment with external Oracle

Put `grcone-app.stage.sh`, `compose.stage.yml`, `grcone-app.jar`, and `stage.env`
in the same deployment directory, with the proxy script at
`docker/nginx/start.sh`. Load the Java, MinIO and Nginx images before starting.
Set `PUBLIC_HOST` to the server's IP address or domain. For HTTPS, set
`PUBLIC_SCHEME=https` and mount the certificate and key with `NGINX_TLS_DIR`.
See the [Docker environments guide](README.md#stage-and-production-with-external-oracle).
Create `stage.env` with `--init-env` if needed;
the template is `docker/env/stage.env.example`. Keep the template in that path
when copying the full repository, or copy it beside the runner as
`stage.env.example` for a portable deployment bundle.
Build and deploy a new application JAR containing `StageDatabaseCommand` before
using `--check` or `--init`; the runner rejects older JARs.

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
./grcone-app.stage.sh --init --detach
./grcone-app.stage.sh --logs
```

`--check-config` validates Compose and `stage.env` only. `--check` starts a
one-shot Java process in the stage container. It connects with the same Oracle
settings as the app, creates a uniquely named `GRCONE_CHECK_*` table, inserts,
reads, updates, reads, deletes, verifies the deletion, and drops the table.
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

`--init` requires an interactive `RESET` confirmation. It stops the stage
containers, uses Flyway `clean` on **all objects in `DB_SCHEMA`**, runs all
packaged migrations from the beginning, and starts the containers. This
permanently deletes the schema's tables and data, including Flyway history.
It does not delete MinIO files. `DB_SCHEMA` must equal the connected Oracle
user's own current schema; Oracle-maintained schemas are refused. Use a dedicated schema
and stop other processes connected to it before initialization. If a migration
fails, the app is not started and the Flyway error appears in the command output.

The Java 21 `jre` container does not include `jshell`. The one-shot command in
the packaged JAR uses its bundled Oracle JDBC driver and Flyway libraries, so
the stage host and container need no JDK or separate JDBC download.
