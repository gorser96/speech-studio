# Инструкция AI-агенту: сборка и отладка Android-приложения в WSL

## Цель и схема окружения

Работай с Android-проектом «Кабинет речи» в существующем окружении. **Не предлагай переносить проект с диска Windows в файловую систему Linux**: текущая рабочая копия намеренно находится на `/mnt/d`.

- **ОС для разработки:** Ubuntu в WSL.
- **Корень Android-проекта:** `/mnt/d/projects/slt-plus/src/slt-mobile`
- **OpenCode:** Linux-версия внутри WSL.
- **Java:** OpenJDK 17.
- **Gradle:** используй Gradle Wrapper проекта (`./gradlew`); сейчас ожидается Gradle 9.6.0.
- **Android SDK для сборки Linux Gradle:** `$HOME/Android/Sdk`
- **Android Studio и Android Emulator:** Windows.
- **ADB для Windows-эмулятора:** `/mnt/c/Users/Gorser/AppData/Local/Android/Sdk/platform-tools/adb.exe`
- **Application ID базовый:** `com.logoped_plus` (именно его устанавливает Android Studio в Windows).
- **Application ID для сборки из WSL:** `com.logoped_plus.wsl` — получается добавлением `-PwslDebug` к `assembleDebug` (в `app/build.gradle.kts` для debug-сборки при этом свойстве задаётся `applicationIdSuffix = ".wsl"`). Сборка **без** флага должна оставаться `com.logoped_plus` — это пакет Android Studio.
- **compileSdk / targetSdk:** 37.
- **minSdk:** 34.
- **Модуль приложения:** `:app`.
- **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk`

Проект использует Kotlin, Jetpack Compose, Room и KSP. Перед изменением Gradle-конфигурации проверяй фактические файлы проекта: версии и настройки могли измениться.

## Обязательные правила безопасности

1. Не перемещай проект и не предлагай делать это без прямого запроса пользователя.
2. Не запускай `git reset --hard`, `git clean -fd`, `git checkout --`, массовое удаление файлов или другие потенциально разрушительные команды без явного разрешения.
3. Перед существенными изменениями проверь `git status --short`. Не перезаписывай пользовательские изменения.
4. Не удаляй приложение с эмулятора командой `adb uninstall` без предупреждения: удаление обычно стирает локальные данные приложения.
5. Если установка APK выдаёт `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, это обычно означает несовпадение подписи установленной и новой сборок. Сначала предупреди о потере данных при удалении приложения и запроси согласие. Не удаляй автоматически.
6. Не редактируй `local.properties` без необходимости. Этот файл локальный, не должен попадать в Git и должен указывать на Linux SDK: `sdk.dir=/home/<linux-user>/Android/Sdk` (используй фактический `$HOME`, не подставляй имя пользователя наугад).
7. Не используй Windows SDK-путь в Linux Gradle для сборки: Linux-инструменты SDK должны находиться в `"$HOME/Android/Sdk"`.
8. Не меняй `compileSdk`, `targetSdk`, `minSdk`, версии Gradle/AGP или подпись приложения только для обхода ошибки, не выяснив причину.
9. Не утверждай, что приложение установлено, запущено или тесты пройдены, пока соответствующая команда не подтвердила результат.
10. Не выводи и не публикуй секреты, ключи подписи, токены или содержимое файлов с учётными данными.

## Подготовка окружения

Команды выполнять в терминале Ubuntu/WSL.

```bash
cd /mnt/d/projects/slt-plus/src/slt-mobile

java -version
echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"
test -x ./gradlew && echo "Gradle Wrapper найден"
test -d "$HOME/Android/Sdk" && echo "Linux Android SDK найден"
```

Ожидается Java 17 и SDK в `$HOME/Android/Sdk`. Если переменные окружения не заданы, проверь `~/.bashrc`. Типичная конфигурация:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Не добавляй повторяющиеся строки в `~/.bashrc`. Если путь или версия отличаются, сначала проверь фактическое состояние системы.

`local.properties` должен указывать на Linux SDK. Пример значения, сформированного из текущего `$HOME`:

```properties
sdk.dir=/home/<linux-user>/Android/Sdk
```

Не коммить `local.properties`.

## Стандартная сборка

Из корня модуля `src/slt-mobile`:

```bash
cd /mnt/d/projects/slt-plus/src/slt-mobile
./gradlew assembleDebug -PwslDebug
```

Флаг `-PwslDebug` обязателен для установки на общий с Android Studio эмулятор: debug-кеystore Linux и Windows отличаются, поэтому APK с одинаковым ID `com.logoped_plus` не могут обновляться поверх друг друга без удаления (потери данных). Отдельный ID `com.logoped_plus.wsl` позволяет WSL-сборке обновляться через `install -r` с сохранением собственных данных и не конфликтовать с приложением Android Studio. Не убирай флаг и не меняй суффикс «для чистоты»: это согласованная схема окружения.

Если нужна более подробная диагностика:

```bash
./gradlew assembleDebug -PwslDebug --stacktrace
```

Проверь ID собранного APK перед установкой (ожидается `com.logoped_plus.wsl`):

```bash
AAPT=$(ls "$ANDROID_HOME"/build-tools/*/aapt 2>/dev/null | sort -V | tail -1)
"$AAPT" dump badging app/build/outputs/apk/debug/app-debug.apk | grep '^package:'
```

Для проверки unit-тестов:

```bash
./gradlew testDebugUnitTest
```

Android instrumentation-тесты требуют подключённого совместимого устройства/эмулятора:

```bash
./gradlew connectedDebugAndroidTest
```

Не запускай все тесты автоматически, если задача пользователя требует только сборку. При ошибке сначала классифицируй её (Java/Gradle, SDK, зависимости, Kotlin/KSP, тесты), затем используй целевую диагностику. Не скрывай ошибку повторными попытками без изменений.

## Проверка APK

После успешной сборки:

```bash
test -f app/build/outputs/apk/debug/app-debug.apk \
  && ls -lh app/build/outputs/apk/debug/app-debug.apk
```

Если файл не найден, не продолжай установку; проверь фактический вариант сборки и путь к APK.

## Windows ADB и эмулятор

Используй Windows ADB из WSL по этому пути:

```bash
ADB_WIN="/mnt/c/Users/Gorser/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

Проверь наличие файла и список устройств:

```bash
test -f "$ADB_WIN" || echo "Windows ADB не найден по ожидаемому пути"
"$ADB_WIN" devices
```

Нормальный пример:

```text
List of devices attached
emulator-5554   device
```

Не предполагай, что серийный номер всегда `emulator-5554`. Возьми его из фактического вывода `adb devices`. Если список пуст:

1. Попроси пользователя запустить AVD в Windows Android Studio → Tools → Device Manager.
2. Дождись полной загрузки эмулятора.
3. Повтори `"$ADB_WIN" devices`.
4. Если устройств по-прежнему нет, попроси проверить команду в Windows PowerShell:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   ```
5. Не переустанавливай SDK и не меняй сетевые настройки WSL до проверки этих базовых шагов.

Для удобства в одной сессии можно определить переменные:

```bash
ADB_WIN="/mnt/c/Users/Gorser/AppData/Local/Android/Sdk/platform-tools/adb.exe"
DEVICE="emulator-5554" # заменить на фактический serial из adb devices
```

Используй `-s "$DEVICE"` для всех команд, если подключено несколько устройств.

## Установка APK

Устанавливай только после успешной сборки с `-PwslDebug` и проверки, что устройство имеет статус `device`:

```bash
"$ADB_WIN" -s "$DEVICE" install -r \
  app/build/outputs/apk/debug/app-debug.apk
```

`-r` пытается обновить приложение, сохраняя данные, но Android отклонит обновление при несовпадении подписи. Установленный WSL-пакет — `com.logoped_plus.wsl`; пакет Android Studio `com.logoped_plus` не трогай.

### Если получено INSTALL_FAILED_UPDATE_INCOMPATIBLE

Сообщение вида:

```text
INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.logoped_plus.wsl signatures do not match newer version
```

означает, что уже установленная версия WSL-пакета подписана другим ключом (например, ранее собиралась без `-PwslDebug` или на другой машине).

- Не запускай `uninstall` автоматически.
- Объясни, что удаление приложения обычно удаляет его локальные данные.
- Сначала уточни, важны ли данные установленной версии.
- Только если пользователь согласен потерять данные, выполни:
  ```bash
  "$ADB_WIN" -s "$DEVICE" uninstall com.logoped_plus.wsl
  "$ADB_WIN" -s "$DEVICE" install \
    app/build/outputs/apk/debug/app-debug.apk
  ```
- Если данные нужно сохранить, остановись и исследуй происхождение APK/ключ подписи. Не обещай сохранить данные без совместимой подписи или проверенного способа миграции.

## Запуск приложения

Package name WSL-сборки:

```text
com.logoped_plus.wsl
```

Пакет Android Studio (`com.logoped_plus`) на том же эмуляторе — отдельное приложение; запускать и проверять его по этой инструкции не нужно.

Попробуй запустить стартовую Activity через Android Monkey:

```bash
"$ADB_WIN" -s "$DEVICE" shell monkey -p com.logoped_plus.wsl 1
```

Проверь установленный пакет:

```bash
"$ADB_WIN" -s "$DEVICE" shell pm list packages \
  | grep -F 'com.logoped_plus.wsl'
```

Команда `monkey` подтверждает отправку стартового события, но сама по себе не гарантирует, что UI работает без ошибок. Для проверки смотри экран эмулятора и `logcat`.

Если пакет не стартует, выясни launchable Activity и изучи `AndroidManifest.xml`, не выдумывая имя Activity.

## Просмотр логов (logcat)

Сбросить буфер логов перед воспроизведением проблемы:

```bash
"$ADB_WIN" -s "$DEVICE" logcat -c
```

Затем воспроизвести проблему в приложении и получить логи:

```bash
"$ADB_WIN" -s "$DEVICE" logcat -d -v threadtime
```

Фильтрация наиболее полезных сообщений:

```bash
"$ADB_WIN" -s "$DEVICE" logcat -d -v threadtime \
  | grep -E 'AndroidRuntime|FATAL EXCEPTION|com\.logoped_plus|Room|SQLite|Kotlin|Exception|Error'
```

Для наблюдения в реальном времени:

```bash
"$ADB_WIN" -s "$DEVICE" logcat -v threadtime
```

Эта команда продолжает работать, пока её не остановят `Ctrl+C`. Для поиска падения найди `FATAL EXCEPTION`, стек вызовов и первую строку, указывающую на код приложения. Не делай вывод по одному сообщению без контекста.

Если вывод слишком большой, сохрани его во временный файл и анализируй релевантный фрагмент. Не добавляй логи с персональными данными пользователей в репозиторий.

## Рекомендуемый цикл работы AI-агента

Для задачи, затрагивающей приложение:

1. Прочитай применимые инструкции репозитория (`AGENTS.md`, README, архитектурную документацию и связанные файлы).
2. Проверь `git status --short`; не трогай посторонние изменения.
3. Исследуй существующий код и воспроизведи/локализуй проблему, если это возможно.
4. Внеси минимальное целевое изменение.
5. Выполни подходящую проверку, обычно `./gradlew assembleDebug -PwslDebug`; для логики дополнительно запускай подходящие тесты.
6. Если нужно проверить приложение на эмуляторе, проверь ADB → установи APK → запусти приложение → собери `logcat`.
7. Сопоставь результат с исходной проблемой. Если проверка не выполнена, явно скажи почему; не называй её пройденной.
8. В итоговом сообщении кратко укажи изменённые файлы, выполненные команды и реальные результаты проверок, а также оставшиеся риски.

## Быстрые команды

Из `/mnt/d/projects/slt-plus/src/slt-mobile`:

```bash
# Сборка (флаг -PwslDebug обязателен: ID com.logoped_plus.wsl, не конфликтует с Android Studio)
./gradlew assembleDebug -PwslDebug

# Unit-тесты
./gradlew testDebugUnitTest

# Проверка устройств
"$ADB_WIN" devices

# Установка APK (укажи реальный DEVICE)
"$ADB_WIN" -s "$DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk

# Запуск
"$ADB_WIN" -s "$DEVICE" shell monkey -p com.logoped_plus.wsl 1

# Логи после воспроизведения
"$ADB_WIN" -s "$DEVICE" logcat -d -v threadtime
```

**Принцип:** сохраняй существующую архитектуру окружения — исходники и Gradle в WSL, Android Studio/эмулятор в Windows, Windows ADB для взаимодействия с эмулятором. Не перестраивай эту схему без явного запроса пользователя.
