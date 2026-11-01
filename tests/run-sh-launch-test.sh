#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

ADB_LOG="$TEMP_DIR/adb.log"
cat > "$TEMP_DIR/adb" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$ADB_LOG"
if [ "${1:-}" = "devices" ]; then
    printf 'List of devices attached\nfake-device\tdevice\n'
elif [[ "$*" == *"shell getprop ro.kernel.qemu"* ]]; then
    printf '1\n'
fi
EOF
chmod +x "$TEMP_DIR/adb"

ADB="$TEMP_DIR/adb" ADB_LOG="$ADB_LOG" ANDROID_SERIAL="fake-device" \
    "$ROOT_DIR/run.sh" app >/dev/null

if ! grep -Fq 'shell am start -n com.example.shoppinglist/.LoginActivity' "$ADB_LOG"; then
    printf 'Expected run.sh to launch LoginActivity. adb calls were:\n' >&2
    cat "$ADB_LOG" >&2
    exit 1
fi

printf 'run.sh launcher: LoginActivity\n'
