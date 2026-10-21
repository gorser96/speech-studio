# iosApp

SwiftUI-оболочка «SpeechStudio» поверх общего Compose Multiplatform-модуля `:shared`.

## Что осталось по OpenSpec

В [cross-platform-kmp](../../../openspec/changes/archive/2026-10-21-cross-platform-kmp/tasks.md) задачи 5.1, 5.2 и 6.2 отмечены выполненными явным решением пользователя 2026-10-21, но ручные UI-сценарии и приёмка на физическом устройстве фактически не выполнялись. Код оболочки и адаптеров добавлен.

На macOS arm64 подготовлена среда сборки: JDK 25, Android SDK 37, `local.properties`, исполняемый бит `gradlew`. Xcode 27.1 установлен: iOS klib, статический `Shared.framework` и приложение для Apple Silicon-симулятора собираются. iOS Simulator runtime 27.0/27.1 установлен; приложение запущено в симуляторе, экран «Расписание» отрисован без демо-данных, `children.db` создана со схемой v4, `iosSimulatorArm64Test` выполнил 41 тест без ошибок. Ниже — порядок воспроизведения и оставшиеся ручные сценарии.

## 1. Подготовить macOS

- Нужен **Apple Silicon (M1/M2/M4 и т. п.)**. В Terminal выполните `uname -m`: ожидается `arm64`. Для Intel Mac (`x86_64`) в проекте отсутствует `iosX64`; до запуска симулятора понадобится отдельное изменение таргетов и KSP.
- Установите полный Xcode, откройте его и завершите установку дополнительных компонентов. В **Xcode → Settings → Components** (в некоторых версиях — Platforms) установите iOS Simulator runtime и создайте симулятор iPhone через **Window → Devices and Simulators**. Deployment target приложения — iOS 15.0; для первой проверки подойдёт доступный более новый runtime.
- Версию Xcode сверяйте с [совместимостью Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform/compatibility-guide.html): в проекте Kotlin **2.2.10** и Compose Multiplatform **1.10.3**. Сам по себе deployment target 15.0 не гарантирует совместимость с любым Xcode 15+. Если подходящий Xcode не работает на установленной macOS, понадобится совместимая версия macOS/Xcode или отдельное обновление зависимостей проекта.
- Установите JDK **25** для Gradle: эта версия закреплена в `gradle/gradle-daemon-jvm.properties`. Gradle умеет скачать daemon JDK автоматически, но установленная JDK нужна для запуска wrapper и удобна для Xcode:

  ```bash
  brew install openjdk@25
  ```

  Формула keg-only, поэтому `java` не появляется в PATH и `/usr/libexec/java_home` может её не видеть. Фаза **Compile Kotlin Framework** сама ищет `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`. Для терминала задайте `JAVA_HOME` явно.

- Установите Android SDK: **Android SDK Platform 37**, Platform-Tools и Build-Tools. Gradle конфигурирует также Android-модули; iOS-сборке нужен только этот этап конфигурации, эмулятор Android не требуется:

  ```bash
  brew install --cask android-commandlinetools
  yes | sdkmanager --sdk_root="$HOME/Library/Android/sdk" --licenses
  yes | sdkmanager --sdk_root="$HOME/Library/Android/sdk" --install \
    "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
  ```

После установки Xcode выполните:

```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
sudo xcodebuild -license accept
sudo xcodebuild -runFirstLaunch
xcodebuild -downloadPlatform iOS
xcodebuild -version
xcrun simctl list runtimes
xcrun simctl list devices available
```

`xcodebuild -downloadPlatform iOS` скачивает iOS Simulator runtime, если он не установлен вместе с Xcode. После этого создайте симулятор iPhone в **Xcode → Window → Devices and Simulators** или через `xcrun simctl create`.

`sudo xcode-select --switch` не обязателен для скрипта: он сам подставляет `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`, если системный toolchain указывает на Command Line Tools. Лицензию и первые компоненты всё равно нужно принять один раз через `sudo`.

Если Xcode установлен под другим именем, подставьте его путь. Понадобится интернет для первой загрузки Gradle, Maven-зависимостей и Kotlin/Native; само приложение работает локально.

## 2. Проверить проект и Gradle

Полная реализация KMP находится в ветке `main`; отдельный перенос рабочего дерева с Windows больше не нужен. Служебные каталоги `.gradle/`, `.kotlin/`, `build/` и `local.properties` не коммитятся.

Из `src/speechstudio-mobile`:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Library/Android/sdk"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
./gradlew --version
```

`local.properties` создаётся на каждой машине и не попадает в Git. Убедитесь, что Gradle сообщает JVM 25. Файл `gradlew` хранится в Git с режимом `100755`; если при переносе исполняемый бит потерян, выполните `chmod +x gradlew` и закоммитьте режим.

Фаза **Compile Kotlin Framework** в Xcode сама находит JDK 25 и добавляет его в `JAVA_HOME`/`PATH`, включая keg-only установку Homebrew. Поэтому Xcode из Dock не требует переменных окружения из терминала. `ENABLE_USER_SCRIPT_SANDBOXING = NO` уже установлено.

## 3. Выполнить Kotlin/Native-проверки

На macOS klib-проверки и линковка уже прошли: `:shared:compileKotlinIosSimulatorArm64`, `:shared:compileKotlinIosArm64`, `:shared:compileTestKotlinIosSimulatorArm64` и `:shared:linkDebugFrameworkIosSimulatorArm64` — `BUILD SUCCESSFUL` с Xcode 27.1. `:shared:iosSimulatorArm64Test` выполнил 41 тест без падений и пропусков.

```bash
mkdir -p build/ios-verification
set -o pipefail
./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64 --stacktrace 2>&1 | tee build/ios-verification/kotlin-compile.log
```

Тесты запускаются командой; они должны реально выполниться, а не быть `SKIPPED`:

```bash
set -o pipefail
./gradlew :shared:iosSimulatorArm64Test --stacktrace 2>&1 | tee build/ios-verification/kotlin-tests.log
```

Отчёт ищите в `shared/build/reports/tests/iosSimulatorArm64Test/`, XML-результаты — в `shared/build/test-results/iosSimulatorArm64Test/`. Если Gradle не может выбрать симулятор, проверьте `./gradlew help --task :shared:iosSimulatorArm64Test` и доступные устройства через `xcrun simctl list devices available`; запустите нужный симулятор в Xcode.

`iosSimulatorArm64Test` выполняет общие тесты и iOS-проверки на bundled SQLite:

- `IosDatabasePersistenceTest` — переоткрытие файла БД сохраняет ребёнка, занятие, участника и видео; миграции 1→4 и 3→4 сохраняют детей, занятия, снимки имён, видео и создают таблицы тэгов/расписания;
- `IosSettingsRepositoryTest` — значения `NSUserDefaults` читаются новым экземпляром репозитория, то есть переживают пересоздание контейнера.

Эти тесты не заменяют ручной UI-проход: выбор файла через document picker, открытие в системном просмотрщике, открепление и возвраты проверяются вручную.

## 4. Собрать и запустить приложение

Для этой и следующих секций нужен полный Xcode и установленный iOS Simulator runtime. В проекте проверены сборка для generic-назначения с `ARCHS=arm64`, запуск на Apple Silicon-симуляторе и первый пустой старт.

`Info.plist` задан явно — `iosApp/iosApp/Info.plist`, `GENERATE_INFOPLIST_FILE = NO`. В нём обязательный для Compose Multiplatform ключ `CADisableMinimumFrameDurationOnPhone = true`: без него `ComposeUIViewController` аварийно завершает приложение с `IllegalStateException`. Произвольный `INFOPLIST_KEY_CADisableMinimumFrameDurationOnPhone` Xcode 27 молча игнорирует, поэтому ключ хранится в отдельном файле.

Из `src/speechstudio-mobile`:

```bash
open iosApp/iosApp.xcodeproj
```

В Xcode выберите схему **iosApp**, конфигурацию **Debug** и конкретный симулятор iPhone. Нажмите **Cmd+R**. Фаза **Compile Kotlin Framework** вызывает `:shared:embedAndSignAppleFrameworkForXcode`, собирает статический `Shared` и подготавливает общие Compose-ресурсы. Не запускайте эту Gradle-задачу отдельно от Xcode без его переменных окружения. CocoaPods для текущего проекта не требуется.

Для сборки, установки и запуска одной командой (без breakpoint, с консолью):

```bash
iosApp/scripts/run-ios-simulator.sh
# или с конкретным симулятором:
iosApp/scripts/run-ios-simulator.sh "iPhone 17"
```

Скрипт сам находит JDK 25, выбирает загруженный или первый доступный iPhone, пишет лог в `build/ios-verification/xcode-build.log` и запускает приложение через `simctl`. Для пошаговой отладки используйте Xcode.

Для воспроизводимой командной сборки сначала получите список назначений:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations
```

Замените `SIMULATOR_UDID` в следующей команде на ID выбранного iPhone:

```bash
set -o pipefail
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=SIMULATOR_UDID' -derivedDataPath build/ios-derived-data CODE_SIGNING_ALLOWED=NO ARCHS=arm64 build 2>&1 | tee build/ios-verification/xcode-build.log
```

Для `-destination 'generic/platform=iOS Simulator'` параметр `ARCHS=arm64` обязателен: иначе Xcode собирает универсальную архитектуру с `x86_64`, а таргета `iosX64` в проекте нет, и `syncComposeResourcesForIos` падает.

Ожидается `BUILD SUCCEEDED`. Для симулятора Apple Developer Team не нужна. Xcode 27.1 читает проект, линкует `Shared` и включает Compose-ресурсы в bundle; при изменении `project.pbxproj` или фазы сборки снова проверяйте лог, а не считайте успех ожидаемым.

Для запуска уже собранного приложения с консолью:

```bash
xcrun simctl boot "$SIMULATOR_UDID" 2>/dev/null || true
open -a Simulator 2>/dev/null || true
xcrun simctl install "$SIMULATOR_UDID" build/ios-derived-data/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch --console-pty "$SIMULATOR_UDID" com.logopedplus.iosApp
```

`open -a Simulator` необязателен: в проверенной установке Xcode 27.1 отдельного `Simulator.app` нет, но `simctl` устанавливает и запускает приложение без него. Скриншоты и консоль при этом доступны через `simctl io … screenshot` и `--console-pty`.

На Xcode 27.1 runtime iOS 27.1 поддерживает только устройство **iPhone Duo**, и его headless-скриншоты получаются чёрными. Для визуальной проверки используйте runtime **iOS 27.0** с **iPhone 18 Pro** (или другой обычный iPhone): на нём экран «Расписание» отрисовывается корректно. Это ограничение среды, а не приложения; чёрный кадр сам по себе не доказывает падение.

Для отладки с breakpoint всё же используйте **Cmd+R** в Xcode: он подключает LLDB и показывает стек Swift/Kotlin. Командный запуск с `--console-pty` удобен для чтения `println`, исключений и логов падения; `Ctrl+C` отсоединяет консоль.

Для первого запуска используйте новый симулятор без данных этого приложения. Ожидается пустое расписание и пустой список детей, без демонстрационных записей; это уже подтверждено на iPhone 18 Pro (iOS 27.0), БД создаётся в `Library/Application Support/children.db`. После создания данных для проверок перезапуска приложение не удаляйте: удаление очищает локальную БД. В Xcode **Stop → Run** завершает процесс и запускает его заново; простого ухода на домашний экран недостаточно для этой проверки.

## 5. Проверить сценарии и отладку

1. Создайте ребёнка, тэг и занятие с комментарием. Измените «Скрывать выходные», диапазон на 08:00–21:00 и число показываемых тэгов. Завершите процесс и запустите снова: записи, связи и настройки должны сохраниться, неделя — показывать пять дней и выбранный диапазон.
2. Проверьте добавление/редактирование и отмену черновика, карточку ребёнка, шаблон и генерацию занятий. Экранная стрелка должна вести редактор → просмотр → список → расписание; из «Тэгов»/«Настроек» — в расписание. Во время записи переходы и повторные действия блокируются. На корневом расписании нет «Назад» и диалога выхода; уход в фон выполняется системным жестом.
3. Откройте drawer: логотип должен отображаться без падения. Проверьте светлую/тёмную тему, безопасные области и клавиатуру в полях, портретную/горизонтальную ориентации. С английским системным языком даты и сортировка остаются русскими. Повторите основные операции без сети, используя локальные файлы.
4. Подготовьте два небольших локальных `.mov`/`.mp4` в приложении «Файлы». На физическом iPhone можно сохранить их из «Фото» через «Поделиться → Сохранить в Файлы». В занятии выберите несколько видео, сохраните, завершите процесс и снова откройте ссылки: системный просмотрщик должен работать без повторного выбора. На iOS кнопка автопоиска по времени отсутствует.
5. Отмените выбор файла: связи не меняются. Открепите видео и сохраните: связь исчезает, исходный файл остаётся в «Файлах». Для другого вложения удалите отдельную тестовую копию исходного файла и попробуйте открыть ссылку: ожидается понятная ошибка, ссылка остаётся и доступна для открепления. При частичном отказе выбора доступные файлы добавляются, показано число ошибок; если воспроизвести такой отказ не удалось, отметьте сценарий как непроверенный.

Для отладки запускайте через **Cmd+R** с включённым **Debug executable** в Edit Scheme → Run. Ошибки сборки доступны в **Report Navigator (Cmd+9)**, сообщения процесса — в **Debug Area (Shift+Cmd+Y)**. При падении сохраните полный текст исключения/stack trace и шаги воспроизведения. Можно поставить breakpoint в `ContentView.swift` на создание контроллера; полноценная отладка Kotlin требует отдельной настройки совместимого Kotlin-инструментария, Swift-breakpoint её не заменяет.

Чтобы посмотреть БД симулятора, после сохранения данных и остановки приложения получите его контейнер (при одном запущенном симуляторе):

```bash
xcrun simctl get_app_container booted com.logopedplus.iosApp data
```

В выведенном каталоге БД расположена в `Library/Application Support/children.db`. Для анализа копируйте также `children.db-wal` и `children.db-shm`, если они существуют. Первичный запуск пустой БД не проверяет миграции старых версий; миграции 1→4 и 3→4 покрыты `IosDatabasePersistenceTest`, но не заменяют приёмку на устройстве.

## 6. Проверить на настоящем iPhone

Задача 6.2 требует физического устройства для выбора файлов и системного просмотрщика. Подключите iPhone к Mac, подтвердите доверие, включите **Developer Mode**, если его требует версия iOS. В **Xcode → Settings → Accounts** добавьте Apple Account; в **iosApp → Signing & Capabilities** выберите Team и при необходимости уникальный Bundle Identifier. Выберите iPhone как назначение и нажмите **Cmd+R**. Для локальной отладки обычно подходит Personal Team; публикация в App Store в эти задачи не входит.

Повторите на устройстве видео-сценарии из раздела 5, включая открытие после завершения процесса, открепление и недоступный файл. Проверьте сохранение данных после перезагрузки устройства. Успех на новой iOS не подтверждает отдельно запуск на минимальной iOS 15 — указывайте фактически проверенные версии.

## 7. Зафиксировать результат

В [verification](../../../openspec/verification/README.md) запишите модель Mac и архитектуру, версии macOS/Xcode/JDK, runtime симулятора и модель/iOS физического устройства, команды, число реально выполненных тестов, результат сборки и сценариев, ссылки на логи. Обнаруженные ошибки указывайте с шагами воспроизведения; недоступные проверки оставляйте открытыми.

Критерии закрытых задач: 5.1 — сборка и первый пустой запуск; 5.2 — сохранность данных/настроек/видео и возвраты; 6.2 — native-тесты и приёмка на физическом устройстве (iOS-миграции покрыты тестами). После выполнения ручных сценариев обновите статус в [verification](../../../openspec/verification/README.md), где они отмечены как непроверенные.
