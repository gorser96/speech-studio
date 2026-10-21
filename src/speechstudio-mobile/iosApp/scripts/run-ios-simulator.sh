#!/usr/bin/env bash
# Сборка и запуск iosApp в симуляторе iPhone из командной строки.
#
# Использование:
#   iosApp/scripts/run-ios-simulator.sh [имя симулятора]
#
# Без аргумента выбирается первый доступный iPhone, приоритет — уже
# загруженный симулятор. Переменные CONFIGURATION и DERIVED_DATA можно
# переопределить. Для отладки с breakpoint запускайте схему iosApp в Xcode.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

if ! xcodebuild -version >/dev/null 2>&1; then
    if [ -d /Applications/Xcode.app/Contents/Developer ]; then
        export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
    fi
fi
if ! xcodebuild -version >/dev/null 2>&1; then
    echo "error: полный Xcode не найден. Установите его и выполните:" >&2
    echo "  sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer" >&2
    echo "  sudo xcodebuild -runFirstLaunch" >&2
    exit 1
fi

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
fi
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ]; then
    JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
fi
if [ -z "${JAVA_HOME:-}" ]; then
    echo "error: JDK 25 не найден. Установите его: brew install openjdk@25" >&2
    exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

SIMCTL_OUTPUT="$(xcrun simctl list runtimes 2>&1 || true)"
if printf '%s\n' "$SIMCTL_OUTPUT" | grep -q 'license agreements'; then
    echo "error: лицензия Xcode не принята. Выполните:" >&2
    echo "  sudo xcodebuild -license accept" >&2
    echo "  sudo xcodebuild -runFirstLaunch" >&2
    exit 1
fi
if ! printf '%s\n' "$SIMCTL_OUTPUT" | grep -q 'iOS'; then
    echo "error: не установлен iOS Simulator runtime. Установите его в Xcode:" >&2
    echo "  Xcode → Settings → Components → iOS Simulator" >&2
    exit 1
fi

CONFIGURATION="${CONFIGURATION:-Debug}"
DERIVED_DATA="${DERIVED_DATA:-$ROOT/build/ios-derived-data}"
SIMULATOR_NAME="${1:-${SIMULATOR_NAME:-}}"

if [ -n "$SIMULATOR_NAME" ]; then
    SIMULATOR_LINE="$(xcrun simctl list devices available | grep -m1 -F "$SIMULATOR_NAME" || true)"
else
    SIMULATOR_LINE="$(xcrun simctl list devices booted | grep -m1 -E 'iPhone .+\([0-9A-F-]{36}\)' || true)"
    if [ -z "$SIMULATOR_LINE" ]; then
        SIMULATOR_LINE="$(xcrun simctl list devices available | grep -m1 -E 'iPhone .+\([0-9A-F-]{36}\)' || true)"
    fi
fi

if [ -z "$SIMULATOR_LINE" ]; then
    echo "error: не найден доступный симулятор iPhone. Создайте его в Xcode:" >&2
    echo "  Window → Devices and Simulators → Simulators → +" >&2
    exit 1
fi

SIMULATOR_UDID="$(printf '%s\n' "$SIMULATOR_LINE" | sed -E 's/.*\(([0-9A-Fa-f-]{36})\).*/\1/')"
if [ -z "$SIMULATOR_UDID" ]; then
    echo "error: не удалось определить UDID симулятора из строки: $SIMULATOR_LINE" >&2
    exit 1
fi

echo "Симулятор: $SIMULATOR_LINE"
xcrun simctl boot "$SIMULATOR_UDID" 2>/dev/null || true
# Simulator.app может отсутствовать в установке Xcode; приложение всё равно
# устанавливается и запускается через simctl.
open -a Simulator >/dev/null 2>&1 || true

mkdir -p "$ROOT/build/ios-verification"
LOG="$ROOT/build/ios-verification/xcode-build.log"
set -o pipefail
xcodebuild \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration "$CONFIGURATION" \
    -destination "platform=iOS Simulator,id=$SIMULATOR_UDID" \
    -derivedDataPath "$DERIVED_DATA" \
    CODE_SIGNING_ALLOWED=NO \
    ARCHS=arm64 \
    build 2>&1 | tee "$LOG"

APP="$DERIVED_DATA/Build/Products/$CONFIGURATION-iphonesimulator/iosApp.app"
if [ ! -d "$APP" ]; then
    echo "error: сборка завершилась без $APP; смотрите $LOG" >&2
    exit 1
fi

xcrun simctl install "$SIMULATOR_UDID" "$APP"
echo "Запуск iosApp. Ctrl+C отсоединяет консоль, приложение остается запущенным."
exec xcrun simctl launch --console-pty "$SIMULATOR_UDID" com.logopedplus.iosApp
