#!/usr/bin/env bash
# Hot reload for the dev stack (CMD from Dockerfile.dev). Do not run manually.
#
# ./src and ./ are mounted read-only (docker-compose.dev.yml). This script:
#   - starts the app with `spring-boot:run` in its own process group,
#   - watches src/main and /workspace/pom.xml with inotifywait,
#   - when src/main changes, compiles (`-o compile`) and touches target/classes/.reloadtrigger,
#     which causes DevTools to restart (SPRING_DEVTOOLS_RESTART_TRIGGER_FILE),
#   - when pom.xml changes, copies it, compiles (downloading anything missing), and restarts the Java process,
#   - if anything fails, leaves the previous version running, prints a banner, and creates
#     /tmp/hot-reload/failed, which the healthcheck uses to mark the container `unhealthy`.
#
# See specs/24-hot-reload-dev-sin-watch.md.

set -u

APP_DIR=/app
SRC_DIR=$APP_DIR/src/main
JAVA_DIR=$SRC_DIR/java
RESOURCES_DIR=$SRC_DIR/resources
CLASSES_DIR=$APP_DIR/target/classes
WORKSPACE_POM=/workspace/pom.xml
APP_POM=$APP_DIR/pom.xml
STATE_DIR=/tmp/hot-reload
FAILED=$STATE_DIR/failed
MVN_OUT=$STATE_DIR/mvn.log
POM_BACKUP=$STATE_DIR/pom.xml.bak
EVENTS=modify,close_write,create,delete,moved_to,moved_from
DEBUG_JVM_ARGS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'

APP_PID=
MVN_PID=

log() {
    echo "[hot-reload] $*"
}

app_running() {
    [[ -n $APP_PID ]] && kill -0 -- "-$APP_PID" 2>/dev/null
}

start_app() {
    log "Starting the app"
    # setsid: Maven and the child JVM run in their own process group, which can be stopped as a whole.
    setsid ./mvnw spring-boot:run -Dmaven.test.skip=true \
        "-Dspring-boot.run.jvmArguments=$DEBUG_JVM_ARGS" &
    APP_PID=$!
}

stop_app() {
    app_running || return 0
    log "Stopping the app"
    kill -TERM -- "-$APP_PID" 2>/dev/null
    for _ in $(seq 40); do
        app_running || break
        sleep 0.2
    done
    app_running && kill -KILL -- "-$APP_PID" 2>/dev/null
    wait "$APP_PID" 2>/dev/null
    APP_PID=
}

on_signal() {
    log "Signal received, stopping"
    [[ -n $MVN_PID ]] && kill "$MVN_PID" 2>/dev/null
    stop_app
    exit 0
}

# Maven runs in the background with `wait` so SIGTERM is handled without waiting for it to finish.
# $1 = offline | online. Output is written to $MVN_OUT.
compile() {
    local args=(-q)
    [[ $1 == offline ]] && args+=(-o)
    ./mvnw "${args[@]}" compile -Dmaven.test.skip=true >"$MVN_OUT" 2>&1 &
    MVN_PID=$!
    wait "$MVN_PID"
    local rc=$?
    MVN_PID=
    return $rc
}

# $1 = banner title
fail() {
    touch "$FAILED"
    echo "[hot-reload] ==================== $1 ===================="
    grep -F '[ERROR]' "$MVN_OUT" || cat "$MVN_OUT"
    if app_running; then
        log "The app is still running with the previous version. Fix the error and save."
    else
        log "The app is not running. Fix the error and save."
    fi
    echo "[hot-reload] ================================================================"
}

ignored() {
    local name=${1##*/}
    [[ $name == .* || $name == *~ || $name == *.swp || $name == *.swx || $name == *.tmp \
        || $name == *___jb_tmp___ || $name == *___jb_old___ ]]
}

# An incremental compile does not remove .class files of deleted or renamed sources.
remove_output() {
    local path=$1 rel
    [[ -e $path ]] && return 0 # recreated in the same batch
    case $path in
        "$JAVA_DIR"/*)
            rel=${path#"$JAVA_DIR"/}
            [[ -n $rel ]] || return 0
            if [[ $rel == *.java ]]; then
                rm -f "$CLASSES_DIR/${rel%.java}.class" "$CLASSES_DIR/${rel%.java}"\$*.class
            else
                rm -rf "${CLASSES_DIR:?}/$rel"
            fi
            ;;
        "$RESOURCES_DIR"/*)
            rel=${path#"$RESOURCES_DIR"/}
            [[ -n $rel ]] || return 0
            rm -rf "${CLASSES_DIR:?}/$rel"
            ;;
        *) return 0 ;;
    esac
    log "Removed output for $rel"
}

on_src_change() {
    if compile offline; then
        rm -f "$FAILED"
        touch "$CLASSES_DIR/.reloadtrigger"
        log "Compilation OK"
        app_running || start_app
    else
        fail "COMPILATION FAILED"
    fi
}

on_pom_change() {
    log "pom.xml changed, resolving dependencies and compiling"
    cp "$APP_POM" "$POM_BACKUP"
    cp "$WORKSPACE_POM" "$APP_POM"
    if compile online; then
        rm -f "$FAILED"
        log "Compilation OK"
        stop_app
        start_app
    else
        cp "$POM_BACKUP" "$APP_POM"
        fail "POM DEPENDENCIES FAILED"
    fi
}

# State of the current event batch
POM_CHANGED=0
SRC_CHANGED=0
DELETED=()

handle_event() {
    local events=$1 path=$2
    case $path in
        "$WORKSPACE_POM")
            POM_CHANGED=1
            return
            ;;
        "$JAVA_DIR"/* | "$RESOURCES_DIR"/*) ;;
        *) return ;;
    esac
    ignored "$path" && return
    if [[ $path == "$JAVA_DIR"/* && $events != *ISDIR* && $path != *.java ]]; then
        return
    fi
    SRC_CHANGED=1
    if [[ $events == *DELETE* || $events == *MOVED_FROM* ]]; then
        DELETED+=("$path")
    fi
}

process_batch() {
    local path
    for path in "${DELETED[@]}"; do
        remove_output "$path"
    done
    if ((POM_CHANGED)) && ! cmp -s "$WORKSPACE_POM" "$APP_POM"; then
        on_pom_change # already recompiles everything
    elif ((SRC_CHANGED)); then
        on_src_change
    fi
    POM_CHANGED=0
    SRC_CHANGED=0
    DELETED=()
}

trap on_signal TERM INT

cd "$APP_DIR" || exit 1
mkdir -p "$STATE_DIR"
rm -f "$FAILED" # /tmp survives `docker compose restart`

# Watchers start before compiling so changes made during startup are not lost.
# /workspace is watched without -r: only pom.xml matters, and changes under target/ are not seen.
exec 3< <(
    inotifywait -m -r -q -e "$EVENTS" --format '%e %w%f' "$SRC_DIR" &
    inotifywait -m -q -e "$EVENTS" --format '%e %w%f' /workspace &
    wait
)

if ! cmp -s "$WORKSPACE_POM" "$APP_POM"; then
    log "Host pom.xml differs from the image version; copying it"
    cp "$WORKSPACE_POM" "$APP_POM"
fi

# Compiling before starting lets us show the banner on failure; spring-boot:run then has nothing to recompile.
if compile online; then
    start_app
else
    fail "COMPILATION FAILED"
fi

log "Watching src/main and pom.xml"
while true; do
    if ((POM_CHANGED || SRC_CHANGED)); then
        # Wait for 1 s without new events before processing the batch.
        read -r -t 1 -u 3 events path
        rc=$?
        if ((rc > 128)); then
            process_batch
            continue
        fi
    else
        read -r -u 3 events path
        rc=$?
    fi
    if ((rc != 0)); then
        log "inotifywait exited unexpectedly"
        stop_app
        exit 1
    fi
    handle_event "$events" "$path"
done
