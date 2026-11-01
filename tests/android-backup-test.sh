#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT_DIR/android/app/build/outputs/apk/debug/app-debug.apk"
APK_ANALYZER="${ANDROID_HOME:-$HOME/Android/Sdk}/cmdline-tools/latest/bin/apkanalyzer"

(cd "$ROOT_DIR/android" && ./gradlew assembleDebug --console=plain >/dev/null)

if ! "$APK_ANALYZER" manifest print "$APK" | grep -Fq 'android:allowBackup="false"'; then
    printf 'Expected the packaged application to disable credential backup.\n' >&2
    exit 1
fi

printf 'packaged manifest: credential backup disabled\n'
