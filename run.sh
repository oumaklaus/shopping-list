#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"
BACKEND_DIR="$ROOT_DIR/backend"
ANDROID_DIR="$ROOT_DIR/android"
PID_FILE="$RUN_DIR/backend.pid"
LOG_FILE="$RUN_DIR/backend.log"
APK="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
MONGO_CONTAINER="shopping-list-mongo"
PORT="${PORT:-8080}"
MONGODB_DB="${MONGODB_DB:-shopping_list}"

mkdir -p "$RUN_DIR"

log() {
    printf '%s\n' "$*"
}

fail() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

process_cmdline() {
    local pid="$1"
    if [ -r "/proc/$pid/cmdline" ]; then
        tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true
    else
        ps -p "$pid" -o command= 2>/dev/null || true
    fi
}

wait_for_exit() {
    local pid="$1"
    local _
    for _ in $(seq 1 25); do
        kill -0 "$pid" 2>/dev/null || return 0
        sleep 0.2
    done
    kill -9 "$pid" 2>/dev/null || true
}

stop_backend() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid="$(cat "$PID_FILE" 2>/dev/null || true)"
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            if process_cmdline "$pid" | grep -qE 'shopping[-]?list|com\.example\.shoppinglist'; then
                log "Stopping previous backend (PID $pid)..."
                kill "$pid" 2>/dev/null || true
                wait_for_exit "$pid"
            else
                log "Ignoring stale Shopping List PID file for unrelated PID $pid."
            fi
        fi
        rm -f "$PID_FILE"
    fi

    local port_pid=""
    if command -v ss >/dev/null 2>&1; then
        port_pid="$(ss -ltnp 2>/dev/null \
            | grep -E "[:.]${PORT}[[:space:]]" \
            | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' \
            | head -n 1 || true)"
    elif command -v lsof >/dev/null 2>&1; then
        port_pid="$(lsof -ti "tcp:${PORT}" 2>/dev/null | head -n 1 || true)"
    fi

    if [ -n "$port_pid" ] && kill -0 "$port_pid" 2>/dev/null; then
        if process_cmdline "$port_pid" | grep -qE 'shopping[-]?list|com\.example\.shoppinglist'; then
            log "Stopping old backend on port $PORT (PID $port_pid)..."
            kill "$port_pid" 2>/dev/null || true
            wait_for_exit "$port_pid"
        else
            fail "Port $PORT is in use by PID $port_pid. Stop that process, then run ./run.sh again."
        fi
    fi
}

start_mongo() {
    if [ -n "${MONGODB_URI:-}" ]; then
        log "Using the configured MongoDB URI (Atlas or another external server)."
        return 0
    fi

    export MONGODB_URI="mongodb://localhost:27017"
    if ! command -v docker >/dev/null 2>&1; then
        if (echo >/dev/tcp/127.0.0.1/27017) >/dev/null 2>&1; then
            log "MongoDB is already running on localhost:27017."
            return 0
        fi
        fail "Docker is not installed and MongoDB is not running on localhost:27017."
    fi

    if docker inspect "$MONGO_CONTAINER" >/dev/null 2>&1; then
        if [ "$(docker inspect -f '{{.State.Running}}' "$MONGO_CONTAINER" 2>/dev/null)" != "true" ]; then
            log "Starting MongoDB container..."
            docker start "$MONGO_CONTAINER" >/dev/null
        else
            log "MongoDB container is already running."
        fi
    else
        log "Creating MongoDB container..."
        if [ "$(uname -s)" = "Linux" ]; then
            docker run -d --name "$MONGO_CONTAINER" --network host \
                -v shopping_list_mongo_data:/data/db mongo:7.0 \
                --bind_ip 127.0.0.1 >/dev/null
        else
            docker run -d --name "$MONGO_CONTAINER" -p 127.0.0.1:27017:27017 \
                -v shopping_list_mongo_data:/data/db mongo:7.0 >/dev/null
        fi
    fi

    log "Waiting for MongoDB..."
    local _
    for _ in $(seq 1 60); do
        if docker exec "$MONGO_CONTAINER" mongosh --quiet \
            --eval 'quit(db.adminCommand({ping:1}).ok ? 0 : 1)' >/dev/null 2>&1; then
            log "MongoDB is ready."
            return 0
        fi
        sleep 1
    done
    fail "MongoDB did not become ready. Check: docker logs $MONGO_CONTAINER"
}

start_backend() {
    stop_backend

    log "Building the backend..."
    (cd "$BACKEND_DIR" && ./gradlew installDist --console=plain)

    local bin="$BACKEND_DIR/build/install/shopping-list-backend/bin/shopping-list-backend"
    [ -x "$bin" ] || fail "Backend executable not found at $bin"

    : > "$LOG_FILE"
    log "Starting the backend on http://localhost:$PORT ..."
    (
        cd "$BACKEND_DIR"
        if command -v setsid >/dev/null 2>&1; then
            setsid env \
                PORT="$PORT" \
                MONGODB_URI="$MONGODB_URI" \
                MONGODB_DB="$MONGODB_DB" \
                "$bin" >>"$LOG_FILE" 2>&1 < /dev/null &
        else
            PORT="$PORT" \
            MONGODB_URI="$MONGODB_URI" \
            MONGODB_DB="$MONGODB_DB" \
            nohup "$bin" >>"$LOG_FILE" 2>&1 &
        fi
        echo $! > "$PID_FILE"
    )

    local backend_pid
    backend_pid="$(cat "$PID_FILE")"
    local _
    for _ in $(seq 1 60); do
        if curl -sf -m 3 "http://localhost:$PORT/items" >/dev/null 2>&1; then
            log "Backend is ready."
            return 0
        fi
        if ! kill -0 "$backend_pid" 2>/dev/null; then
            break
        fi
        sleep 1
    done

    log "Backend log:"
    tail -n 30 "$LOG_FILE" >&2 || true
    fail "Backend did not become ready. See $LOG_FILE"
}

find_adb() {
    if [ -n "${ADB:-}" ] && [ -x "$ADB" ]; then
        printf '%s\n' "$ADB"
        return 0
    fi
    if command -v adb >/dev/null 2>&1; then
        command -v adb
        return 0
    fi
    local sdk
    for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Android/Sdk"; do
        if [ -n "$sdk" ] && [ -x "$sdk/platform-tools/adb" ]; then
            printf '%s\n' "$sdk/platform-tools/adb"
            return 0
        fi
    done
    return 1
}

start_app() {
    local cloud_url="${1:-}"
    local adb
    if ! adb="$(find_adb)"; then
        fail "adb was not found. Install Android SDK platform-tools or set ANDROID_HOME."
    fi

    local device="${ANDROID_SERIAL:-}"
    local candidates=""
    if [ -z "$device" ]; then
        candidates="$("$adb" devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
        local candidate
        for candidate in $candidates; do
            if [ "$("$adb" -s "$candidate" shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')" != "1" ]; then
                device="$candidate"
                break
            fi
        done
        if [ -z "$device" ] && [ -n "$candidates" ]; then
            device="$(printf '%s\n' "$candidates" | head -n 1)"
        fi
    fi

    if [ -z "$device" ]; then
        local pending
        pending="$("$adb" devices | awk 'NR > 1 && $1 != "" { print $1 " (" $2 ")"; exit }')"
        if [ -n "$pending" ]; then
            fail "Device $pending is not ready. Unlock the phone and accept the USB debugging prompt."
        fi
        log "No Android device detected."
        log "Connect your phone with USB debugging enabled, unlock it, accept the"
        log "'Allow USB debugging' prompt, then run ./run.sh again."
        return 1
    fi

    local status
    status="$("$adb" devices | awk -v d="$device" '$1 == d { print $2; exit }')"
    if [ "$status" != "device" ]; then
        fail "Device $device is not ready (state: ${status:-not found}). Unlock the phone and accept the USB debugging prompt."
    fi

    local is_emulator api_base_url
    is_emulator="$("$adb" -s "$device" shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r' || true)"
    if [ -n "$cloud_url" ]; then
        api_base_url="${cloud_url%/}/"
        log "Using cloud API: $api_base_url"
    elif [ "$is_emulator" = "1" ]; then
        api_base_url="http://10.0.2.2:${PORT}/"
        log "Using emulator networking: $api_base_url"
    else
        log "Forwarding port $PORT to the phone with adb reverse..."
        "$adb" -s "$device" reverse "tcp:$PORT" "tcp:$PORT" >/dev/null \
            || fail "adb reverse failed. Reconnect the phone and try again."
        api_base_url="http://127.0.0.1:${PORT}/"
    fi

    log "Building the Android app..."
    (cd "$ANDROID_DIR" && ./gradlew assembleDebug -PapiBaseUrl="$api_base_url" --console=plain)

    [ -f "$APK" ] || fail "APK not found at $APK"
    log "Installing the app on $device..."
    "$adb" -s "$device" install -r "$APK"

    log "Launching the app..."
    "$adb" -s "$device" shell am start -n com.example.shoppinglist/.MainActivity >/dev/null

    log ""
    log "Done. The app is open on your phone."
    if [ "$is_emulator" != "1" ] && [ -z "$cloud_url" ]; then
        log "Keep the phone connected by USB while testing; adb reverse carries the backend connection."
    fi
}

case "${1:-all}" in
    all|start)
        start_mongo
        start_backend
        start_app
        ;;
    backend)
        start_mongo
        start_backend
        ;;
    app)
        start_app
        ;;
    cloud)
        cloud_url="${2:-${API_BASE_URL:-}}"
        [ -n "$cloud_url" ] || fail "Usage: ./run.sh cloud https://YOUR-APP.azurewebsites.net"
        start_app "$cloud_url"
        ;;
    stop)
        stop_backend
        log "Backend stopped. MongoDB is still running."
        ;;
    *)
        log "Usage: ./run.sh [all|backend|app|cloud URL|stop]"
        exit 1
        ;;
esac
