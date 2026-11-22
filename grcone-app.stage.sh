#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="${GRCONE_STAGE_PROJECT:-grcone-stage}"
COMPOSE_FILE="$SCRIPT_DIR/compose.stage.yml"
ENV_FILE="$SCRIPT_DIR/stage.env"
APP_JAR="$SCRIPT_DIR/grcone-app.jar"
MODE=""
DETACH="false"
USE_NGINX="true"
JAR_SOURCE=""

usage() {
    cat <<'EOF_USAGE'
GRCONE Stage Environment Runner (Java 21 + MinIO + Nginx, external Oracle)

Usage:
  ./grcone-app.stage.sh --init-env          Create stage.env from the example
  ./grcone-app.stage.sh --check-config      Validate stage.env and Compose
  ./grcone-app.stage.sh --check             Check Oracle connection and temporary-table CRUD
  ./grcone-app.stage.sh --check-schema-copy Read Oracle permissions for a future schema copy
  ./grcone-app.stage.sh --reset             Clean the stage Oracle schema, migrate, and start
  ./grcone-app.stage.sh --reset --detach    Reset and start in the background
  ./grcone-app.stage.sh                     Start attached using grcone-app.jar
  ./grcone-app.stage.sh --detach            Start in the background
  ./grcone-app.stage.sh --no-nginx --detach Start with direct HTTP and MinIO ports
  ./grcone-app.stage.sh /path/to/app.jar --detach
                                          Copy the selected JAR and start
  ./grcone-app.stage.sh --status            Show service status
  ./grcone-app.stage.sh --logs              Follow the latest service logs
  ./grcone-app.stage.sh --down              Stop and remove stage containers
  ./grcone-app.stage.sh --help

Attached mode is the default; Ctrl+C stops the services.
--detached and -d are aliases for --detach.
Set STAGE_NGINX_ENABLED=false in stage.env to skip the bundled reverse proxy.
--no-nginx overrides stage.env for this invocation.
Relative JAR paths are resolved from your current working directory.
Compose, stage.env and the runtime grcone-app.jar live beside this script.
MinIO files and application logs persist in data/minio and data/app/logs.
Starting with a replacement JAR recreates the application and proxy.
The application applies packaged Oracle migrations when FLYWAY_ENABLED=true.
--reset permanently removes all objects in the configured Oracle schema.
It requires an interactive RESET confirmation and does not erase MinIO files.
--init remains an alias for --reset.
--check creates and removes a uniquely named temporary table in that schema.
EOF_USAGE
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

set_mode() {
    [[ -z "$MODE" ]] || fail "Choose only one management command."
    MODE="$1"
}

parse_args() {
    while (($# > 0)); do
        case "$1" in
            --help|help|-h) set_mode help ;;
            --init-env|init-env) set_mode init_env ;;
            --reset|reset|--init|init) set_mode reset ;;
            --check-config|check-config) set_mode check_config ;;
            --check|check) set_mode check ;;
            --check-schema-copy) set_mode schema_copy_check ;;
            --status|status|--ps) set_mode status ;;
            --logs|logs) set_mode logs ;;
            --down|down) set_mode down ;;
            --detach|--detached|detach|-d) DETACH="true" ;;
            --no-nginx) USE_NGINX="false" ;;
            -*) fail "Unknown option: $1 (use --help)." ;;
            *)
                [[ -z "$JAR_SOURCE" ]] || fail "Only one JAR can be selected."
                # Capture the caller's directory before changing to SCRIPT_DIR.
                if [[ "$1" == /* ]]; then
                    JAR_SOURCE="$1"
                else
                    JAR_SOURCE="$PWD/$1"
                fi
                ;;
        esac
        shift
    done
    MODE="${MODE:-up}"
    if [[ "$MODE" != "up" && "$MODE" != "reset" && ( "$DETACH" == "true" || -n "$JAR_SOURCE" ) ]]; then
        fail "A JAR path and --detach are only valid when starting the environment."
    fi
    if [[ "$MODE" == "reset" && -n "$JAR_SOURCE" ]]; then
        fail "Place the intended runtime JAR beside this script before --reset; a JAR path is not accepted."
    fi
    JAR_SOURCE="${JAR_SOURCE:-$APP_JAR}"
}

compose() {
    local -a args=(sudo env APP_JAR_PATH="$APP_JAR" docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")
    if [[ "$USE_NGINX" == "true" ]]; then
        "${args[@]}" -p "$PROJECT_NAME" "$@"
        return
    fi

    # Only direct Stage publishes the app and MinIO API ports itself.
    "${args[@]}" -f - -p "$PROJECT_NAME" "$@" <<'DIRECT_STAGE'
services:
  app:
    ports:
      - target: 8080
        published: "${APP_PORT:-8080}"
        host_ip: "${STAGE_DIRECT_BIND_ADDRESS:-0.0.0.0}"
    environment:
      GRCONE_MINIO_PUBLIC_ENDPOINT: ${GRCONE_MINIO_PUBLIC_ENDPOINT:-${DIGIGRCONE_MINIO_PUBLIC_ENDPOINT:-${GRCPC_MINIO_PUBLIC_ENDPOINT:-http://${PUBLIC_HOST:-localhost}:${STAGE_MINIO_API_PORT:-9000}}}}
  minio:
    ports:
      - target: 9000
        published: "${STAGE_MINIO_API_PORT:-9000}"
        host_ip: "${STAGE_DIRECT_BIND_ADDRESS:-0.0.0.0}"
  nginx:
    profiles: [nginx]
DIRECT_STAGE
}

configure_nginx() {
    [[ "$USE_NGINX" == "true" ]] || return 0

    local enabled
    enabled="$(compose config --environment | sed -n 's/^STAGE_NGINX_ENABLED=//p')" \
        || fail "Could not read Stage settings from stage.env."
    case "${enabled,,}" in
        ""|true) ;;
        false) USE_NGINX="false" ;;
        *) fail "STAGE_NGINX_ENABLED must be true or false in stage.env." ;;
    esac
}

start_services() {
    if [[ "$USE_NGINX" == "true" ]]; then
        compose up "$@" app nginx
    else
        compose --profile nginx rm --stop --force nginx
        compose up "$@" app
    fi
}

select_existing_project() {
    [[ -z "${GRCONE_STAGE_PROJECT:-}" ]] || return 0

    local candidate containers selected=""
    for candidate in grcone-stage digigrcone-stage grcpc-stage; do
        containers="$(sudo docker ps -a --filter "label=com.docker.compose.project=$candidate" \
            --format '{{.Label "com.docker.compose.project.working_dir"}}')"
        if grep -Fxq -- "$SCRIPT_DIR" <<< "$containers"; then
            [[ -z "$selected" ]] || fail "Multiple Stage projects use this directory. Select one with GRCONE_STAGE_PROJECT."
            selected="$candidate"
        fi
    done
    PROJECT_NAME="${selected:-$PROJECT_NAME}"
}

init_env() {
    if [[ -e "$ENV_FILE" || -L "$ENV_FILE" ]]; then
        [[ -f "$ENV_FILE" && -r "$ENV_FILE" ]] || fail "stage.env is not a readable file."
        echo "Using existing stage.env; its values were preserved."
        return
    fi

    local template="$SCRIPT_DIR/stage.env.example"
    if [[ ! -f "$template" ]]; then
        template="$SCRIPT_DIR/docker/env/stage.env.example"
    fi
    [[ -f "$template" && -r "$template" ]] || fail \
        "Copy docker/env/stage.env.example beside this script as stage.env.example or prepare stage.env."

    # Do not execute the environment file or overwrite a concurrently created one.
    (umask 077; set -C; cat -- "$template" > "$ENV_FILE")
    echo "Created stage.env with owner-only permissions."
    echo "Fill Oracle settings, MinIO credentials and PUBLIC_HOST before starting."
}

check_compose() {
    command -v docker >/dev/null 2>&1 || fail "Docker CLI is not available."
    docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 is not available."
    [[ -f "$COMPOSE_FILE" ]] || fail "Compose file not found: $COMPOSE_FILE"
    [[ -f "$ENV_FILE" && -r "$ENV_FILE" ]] || fail "stage.env is not a readable file."
    configure_nginx
    compose config --quiet
}

require_runtime_jar() {
    [[ -f "$APP_JAR" && -r "$APP_JAR" ]] || fail \
        "Runtime JAR is missing or unreadable: $APP_JAR"
    LC_ALL=C grep -aFq \
        'BOOT-INF/classes/com/digiaudit/grcone/runtime/StageDatabaseCommand.class' \
        "$APP_JAR" || fail \
        "Runtime JAR lacks the stage database commands. Build and deploy the updated application JAR first."
}

run_db_command() {
    require_runtime_jar
    case "$1" in
        reset)
            compose run --rm --no-deps -e GRCONE_STAGE_DB_INIT_CONFIRMED=RESET \
                app --stage-db-init
            ;;
        check)
            compose run --rm --no-deps app --stage-db-check
            ;;
        schema_copy_check)
            compose run --rm --no-deps app --schema-copy-check
            ;;
    esac
}

confirm_reset() {
    local answer=""
    [[ -t 0 ]] || fail "--reset requires an interactive terminal for destructive confirmation."
    echo "WARNING: --reset permanently removes ALL objects and data in the Oracle schema configured by DB_SCHEMA."
    echo "Flyway will rebuild that schema from the packaged migrations; MinIO files are preserved."
    echo "Stop any other application using this schema before continuing."
    read -r -p "Type RESET to continue: " answer || answer=""
    [[ "$answer" == "RESET" ]] || fail "Reset cancelled; no data was changed."
}

prepare_jar() {
    [[ "${JAR_SOURCE,,}" == *.jar ]] || fail "The selected file must have a .jar extension."
    [[ -f "$JAR_SOURCE" && -r "$JAR_SOURCE" ]] || fail "JAR file is not readable: $JAR_SOURCE"
    [[ ! -d "$APP_JAR" ]] || fail "grcone-app.jar is a directory; provide an executable JAR file."

    if [[ "$JAR_SOURCE" -ef "$APP_JAR" ]]; then
        echo "Using runtime JAR: $APP_JAR"
        return
    fi

    local temporary_jar
    temporary_jar="$(mktemp "$SCRIPT_DIR/.grcone-stage-jar.XXXXXX")"
    if ! cp -- "$JAR_SOURCE" "$temporary_jar" || ! chmod 644 "$temporary_jar" || ! mv -f -- "$temporary_jar" "$APP_JAR"; then
        rm -f -- "$temporary_jar"
        fail "Could not copy the selected JAR; the previous runtime JAR was preserved."
    fi
    echo "Copied selected JAR to: $APP_JAR"
}

main() {
    parse_args "$@"
    cd -- "$SCRIPT_DIR"

    if [[ "$MODE" == "help" ]]; then
        usage
        return
    fi
    if [[ "$MODE" == "init_env" ]]; then
        init_env
        return
    fi
    if [[ ! -e "$ENV_FILE" && ! -L "$ENV_FILE" ]]; then
        if [[ "$MODE" == "up" || "$MODE" == "check" || "$MODE" == "reset" || "$MODE" == "check_config" || "$MODE" == "schema_copy_check" ]]; then
            init_env
        fi
        fail "Configure stage.env, then run the command again (use --init-env to create it)."
    fi

    check_compose
    if [[ "$MODE" == "check_config" ]]; then
        echo "Stage configuration is valid. Oracle connectivity was not checked."
        return
    fi
    sudo docker info >/dev/null 2>&1 || fail "Docker engine is unavailable or the current user cannot access it."
    select_existing_project

    case "$MODE" in
        check)
            run_db_command check
            ;;
        schema_copy_check)
            run_db_command schema_copy_check
            ;;
        reset)
            require_runtime_jar
            confirm_reset
            compose --profile nginx down
            run_db_command reset
            mkdir -p -- "$SCRIPT_DIR/data/minio" "$SCRIPT_DIR/data/app/logs"
            if [[ "$DETACH" == "true" ]]; then
                start_services -d
                compose ps --all
                echo "Schema rebuilt and startup requested. Use --logs to check application readiness."
            else
                echo "Schema rebuilt. Starting attached; press Ctrl+C to stop the services."
                start_services
            fi
            ;;
        status)
            echo "Runtime JAR: $APP_JAR"
            echo "MinIO data: $SCRIPT_DIR/data/minio"
            compose ps --all
            ;;
        logs) compose logs --tail 200 -f ;;
        down)
            echo "Stopping stage containers; persistent data will be preserved."
            compose --profile nginx down
            ;;
        up)
            prepare_jar
            mkdir -p -- "$SCRIPT_DIR/data/minio" "$SCRIPT_DIR/data/app/logs"
            echo "MinIO data: $SCRIPT_DIR/data/minio"
            echo "Application logs: $SCRIPT_DIR/data/app/logs"
            if [[ "$DETACH" == "true" ]]; then
                start_services -d --force-recreate
                compose ps --all
                echo "Startup requested. Use --logs to check Oracle connectivity and application readiness."
            else
                echo "Starting attached. Press Ctrl+C to stop the services."
                start_services --force-recreate
            fi
            ;;
    esac
}

main "$@"
