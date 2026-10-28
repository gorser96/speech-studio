# Инструкции для ИИ-агента

Мобильное приложение «SpeechStudio» — Kotlin Multiplatform (Android + iOS). Все Gradle-команды выполняются из `src/speechstudio-mobile/`.

## Идентификаторы и устройство проекта

- Kotlin package и Android `namespace` — `com.speech_studio`; `applicationId` — `com.logoped_plus` (activity `com.logoped_plus.MainActivity`), файл БД — `children.db`, iOS bundle id — `com.logopedplus.iosApp`. Не переименовывай исходники под `applicationId`.
- `shared` — основной код: `commonMain` (domain/data/ui/Compose), `androidMain` и `iosMain` — платформенные `actual`-адаптеры. `app` — Android-оболочка и Android-only `MediaStoreVideoScanner`. `iosApp` — SwiftUI/Xcode-оболочка.
- DI собирается вручную в `com.speech_studio.app.AppContainer` (Android — `createAppContainer`, iOS — `createIosAppContainer`). Hilt, Navigation Compose и отдельный use case на каждую операцию не внедрены.
- Room v5; KSP экспортирует схемы в `shared/schemas`. Android использует прежний `children.db` и системный SQLite, iOS — bundled SQLite и Application Support.
- `ARCHITECTURE.md` и `описание проекта.md` описывают в том числе планы. Источник истины — код и Gradle-файлы.

## Сборка и проверки

- JDK 25 закреплён в `gradle/gradle-daemon-jvm.properties` и берётся из `JAVA_HOME`; команда `java` может отсутствовать в PATH. `local.properties` задаёт Android SDK. Эмулятор, установка и ручные проверки — `TESTING.md`.
- `.\gradlew.bat :app:assembleDebug` — сборка Android; `.\gradlew.bat :app:compileDebugKotlin` — быстрая проверка компиляции общего кода.
- Основные тесты domain/ViewModel/UI-логики: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.speech_studio.domain.text.RussianTextTest"`. Полный набор — при широком влиянии изменения.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.speech_studio.data.local.LocalDatabaseIntegrationTest"` — Android/Room/Robolectric-тесты.
- `:app:connectedDebugAndroidTest` — только при существенном Android/Room/UI-риске и запущенном эмуляторе. По состоянию на 2026-10-28 в отчёте `hour-grid-ux` отмечено, что `app/src/androidTest` не компилируется после перехода на `kotlinx-datetime`; без фактического запуска не считай их пройденными.
- iOS на Windows: klib собираются только с `-Pkotlin.native.enableKlibsCrossCompilation=true`; Xcode-сборка, запуск и `:shared:iosSimulatorArm64Test` — только на macOS (`iosApp/README.md`).

## OpenSpec

- Изменение поведения — по OpenSpec. Перед проектированием и реализацией прочитай `ARCHITECTURE.md`, затем `openspec/specs/` по затронутым возможностям и активное изменение в `openspec/changes/`. Правила — `openspec/config.yaml`.
- Контракты — `openspec/specs/`; выполненные и незавершённые проверки — `openspec/verification/README.md`; завершённые изменения — `openspec/changes/archive/`.
- `openspec` установлен в PATH; если его нет — `npx --yes @fission-ai/openspec@1.13.0 ...`. Проверка: `openspec validate <change> --strict`.
- `openspec/README.md` в репозитории отсутствует, хотя на него ссылаются `config.yaml` и старые инструкции. Не создавай его ради ссылки и не считай отсутствие файла отсутствием процесса.
- Спецификация не доказывает реализацию. Для багфикса сначала проверь, нет ли уже требования; не дублируй его. Не подгоняй требования под ошибочную реализацию.
- Для изменения поведения зафиксируй цель, критерии приёмки, затронутые возможности, решение и задачи, не дублируя сведения между артефактами. Существенные неоднозначности уточняй до реализации зависимой части.
- Запрос спецификации или плана не означает запрос реализации; следуй выбранному пользователем этапу. При разрешённом полном цикле продолжай по этапам.
- Документы пиши по-русски, служебные заголовки OpenSpec сохраняй. Не создавай дополнительные исследования и чек-листы без нужды. Для документации, опечаток и рефакторинга без изменения поведения полный цикл не нужен.

## Поиск кода

- Начни с `PACKAGES.md`: выбери пакет по задаче и открой его `PACKAGE.md`. Карта может отставать, поэтому проверяй фактический код.
- Заголовки `PACKAGE.md` местами используют старые имена `com.logoped_plus.*`, хотя реальные package — `com.speech_studio.*`. Ориентируйся на путь и объявление `package`.
- `rg` на этой машине может отсутствовать в PATH. Используй инструмент поиска OpenCode или `Get-ChildItem`/`Select-String`; исключай `build`, `app/build`, `.gradle`, `.kotlin`, `.idea`, `.git` и не правь сгенерированные классы.
- Прослеживай цепочку: экран → callback/действие → ViewModel → интерфейс repository → реализация → модель. Проверяй вызывающий код и потребителей изменяемого контракта. Перед переходом в другой пакет прочитай его `PACKAGE.md`.
- Для расписания обязательно учитывай `shared/src/commonMain/kotlin/com/speech_studio/ui/screen/ScheduleScreen.kt` (маршрутизация форм и черновики) и пакет `ui/screen/schedule`.
- Если поиск по пакету пуст, расширяй до `shared/src/commonMain`, `shared/src`, `app/src`; отсутствие сущности в документации не доказывает отсутствие кода. Тесты — `shared/src/commonTest`, `app/src/test`, `app/src/androidTest`; ссылки на существующие тесты есть в `PACKAGE.md`. Поведение платформы — в `AndroidManifest.xml` и `res`; сборка — `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `shared/build.gradle.kts`, `gradle/libs.versions.toml`.

## Карта пакетов

- Для каждого реально объявленного Kotlin/Java package поддерживай `PACKAGE.md` рядом с исходниками. Родительские каталоги без объявления `package` не описывай.
- При добавлении, переносе или удалении файлов обновляй таблицу файлов, связи и ограничения; при изменении набора пакетов — `PACKAGES.md`.
- Package в `main`/`test`/`androidTest` описывается одной картой в `main` со ссылками на тесты. Package только в тестах — в соответствующем тестовом каталоге и в индексе.
- Пиши по-русски, сохраняй точные имена символов и относительные ссылки, не привязывайся к номерам строк. Явно отмечай нереализованное.

## Выбор проверок

- Сначала используй существующие адресные тесты. Новый тест нужен для непокрытого существенного бизнес-правила, воспроизводимой регрессии или риска потери/повреждения данных.
- Проверяй на самом дешёвом достаточном уровне. Не дублируй один сценарий в repository, ViewModel и UI; не тестируй геттеры, делегирование и детали реализации. TDD, квоты покрытия и тест на каждый класс не требуются.
- Room и миграции проверяй с реальной БД; UI — только при существенном риске. Для визуальной правки достаточно адресной ручной проверки; для документации — проверки ссылок и `git diff --check`. Охват package проверяй при изменении карты или структуры.
- Сохраняй полезные тесты. Удаляй только доказанно дублирующие или устаревшие проверки в рамках задачи.
- В ответе укажи, что запускалось, результат и что не запускалось. Недоступная среда не означает успешную проверку; `Example*Test` не подтверждают пользовательские сценарии.
