#!/usr/bin/env bash

set -u
set -o pipefail

PROJECT_NAME="grcone-test"
COMPOSE_ENV=()
COMPOSE_FILE="compose.test.yml"
APP_JAR="grcone-app.jar"
SELECTED_JAR_FILE=".grcone-selected-jar.txt"
MODE="up"
RESET_DATA="false"
DETACH="false"
JAR_SOURCE=""
JAR_SOURCE_NAME=""

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$SCRIPT_DIR" || exit 1

usage() {
    cat <<'EOF_USAGE'

============================================================
GRCONE Test Environment Runner
============================================================

Default behavior is ATTACHED mode.
If no jar is supplied, ./grcone-app.jar is used.
Oracle data is stored in a Docker named volume managed by Compose.
MinIO data and application logs are stored under ./data.

Attached mode - default:
  ./grcone-app.test.sh
  ./grcone-app.test.sh grcone-app.jar
      Equivalent commands. Press Ctrl+C to stop the environment.

Detached mode:
  ./grcone-app.test.sh --detach
  ./grcone-app.test.sh grcone-app.jar --detach

Reset all test data and start attached:
  ./grcone-app.test.sh --reset
  ./grcone-app.test.sh grcone-app.jar --reset
      Shows a destructive-reset warning and requires y/yes confirmation.
      If confirmed, removes the Oracle named volume, MinIO data, app logs,
      and any legacy ./data/oracle bind-mount data before starting again.

Reset and start detached:
  ./grcone-app.test.sh --reset --detach
      Uses the same confirmation before deleting any persistent data.

Use another jar:
  ./grcone-app.test.sh grcone-app-1.1.0.jar
  ./grcone-app.test.sh grcone-app-1.1.0.jar --reset
  ./grcone-app.test.sh grcone-app-1.1.0.jar --detach

Management:
  ./grcone-app.test.sh --status
  ./grcone-app.test.sh --logs
  ./grcone-app.test.sh --check-schema-copy
  ./grcone-app.test.sh --down
  ./grcone-app.test.sh --help

--down preserves persistent data, including the Oracle named volume.
--reset destroys persistent test data only after explicit confirmation.

URLs:
  App URL:       http://localhost:8080
  Oracle:        localhost:1521 / FREEPDB1 / GRCONE
  MinIO API:     http://localhost:9000
  MinIO Console: http://localhost:9001

EOF_USAGE
}

parse_args() {
    while (($# > 0)); do
        case "$1" in
            --help|help|-h)
                MODE="help"
                ;;
            --status|status|--ps)
                MODE="status"
                ;;
            --check-schema-copy)
                MODE="schema_copy_check"
                ;;
            --logs|logs)
                MODE="logs"
                ;;
            --down|down)
                MODE="down"
                ;;
            --reset|reset)
                RESET_DATA="true"
                ;;
            --detach|detach)
                DETACH="true"
                ;;
            *)
                if [[ -n "$JAR_SOURCE" ]]; then
                    echo "ERROR: Only one jar file can be selected."
                    echo "First jar:  $JAR_SOURCE"
                    echo "Second arg: $1"
                    echo
                    usage
                    return 1
                fi
                if [[ -e "$1" ]]; then
                    JAR_SOURCE="$1"
                    JAR_SOURCE_NAME="$(basename -- "$1")"
                else
                    echo "ERROR: Unknown argument or file not found: $1"
                    echo
                    usage
                    return 1
                fi
                ;;
        esac
        shift
    done

    if [[ "$MODE" == "up" && -z "$JAR_SOURCE" ]]; then
        JAR_SOURCE="$APP_JAR"
        JAR_SOURCE_NAME="$APP_JAR"
    fi
}

compose() {
    docker compose "${COMPOSE_ENV[@]}" -f "$COMPOSE_FILE" -p "$PROJECT_NAME" "$@"
}

select_test_installation() {
    local candidate selected=""
    if [[ -n "${GRCONE_TEST_PROJECT:-}" ]]; then
        PROJECT_NAME="$GRCONE_TEST_PROJECT"
    else
        for candidate in grcone-test digigrcone-test grcpc-test; do
            if docker volume inspect "${candidate}_oracle-data" >/dev/null 2>&1; then
                if [[ -n "$selected" ]]; then
                    echo "ERROR: Multiple Test volumes exist. Select one with GRCONE_TEST_PROJECT."
                    return 1
                fi
                selected="$candidate"
            fi
        done
        if [[ -z "$selected" ]]; then
            for candidate in digigrcone-test grcpc-test; do
                if docker container inspect "${candidate}-oracle" >/dev/null 2>&1; then
                    echo "ERROR: Found a previous Test Oracle container with a different storage layout. Verify its mount before upgrading."
                    return 1
                fi
            done
            if [[ -d data/oracle && -n "$(find data/oracle -mindepth 1 -maxdepth 1 -print -quit)" ]]; then
                echo "ERROR: Found legacy Oracle files in data/oracle. Migrate the existing bind mount before starting this version."
                return 1
            fi
        fi
        PROJECT_NAME="${selected:-$PROJECT_NAME}"
    fi

    export GRCONE_TEST_PROJECT="$PROJECT_NAME"
    case "$PROJECT_NAME" in
        grcpc-test) export GRCONE_TEST_DB_USER=GRCPC GRCONE_TEST_DB_PASSWORD=grcpc ;;
        digigrcone-test) export GRCONE_TEST_DB_USER=DIGIGRCONE GRCONE_TEST_DB_PASSWORD=digigrcone ;;
    esac
    if [[ -f test.env ]]; then
        if [[ -f .env ]]; then COMPOSE_ENV+=(--env-file .env); fi
        COMPOSE_ENV+=(--env-file test.env)
    fi
}

check_docker() {
    echo "Checking Docker CLI..."
    if ! command -v docker >/dev/null 2>&1; then
        echo "ERROR: Docker CLI is not available."
        return 1
    fi

    echo "Checking Docker Compose plugin..."
    if ! docker compose version >/dev/null 2>&1; then
        echo "ERROR: Docker Compose v2 plugin is not available."
        return 1
    fi

    echo "Checking Docker engine..."
    if ! docker info >/dev/null 2>&1; then
        echo "ERROR: Docker engine is not running or the current user cannot access it."
        echo "Start Docker or run with a user that can execute 'docker info'."
        return 1
    fi

    echo "Docker engine is ready."
}

prepare_dirs() {
    mkdir -p data/minio data/app/logs
}

prepare_jar() {
    if [[ "${JAR_SOURCE,,}" != *.jar ]]; then
        echo "ERROR: The selected file is not a .jar file: $JAR_SOURCE"
        return 1
    fi

    if [[ ! -f "$JAR_SOURCE" ]]; then
        echo "ERROR: Jar file not found: $SCRIPT_DIR/$JAR_SOURCE"
        return 1
    fi

    local source_full target_full
    source_full="$(cd -- "$(dirname -- "$JAR_SOURCE")" 2>/dev/null && printf '%s/%s' "$PWD" "$(basename -- "$JAR_SOURCE")")"
    target_full="$SCRIPT_DIR/$APP_JAR"

    echo
    echo "Selected jar: $JAR_SOURCE"
    echo "Executable runtime jar: $APP_JAR"

    if [[ "$source_full" == "$target_full" ]]; then
        echo "Selected jar is already the executable runtime jar."
    else
        echo "Copying selected jar to runtime file..."
        cp -f -- "$JAR_SOURCE" "$APP_JAR" || return 1
    fi

    printf '%s\n' "$JAR_SOURCE_NAME" > "$SELECTED_JAR_FILE"
}

load_selected_jar_name() {
    SELECTED_JAR_NAME="$APP_JAR"
    if [[ -f "$SELECTED_JAR_FILE" ]]; then
        IFS= read -r SELECTED_JAR_NAME < "$SELECTED_JAR_FILE" || true
    fi
    [[ -n "${SELECTED_JAR_NAME:-}" ]] || SELECTED_JAR_NAME="$APP_JAR"
}

print_jar_info() {
    load_selected_jar_name
    echo "Executable jar name: $SELECTED_JAR_NAME"
    echo "Runtime mounted jar: $APP_JAR"
    echo "Runtime jar path:   $SCRIPT_DIR/$APP_JAR"
    if [[ -f "$APP_JAR" ]]; then
        echo "Runtime jar size:   $(wc -c < "$APP_JAR" | tr -d ' ') bytes"
    else
        echo "Runtime jar status: NOT FOUND"
    fi
}

is_environment_running() {
    [[ -n "$(compose ps --status running -q 2>/dev/null | head -n 1)" ]]
}

confirm_reset() {
    local answer=""

    printf '\n\033[33m%s\033[0m\n' "WARNING: RESET WILL PERMANENTLY DELETE ALL TEST DATA."
    printf '\033[33m%s\033[0m\n' "  - Oracle database named volume"
    printf '\033[33m%s\033[0m\n' "  - MinIO object data"
    printf '\033[33m%s\033[0m\n' "  - Application logs"
    printf '\033[33m%s\033[0m\n' "  - Legacy data/oracle directory, if present"
    printf '\033[33m%s\033[0m\n\n' "This operation cannot be undone."

    read -r -p "Continue with RESET? [y/N]: " answer || answer=""
    case "${answer,,}" in
        y|yes)
            return 0
            ;;
        *)
            echo
            echo "Reset cancelled. No persistent data was changed."
            return 2
            ;;
    esac
}

reset_data() {
    echo
    echo "Reset confirmed. Stopping containers and deleting all persistent test data..."
    compose down -v --remove-orphans || return $?
    rm -rf -- data/oracle data/minio data/app
    prepare_dirs
}

compose_status() {
    echo
    echo "============================================================"
    echo "GRCONE Test Environment Status"
    echo "============================================================"
    echo
    print_jar_info
    echo
    if is_environment_running; then
        echo "Environment status: RUNNING"
    else
        echo "Environment status: NOT RUNNING"
    fi
    echo
    echo "Docker Compose services:"
    compose ps --all
    echo
    echo "Docker Compose volumes:"
    compose config --volumes
}

compose_down() {
    echo "Stopping GRCONE test environment (persistent data will be preserved)..."
    compose down --remove-orphans
}

compose_logs() {
    compose logs -f
}

wait_for_app_port() {
    local address port i
    address="$(compose port app 8080)" || return 1
    port="${address##*:}"
    [[ "$port" =~ ^[0-9]+$ ]] || return 1
    echo "Waiting for the application on port $port..."
    for ((i = 1; i <= 180; i++)); do
        if timeout 2 bash -c '
            exec 3<>/dev/tcp/127.0.0.1/"$1" || exit 1
            printf "GET /api/setup/status HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3
            read -r status <&3 && [[ "$status" == HTTP/*" 200 "* ]]
        ' _ "$port" >/dev/null 2>&1; then
            echo "Application is ready."
            return 0
        fi
        echo "Waiting for app... $i/180"
        sleep 2
    done
    echo "ERROR: Application did not become ready on port $port. See --logs."
    return 1
}

ATTACHED_CLEANED_UP="false"
cleanup_attached() {
    [[ "$ATTACHED_CLEANED_UP" == "true" ]] && return 0
    ATTACHED_CLEANED_UP="true"
    echo
    echo "Attached session ended. Ensuring GRCONE services are stopped..."
    compose down --remove-orphans >/dev/null 2>&1 || true
    echo "GRCONE test environment is stopped. Persistent data was preserved."
}

start_attached() {
    echo "Starting in ATTACHED mode."
    echo "Press Ctrl+C to stop the environment."
    echo

    ATTACHED_CLEANED_UP="false"
    trap 'exit 130' INT TERM HUP
    trap 'cleanup_attached' EXIT

    compose up --force-recreate --remove-orphans app
    local rc=$?

    trap - INT TERM HUP EXIT
    cleanup_attached
    return "$rc"
}

start_detached() {
    echo "Starting in DETACHED mode..."
    compose up -d --force-recreate --remove-orphans app || return 1
    wait_for_app_port || return 1
    echo
    echo "GRCONE test environment is running in DETACHED mode."
}

main() {
    if [[ "$MODE" == "help" ]]; then
        usage
        return 0
    fi

    if [[ "$MODE" != "up" && "$DETACH" == "true" ]]; then
        echo "ERROR: --detach is only valid when starting the environment."
        return 1
    fi
    if [[ "$MODE" != "up" && "$RESET_DATA" == "true" ]]; then
        echo "ERROR: --reset is only valid when starting the environment."
        return 1
    fi
    if [[ ! -f "$COMPOSE_FILE" ]]; then
        echo "ERROR: Compose file not found: $SCRIPT_DIR/$COMPOSE_FILE"
        return 1
    fi

    check_docker || return $?
    select_test_installation || return $?

    case "$MODE" in
        status) compose_status; return $? ;;
        down) compose_down; return $? ;;
        logs) compose_logs; return $? ;;
        schema_copy_check)
            compose run --rm --no-deps --entrypoint java app \
                -jar /opt/grcone/grcone-app.jar --schema-copy-check
            return $?
            ;;
        up) ;;
        *) echo "ERROR: Unsupported mode: $MODE"; return 1 ;;
    esac

    if [[ "$RESET_DATA" == "true" ]]; then
        confirm_reset || return $?
        reset_data || return $?
    fi

    prepare_dirs || return $?
    prepare_jar || return $?

    echo
    echo "============================================================"
    echo "Starting GRCONE test environment"
    echo "============================================================"
    echo
    print_jar_info
    if [[ "$DETACH" == "true" ]]; then
        echo "Run mode:          DETACHED"
    else
        echo "Run mode:          ATTACHED"
    fi
    echo "Oracle data:       Docker named volume managed by Compose"
    echo "MinIO data:        $SCRIPT_DIR/data/minio"
    echo "Application logs:  $SCRIPT_DIR/data/app/logs"
    echo

    if [[ "$DETACH" == "true" ]]; then
        start_detached
    else
        start_attached
    fi
}

parse_args "$@" || exit $?
main
exit $?
