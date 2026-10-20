# Статус проверок

По одному разделу на изменение: выполненные проверки и незавершённая ручная приёмка.

## cross-platform-kmp

Изменение заархивировано 2026-10-20; выполнены задачи 1.1–5.1 [изменения](../changes/archive/2026-10-20-cross-platform-kmp/tasks.md). iOS-оболочка собирается и запускается в симуляторе с пустой БД; сохранность БД/настроек и миграции покрыты iOS-тестами. Ручные UI-сценарии 5.2 (видео, возвраты) и приёмка на физическом устройстве 6.2 открыты.

### Выполненные проверки

- Базовая линия Android до перехода: `:app:assembleDebug` и `:app:testDebugUnitTest` — BUILD SUCCESSFUL, 36 тестов без сбоев.
- `:shared:compileAndroidMain` и `:app:assembleDebug` — BUILD SUCCESSFUL; domain, Room, настройки, тема, общие экраны и ViewModel компилируются в `commonMain`. `:shared:compileKotlinMetadata` вызывался, но задача SKIPPED: Kotlin/Native-таргеты отключены на Windows, общий код проверяется Android-компиляцией.
- `:shared:testAndroidHostTest` — 37 тестов без сбоев: `RussianTextTest` (5), `RussianNameSortTest` (5), `RecurringLessonPlannerTest` (11), `FindLessonVideosUseCaseTest` (4), `WeekTimelineTest` (8), `LessonValidationTest` (3), `ChildrenViewModelCardTagsTest` (1).
- `:app:testDebugUnitTest` — 13 тестов без сбоев: `LocalDatabaseIntegrationTest` (9, Robolectric sdk 34) и `MediaStoreTimeToMillisTest` (3), `AndroidSettingsRepositoryTest` (1).
- Добавлены проверки миграции 1→4, повторного открытия непустой v4 и отсутствия destructive fallback; чтение прежних ключей `settings` и сохранение изменений.
- Экспортированная схема [shared/schemas/.../4.json](../../src/slt-mobile/shared/schemas/com.logoped_plus.data.local.ChildrenDatabase/4.json) побайтно совпадает с прежней [app/schemas/.../4.json](../../src/slt-mobile/app/schemas/com.logoped_plus.data.local.ChildrenDatabase/4.json), identity hash `7441455d846ab97cdbe9ae33d072969c`.
- Осмотр кода: тема вынесена в `commonMain` с Android actual для dynamic color и iOS actual с общими схемами; логотип drawer — общий `composeResources/drawable/icon1024.png`; системный Back — `PlatformBackHandler` (Android actual, iOS no-op); `showSectionBack` включает экранную стрелку, `onExit = null` на iOS исключает подтверждение выхода; busy-блокировки и черновики остаются в общих ViewModel.
- Карты пакетов обновлены: [PACKAGES.md](../../src/slt-mobile/PACKAGES.md) и `PACKAGE.md` перенесённых пакетов; относительные ссылки проверены скриптом.
- iOS-klib собраны на Windows с `-Pkotlin.native.enableKlibsCrossCompilation=true`: `:shared:compileKotlinIosSimulatorArm64`, `:shared:compileKotlinIosArm64`, `:shared:compileTestKotlinIosSimulatorArm64` — BUILD SUCCESSFUL. Проверена компиляция общего кода и iOS-адаптеров (Room bundled, `NSUserDefaults`, видео, возврат); это не заменяет Xcode-сборку и запуск на macOS.
- Все iOS-klib также собраны на macOS arm64 без Windows-флага: `:shared:compileKotlinIosSimulatorArm64`, `:shared:compileKotlinIosArm64`, `:shared:compileTestKotlinIosSimulatorArm64` — BUILD SUCCESSFUL.
- На macOS arm64 подготовлена среда: JDK 25.0.4.1 (Homebrew), Android SDK Platform 37.0/Build-Tools 37.0.0/Platform-Tools, `local.properties`; `./gradlew --version` сообщает JVM 25. Повторно пройдены `:app:assembleDebug`, `:app:testDebugUnitTest` (13 тестов) и `:shared:testAndroidHostTest` (37 тестов) — без сбоев.
- Xcode-проект проверен структурно: `plutil -lint` и разбор всех object/file references — ошибок нет; `gradlew` хранится с режимом `100755`; фаза **Compile Kotlin Framework** сама находит JDK 25, включая keg-only Homebrew, и вызывает `embedAndSignAppleFrameworkForXcode`. Добавлен [run-ios-simulator.sh](../../src/slt-mobile/iosApp/scripts/run-ios-simulator.sh) для сборки, установки и запуска в симуляторе; проверен `bash -n` и реальной сборкой/запуском.
- `:shared:linkDebugFrameworkIosSimulatorArm64` — BUILD SUCCESSFUL после установки Xcode 27.1 (27A9275); Kotlin 2.2.10 слинковал статический `Shared.framework` без ошибки совместимости, только с предупреждением о неявном bundle ID.
- `xcodebuild -project iosApp/iosApp.xcodeproj -list` читает проект; сборка `xcodebuild -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' -derivedDataPath build/ios-derived-data CODE_SIGNING_ALLOWED=NO ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build` — **BUILD SUCCEEDED**. Фаза **Compile Kotlin Framework** нашла JDK 25, выполнила `syncComposeResourcesForIos` и `assembleDebugAppleFrameworkForXcodeIosSimulatorArm64`; в `iosApp.app` лежат `compose-resources/.../drawable/icon1024.png` и Swift-оболочка. Лог: `build/ios-verification/xcode-build-generic-arm64.log`.
- Без `ARCHS=arm64` сборка на generic-назначении падает в `syncComposeResourcesForIos`: `ARCHS` содержит `x86_64`, а таргета `iosX64` в проекте нет. Это ограничение обхода для универсальной generic-сборки, а не ошибка Xcode-проекта: конкретный Apple Silicon-симулятор и `ARCHS=arm64` собираются.
- После выделения общего `AppContainer` и iOS-оболочки: `:app:assembleDebug`, `:app:testDebugUnitTest` (13 тестов) и `:shared:testAndroidHostTest` (37 тестов) — BUILD SUCCESSFUL, регрессий Android не выявлено.
- Общая зависимость ViewModel — `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.6`: артефакт `androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0` не содержит iOS-вариантов и ломал резолв iOS-таргетов.
- Осмотр кода iOS-видео: `UIDocumentPickerViewController` для `UTType.movie` с множественным выбором и `asCopy = false`; выбранные файлы сохраняются как Base64 security-scoped bookmark, имя берётся из последнего компонента пути; просмотр — `UIDocumentInteractionController` (preview, затем open-in) с освобождением security-scoped доступа после закрытия; ошибки `Missing`/`Permission`/`InvalidLink`/`NoViewer` возвращаются в общий UI; `deviceScan = false`, файл не копируется и не удаляется.
- Осмотр кода iOS-оболочки: общий `AppContainer` принимает БД, настройки и `VideoLibrary`; iOS-фабрика использует Application Support/`children.db`, `BundledSQLiteDriver`, `NSUserDefaults` и пустой источник автопоиска; `MainViewController` оборачивает общий `App` в `ComposeUIViewController`; `iosApp` — SwiftUI + `UIViewControllerRepresentable`, framework `Shared` статический; выход `onExit = null`, экранное «Назад» включено.
- Первый запуск на iPhone 18 Pro (iOS 27.0, Xcode 27.1): приложение открыло пустое «Расписание» без демо-данных, процесс не завершился, в консоли запуска нет исключений. Создана `Library/Application Support/children.db` версии 4 с таблицами `children`, `lessons`, `lesson_participants`, `lesson_videos`, `tags`, `child_tags`, `child_schedule`, `child_schedule_slots`, `room_master_table`; режим WAL.
- Найден и исправлен дефект первого запуска: Compose Multiplatform завершал процесс с `IllegalStateException` из `PlistSanityCheck` без `CADisableMinimumFrameDurationOnPhone`. Xcode 27 игнорирует произвольный `INFOPLIST_KEY_CADisableMinimumFrameDurationOnPhone`, поэтому добавлен явный [Info.plist](../../src/slt-mobile/iosApp/iosApp/Info.plist) и `GENERATE_INFOPLIST_FILE = NO`; в собранном bundle ключ присутствует, `CFBundleIdentifier` и `MinimumOSVersion 15.0` сохранены.
- `:shared:iosSimulatorArm64Test` — BUILD SUCCESSFUL, 41 тест без падений и пропусков: 37 общих и 4 iOS-теста. `IosDatabasePersistenceTest` проверяет на bundled SQLite переоткрытие файла (ребёнок, занятие, участник и URI видео сохранены), миграцию 1→4 (дети сохранены, таблицы занятий/тэгов/расписания созданы) и миграцию 3→4 (занятие, снимки имён, участники и видео сохранены, новые таблицы пусты). `IosSettingsRepositoryTest` проверяет, что новое создание репозитория читает сохранённые значения `NSUserDefaults`. XML-результаты в `shared/build/test-results/iosSimulatorArm64Test/`.
- Ограничение среды: runtime iOS 27.1 в Xcode 27.1 поддерживает только iPhone Duo, его headless-скриншоты чёрные; визуальная проверка выполнена на iOS 27.0/iPhone 18 Pro. Отдельного `Simulator.app` в установке Xcode нет, `simctl` работает без него.
- `openspec validate cross-platform-kmp --strict` — успешно; `git -c core.whitespace=cr-at-eol diff --check` — без ошибок (только предупреждения о CRLF).
- Запуск на эмуляторе Pixel_7a (Android 14, API 34): APK собирался, но падал при старте с `MissingResourceException` для `drawable/icon1024.png` — AGP-KMP не передавал сгенерированные Compose-ресурсы в assets приложения. В `shared/build.gradle.kts` включено `androidResources.enable = true`; после `:app:installDebug` приложение открывает «Расписание», в logcat нет падений. `:app:connectedDebugAndroidTest` по-прежнему не запускался.

### Незавершённые проверки и приёмка

- Инструкция продолжения: [подготовка среды, сборка, симулятор, отладка и iPhone](../../src/slt-mobile/iosApp/README.md). Обновлена 2026-10-20 по фактической macOS-среде; Xcode 27.1, runtime iOS 27.0/27.1, сборка и первый запуск проверены, iOS-тесты БД и настроек проходят. Задача 5.1 закрыта; 5.2 (ручной UI и видео) и 6.2 (физический iPhone) остаются открытыми.
- iOS на macOS: в симуляторе вручную проверить создание ребёнка и занятия через интерфейс, сохранение после перезапуска, выбор нескольких видео через document picker, открытие в системном просмотрщике после перезапуска, открепление без удаления файла, недоступный файл, возвраты и черновики. Сохранность БД/настроек и миграции 1→4 и 3→4 уже покрыты iOS-тестами; автоматизированного ввода в UI нет, поэтому эти сценарии требуют ручного прохода (задача 5.2).
- iOS-дефект вне рамок настройки сборки: в недельной сетке метки часов дублируются (`09:00`, `10:00` и т. д. видны дважды). Причина — в `WeekScheduleView` каждая строка выводит `hour` и `hour + 1`; код общий, поэтому дефект вероятен и на Android. Требует отдельного решения и проверки, в текущем изменении не исправлялся.
- iOS-видео на macOS/устройстве: системный выбор нескольких видео, сохранение bookmark после перезапуска, открытие в системном просмотрщике, открепление без удаления файла, недоступный/отозванный файл, частичный отказ при выборе. На macOS проверены компиляция и осмотр кода; ручной выбор файлов ещё не выполнялся.
- iOS-возврат: экранное «Назад» в заголовке для вложенных экранов и корневых разделов, отсутствие подтверждения выхода; системный жест уводит приложение в фон.
- `:app:connectedDebugAndroidTest` не запускался; установка debug APK и запуск на эмуляторе Pixel_7a (Android 14, API 34) выполнены вручную 2026-10-20, приложение открывается без падений.
- Ручная приёмка Android после перехода: открытие существующей `children.db` v4, сохранение настроек и логотип drawer, навигация/Back, черновики и блокировки записи, Android-видео (SAF, сохранение grant после перезапуска, `ACTION_VIEW`).


## auto-attach-lesson-videos

Реализовано: 2026-09-30.

### Выполненные проверки

- Unit-тесты `:app:testDebugUnitTest` — 13/13 без сбоев, включая [FindLessonVideosUseCaseTest](../../src/slt-mobile/shared/src/commonTest/kotlin/com/logoped_plus/domain/usecase/FindLessonVideosUseCaseTest.kt) (границы интервала, дубликаты, пустой результат, занятие через полночь) и [MediaStoreTimeToMillisTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/data/media/MediaStoreTimeToMillisTest.kt) (нормализация секунд/миллисекунд).
- Сборка `:app:assembleDebug` — успешно.
- `READ_MEDIA_VIDEO` объявлено в `AndroidManifest.xml`.
- Карты обновлены: `PACKAGES.md`, пакеты `domain.model`, `domain.repository`, `domain.usecase` (новый), `data.media` (новый), `com.logoped_plus`, `ui.screen.schedule`, `ui.screen.schedule.component`.
- Ручная проверка на реальном устройстве OnePlus PJE110 (2026-09-30, по явному запросу пользователя; adb по Wi-Fi):
  - Найден и исправлен дефект: OxygenOS хранит `DATE_TAKEN` в миллисекундах, из-за чего видео не находились; фикс — нормализация единиц в `mediaStoreTimeToMillis` (риск зафиксирован в design.md).
  - Полный сценарий из деталей: занятие 30.09, 14:00, 40 мин; два видео, снятые в 14:16 — кнопка «Найти видео занятия» → «Добавлено видео: 2.» → «Сохранить» → обе ссылки (`content://media/external/video/media/1000021947`, `…1948`) записаны в `lesson_videos`, в деталях отображаются без кнопки «Сохранить».

### Ручная приёмка (незавершена)

- Разрешение: отказ в системном запросе → сообщение об ошибке, список не меняется, повторное нажатие повторяет запрос.
- Поиск: отсутствие подходящих видео → сообщение об отсутствии найденных; некорректные дата/время/длительность → сообщение об исправлении значений без поиска.
- Сохранение из редактирования (черновик) и повторный поиск без дублирования ссылок.
- Отзыв разрешения после поиска: открытие прикреплённого видео показывает стандартную ошибку недоступного файла, ссылка сохраняется, открепление доступно.

## scroll-week-schedule-screen

Реализовано: 2026-10-15. Архив: [2026-10-15-scroll-week-schedule-screen](../changes/archive/2026-10-15-scroll-week-schedule-screen/).

### Выполненные проверки

- `:app:testDebugUnitTest --tests "com.logoped_plus.ui.screen.schedule.WeekTimelineTest"` (неинкрементальный запуск, `-Pksp.incremental=false`, из-за смешения путей WSL/Windows в кэше сборки) — BUILD SUCCESSFUL.
- `openspec validate scroll-week-schedule-screen --strict` — успешно.
- `git -c core.whitespace=cr-at-eol diff --check` по затронутым файлам — успешно.
- Осмотр кода: внешняя высота измеряется до вертикальной прокрутки; недельный компонент получает конечную высоту не меньше 420 dp; внутренний скролл часов и минимум 64 dp сохранены; внутри сетки — штатная вложенная прокрутка Compose.

### Ручная приёмка (незавершена)

- В недельном режиме свайпом скрыть верхний переключатель/заголовок и нижнюю кнопку, затем отдельным свайпом прокрутить часы; портретная и горизонтальная ориентации, семь и пять дней. Эмуляторы и установка приложения отключены настройками проекта; приёмка остаётся открытой до разрешения проверки на устройстве.

## delete-child

Реализовано: 2026-10-15. Архив: [2026-10-15-delete-child](../changes/archive/2026-10-15-delete-child/).

### Выполненные проверки

- Сборка `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` + `:app:compileDebugAndroidTestKotlin` (`-Pksp.incremental=false`) — BUILD SUCCESSFUL; KSP экспортировал схему [3.json](../../src/slt-mobile/app/schemas/com.logoped_plus.data.local.ChildrenDatabase/3.json), DDL `MIGRATION_2_3` совпадает с экспортом.
- Unit-тесты `:app:testDebugUnitTest` — 16/16 без сбоев, включая [LessonValidationTest](../../src/slt-mobile/shared/src/commonTest/kotlin/com/logoped_plus/data/local/LessonValidationTest.kt) (занятие только с историческими участниками валидно; пустой список и дубли ID — InvalidData).
- `openspec validate delete-child --strict` — успешно.
- `git -c core.whitespace=cr-at-eol diff --check` по затронутым файлам — успешно.
- Осмотр кода: `ChildDao.deleteOnce` снимает имя участников в той же транзакции, что и удаление; `toDomain` мапит `childName` в `removedName`, `toUiModel` берёт имя из снимка либо по ID; `canSave`/`invalidReason` принимают исторических участников; редактор занятия передаёт их в сохранение без изменений; кнопка удаления — только в форме существующего ребёнка, `deleting` блокирует повтор и закрытие диалога.

### Ручная приёмка (незавершена)

- Обновление установки с данными (миграция v2→v3): дети, занятия, участники, порядок, комментарии и видео сохранены, имена показаны как прежде.
- Удаление ребёнка с занятиями: диалог и блокировки; исчезновение из списка и выбора участников; занятия в месячном, недельном и дневном расписании с именем; после перезапуска ребёнок не восстанавливается, занятия сохранены.
- Редактирование и сохранение занятия с удалённым участником: исторический участник сохранён с записанным именем.
- androidTest `RoomChildRepositoryTest` не запускался (connected-тесты отключены настройками проекта). Эмуляторы и установка приложения отключены; приёмка остаётся открытой до разрешения проверки на устройстве.

## child-card-tags-recurring-schedule

Реализовано: 2026-10-16. Архив: [2026-10-16-child-card-tags-recurring-schedule](../changes/archive/2026-10-16-child-card-tags-recurring-schedule/).

### Выполненные проверки

- Unit-тесты `:app:testDebugUnitTest` (`-Pksp.incremental=false`, из-за смешения путей WSL/Windows в кэше сборки) — 30/30 без сбоев:
  - новые [RecurringLessonPlannerTest](../../src/slt-mobile/shared/src/commonTest/kotlin/com/logoped_plus/domain/usecase/RecurringLessonPlannerTest.kt) (11): пример 8 дат октября 2026 (8, 9, 15, 16, 22, 23, 29, 30), текущее время на последнем дне (15:00:30 → только 16:00; ровно 15:00:00 → обе строки), повтор без дублей и существующее групповое занятие с другой длительностью, конфликты со всем расписанием (16:20–16:50 против 16:00–16:40 пропущен, 16:40–17:10 создан), соприкосновение границ, переход через полночь (23:50+40 против 00:10), пересекающиеся строки шаблона (первая побеждает), одинаковое начало (вторая — «уже существует»), перенесённое занятие освобождает старое время, невалидные шаблоны (нет даты, день 8, длительность ≤ 0, непредставимая).
  - новые [LocalDatabaseIntegrationTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/data/local/LocalDatabaseIntegrationTest.kt) (3) на Robolectric sdk 34, подключённом только как `testImplementation` (release/androidTest зависимости не менялись): миграция v3→v4 непустой БД (ID/позиции/имена/комментарий/URI/снимки исторических имён сохранены, новые таблицы пусты, версия 4); атомарная карточка (переименование + замена тэгов + строки/ordinal, «ghost»-тэг отфильтрован, NotFound для чужого ID), удаление тэга (отвязка, справочник и дети сохранены), удаление ребёнка (каскад child_tags/child_schedule(_slots), занятие со снимком имени); генерация: (создано 1, конфликты 1) с глобальным конфликтом, повтор (0, 1, 1) без дублей, сохранение прежних занятий, rollback пакетной вставки через trigger `RAISE(ABORT)` — частичного набора нет, повтор после снятия причины (4, 0, 0).
  - существующие 16 тестов (LessonValidationTest, MediaStoreTimeToMillisTest, FindLessonVideosUseCaseTest, WeekTimelineTest) без изменений.
- KSP экспортировал схему [4.json](../../src/slt-mobile/app/schemas/com.logoped_plus.data.local.ChildrenDatabase/4.json); DDL `MIGRATION_3_4` совпадает с экспортом (таблицы tags, child_tags, child_schedule, child_schedule_slots, индексы).
- Сборка `:app:assembleDebug` — BUILD SUCCESSFUL.
- Осмотр кода: Back-слои «диалог удаления → редактор карточки → просмотр → список → расписание»; `appBusy` (saving расписания + busy детей + busy тэгов) блокирует drawer, кнопку меню, жесты и системный Back; черновики, запрос поиска и активные операции живут в ViewModel; генерация — только кнопкой из сохранённого просмотра, без автоматического запуска; `addLesson` не меняет разрешение ручных пересечений; поиск/сортировка — чистая проекция (Collator ru-RU, тёмки по исходной позиции).

### Ручная приёмка (незавершена)

- Обновление установки с данными (миграция v3→v4 на устройстве): дети, занятия, порядок, комментарии и видео сохранены, приложение открывается без удаления файла.
- Карточка: выбор тэгов (пустой справочник → подсказка), строки шаблона (повтор дня, пересечения), дата окончания, сохранение/отмена/ошибка, возвраты и блокировки, удаление из карточки; черновик переживает поворот.
- Генерация: счётчики «Создано/уже существуют» (счётчик «конфликты» с 2026-10-16 убран изменением allow-overlapping-generated-lessons), блокировка повторного нажатия и ухода, повтор без дублей, карточка остаётся открытой; пересоздание Activity не запускает вторую операцию.
- Раздел «Тэги»: палитра с названиями и отметкой выбора, пустое/дублирующееся (без регистра) название, переименование/перекрас → актуализация у детей, удаление с отвязкой; возвраты.
- Список детей: поиск с одного символа, «Ничего не найдено» против «Детей пока нет», русская алфавитная сортировка, переименование занимает новое место.
- Эмуляторы и установка приложения отключены конфигурацией проекта; приёмка остаётся открытой до разрешения проверки на устройстве.

## allow-overlapping-generated-lessons

Реализовано: 2026-10-16. Архив: [2026-10-16-allow-overlapping-generated-lessons](../changes/archive/2026-10-16-allow-overlapping-generated-lessons/).

### Выполненные проверки

- Unit-тесты `:app:testDebugUnitTest` (`-Pksp.incremental=false`, из-за смешения путей WSL/Windows в кэше сборки) — 33/33 без сбоев:
  - обновлённые [RecurringLessonPlannerTest](../../src/slt-mobile/shared/src/commonTest/kotlin/com/logoped_plus/domain/usecase/RecurringLessonPlannerTest.kt) (11): пример 8 дат октября 2026 (8, 9, 15, 16, 22, 23, 29, 30), текущее время на последнем дне, повтор без дублей и существующее групповое занятие с другой длительностью, пересечения со всем расписанием создаются (16:20–16:50 и 16:40–17:10 против чужого 16:00–16:40), соприкосновение границ, переход через полночь (23:50+40 против 00:10 — создано), пересекающиеся строки шаблона (обе созданы), одинаковое начало (вторая — «уже существует»), перенесённое занятие, невалидные шаблоны (нет даты, день 8, длительность ≤ 0, непредставимая).
  - обновлённые [LocalDatabaseIntegrationTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/data/local/LocalDatabaseIntegrationTest.kt) (5): генерация (создано 2, уже существуют 0) с пересечением занятия другого ребёнка, повтор (0, 2) без дублей, сохранение прежних занятий, rollback пакетной вставки через trigger `RAISE(ABORT)` — частичного набора нет, повтор после снятия причины (4, 0).
  - [ChildrenViewModelCardTagsTest](../../src/slt-mobile/shared/src/commonTest/kotlin/com/logoped_plus/ui/screen/children/ChildrenViewModelCardTagsTest.kt) (1) — мок генерации с двухсчётчиком `GenerationCounts()`.
  - существующие тесты (LessonValidationTest, MediaStoreTimeToMillisTest, FindLessonVideosUseCaseTest, WeekTimelineTest) без изменений.
- `:app:compileDebugAndroidTestKotlin` (`-Pksp.incremental=false`) — BUILD SUCCESSFUL: обновлён текст результата в [ChildCardTagsUiTest](../../src/slt-mobile/app/src/androidTest/java/com/logoped_plus/ui/screen/ChildCardTagsUiTest.kt).
- Осмотр кода: планировщик не читает все интервалы расписания (`findLessonSpans` удалён из `LessonDao`), `GenerationCounts` — «создано/уже существуют», текст карточки «Создано: N; уже существуют: M»; дедупликация по «ребёнок/точное начало» и порядок кандидатов сохранены; ручное `addLesson` и отображение пересечений в расписании не менялись.
- `openspec validate allow-overlapping-generated-lessons --strict` — успешно.

### Ручная приёмка (незавершена)

- Генерация с пересечениями: кандидат, пересекающийся с занятием другого ребёнка или с другим кандидатом запуска, создаётся; карточка показывает «Создано/уже существуют» без «конфликтов»; повтор без дублей; пересекающиеся строки шаблона создают оба занятия.
- Эмуляторы и установка приложения отключены конфигурацией проекта; приёмка остаётся открытой до разрешения проверки на устройстве.

## fix-week-grid-landscape-scroll

Реализовано: 2026-10-15. Архив: [2026-10-15-fix-week-grid-landscape-scroll](../changes/archive/2026-10-15-fix-week-grid-landscape-scroll/).

### Выполненные проверки

- Осмотр кода: минимальная высота часа применяется ко всем часам, меткам и карточкам; единый Row прокручивается по 24 часам; заголовки вне скролла; начальное смещение — восемь часов; расчёт одинаков для пяти и семи дней.
- `WeekTimelineTest` — 6 тестов, 0 ошибок/падений; повтор с `-Pksp.incremental=false` завершился BUILD SUCCESSFUL.
- `git -c core.whitespace=cr-at-eol diff --check` по затронутым файлам — успешно (без учёта CRLF Git сообщает о существующих Windows-окончаниях строк в `WeekScheduleView.kt`).
- `openspec validate fix-week-grid-landscape-scroll --strict` — успешно.

### Ручная приёмка (незавершена)

- Портретная и горизонтальная ориентации: свайп до начала/конца суток, закреплённые заголовки, открытие занятия, двойное нажатие свободного часа; повтор с пятью днями. Час не ниже 64 dp, длительность 90 минут занимает 1,5 часа. Эмуляторы и установка приложения отключены настройками проекта.

## grid-hours-and-unified-time-inputs

Реализовано: 2026-10-16.

### Выполненные проверки

- Unit-тесты `:app:testDebugUnitTest` (`-Pksp.incremental=false`, из-за смешения путей WSL/Windows в кэше сборки) — 35/35 без сбоев, включая новые [WeekTimelineTest](../../src/slt-mobile/shared/src/commonTest/kotlin/com/logoped_plus/ui/screen/schedule/WeekTimelineTest.kt) (8): фильтр диапазона — span внутри сохраняется, пересекающий границу и полностью вне диапазона исключаются, прилегание к границам сохраняется.
- Сборка `:app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` + `:app:compileDebugKotlin` (`-Pksp.incremental=false`) — BUILD SUCCESSFUL.
- `openspec validate grid-hours-and-unified-time-inputs --strict` — успешно.
- `git diff --check` по затронутым файлам — успешно; относительные ссылки обновлённых карт проверены скриптом (существующие битые ссылки на отсутствующие `openspec/README.md` и `описание проекта.md` в `PACKAGES.md` не тронуты).
- Осмотр кода (сценарии 40–43 browse-schedule, 1–4 app-settings, 44 create-lesson, 45 child-recurring-schedule): диапазон 08:00–21:00 — 13 строк, последняя метка 21:00; занятие 07:30–08:30 скрыто, 08:00–09:00 видно; 23:30–01:00 при 20:00–24:00 видно в пятницу (23:30–24:00), скрыто в субботу; некорректная пара (20/10) — запасные 24 часа и ошибка в настройках; дефолты 0/24 — исходная раскладка (24 строки, метка 24:00, смещение 8); набор «0 8 3 0» в «08:00» — «08:30» (цифры → автоформат), «6 0» в «40» — «60» (выделение при фокусе).
- Доработка выделения (задача 6, 2026-10-16): полное выделение — только при входе фокуса из несфокусированного состояния, повторное нажатие на сфокусированный `TimeField`/`NumberField` переводит в точечное редактирование (осмотр кода, сценарий 13 time-input-controls; удалён неиспользуемый `FocusRequester`); `:app:compileDebugKotlin` и `:app:testDebugUnitTest` (`-Pksp.incremental=false`) — BUILD SUCCESSFUL, 35/35 без сбоев; `openspec validate grid-hours-and-unified-time-inputs --strict` — успешно.

### Ручная приёмка (незавершена)

- Настройка диапазона на устройстве: ввод «Начального/Конечного часа», ошибки и запасные 24 часа, сохранение после перезапуска.
- Наборы в полях времени и длительности (форма занятия, строки шаблона карточки ребёнка): замена значения набором, частичный ввод «9» и «08» некорректны и блокируют сохранение.
- Повторное нажатие на сфокусированное поле времени/числа: курсор в точку нажатия, набор вставляет в позицию курсора; выход из фокуса и повторный вход — выделение значения снова.
- Сетки недели/дня с нестандартным диапазоном: строки и метки, скрытые/видимые занятия, ночное занятие на границе, начальное смещение, двойное нажатие свободного часа диапазона.
- Эмуляторы и установка приложения отключены конфигурацией проекта; приёмка остаётся открытой до разрешения проверки на устройстве.
