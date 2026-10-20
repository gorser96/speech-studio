# Статус проверок

По одному разделу на изменение: выполненные проверки и незавершённая ручная приёмка.

## rename-app-to-kabinet-rechi

Реализовано: 2026-10-21. Название приложения — «Кабинет речи»; GitHub-репозиторий — `gorser96/speech-studio`.

### Выполненные проверки

- `android:label` приложения и activity по-прежнему ссылается на `@string/app_name`; в `packageDebugResources/values/values.xml` и собранном APK значение `app_name` — «Кабинет речи» (12 кодовых точек, совпадение с ожидаемым).
- Обе конфигурации iOS (Debug, Release): `INFOPLIST_KEY_CFBundleDisplayName = "Кабинет речи"`; bundle id и `PRODUCT_NAME` не менялись.
- Осмотр `AppDrawer.kt`: заголовок — «Кабинет речи», `contentDescription` — «Кабинет речи», fallback — «КР»; удалён неиспользуемый импорт `Color`. Перенос длинного названия обеспечивается штатным многострочным `Text` в `Column`.
- Графика: обе копии `icon1024.png` и launcher-ресурсы всех плотностей пересобраны из прежнего изобразительного мотива без старой надписи; просмотрены `icon1024.png`, foreground, обычная и круглая иконки; адаптивные XML ссылаются на существующие foreground/background, monochrome использует foreground. Все 15 WebP в APK побайтно совпадают с ресурсами проекта.
- Идентификаторы сохранены: `applicationId`/`namespace` — `com.logoped_plus`, iOS bundle id — `com.logopedplus.iosApp`, package, БД `children.db`, ключи настроек, rootProject и подпись не менялись.
- Документация: название заменено в `ARCHITECTURE.md`, `PACKAGES.md`, `iosApp/README.md`, `описание проекта.md`, `README.md`, `WSL_ANDROID_DEBUGGING_AGENT.md`, `openspec/config.yaml`, `package-lock.json`. Исторические изменения и отчёты не переписаны. Поиск `Логопед` по `src/` не находит пользовательских упоминаний; в `openspec/specs/` остаётся только слово «Логопед» как название профессии.
- `:app:assembleDebug` — BUILD SUCCESSFUL (Gradle 1m48s). `openspec validate rename-app-to-kabinet-rechi --strict` — успешно. `git diff --check` — без ошибок (только предупреждения о CRLF).
- GitHub: имя `gorser96/speech-studio` было свободно, у токена есть admin; репозиторий переименован через API (`200`, `full_name` = `gorser96/speech-studio`). `git remote -v` указывает на новый адрес, `git ls-remote origin HEAD` возвращает `f164067`.

### Незавершённые проверки и приёмка

- Обновление существующей установки Android с данными: установить сборку с новым названием поверх прежней с совместимой подписью и убедиться, что приложение обновилось, а дети, занятия, тэги, шаблоны, видео и настройки доступны. Проверяется неизменностью идентификаторов; эмулятор и установка отключены конфигурацией проекта.
- Визуальная проверка меню и launcher-иконок на устройствах: полное название при увеличенном системном тексте, отсутствие наложения, обычная/круглая/adaptive/monochrome иконки на светлой и тёмной теме.
- Доступность: TalkBack на Android и VoiceOver на iOS читают «Кабинет речи» для логотипа.
- iOS: Xcode-сборка и отображение имени на симуляторе/устройстве — только на macOS; на Windows проверены обе конфигурации статически.
- Название карточки Google Play и доступность имени в магазине — вне репозитория, меняются при публикации в Play Console.
- Локальная папка остаётся `D:\projects\slt-plus`; это не влияет на имя репозитория. Инструкции WSL используют путь `/mnt/d/projects/slt-plus` и сохранены.

## cross-platform-kmp

Изменение закрыто и заархивировано 2026-10-21. Задачи 1.1–4.3, 6.1 и 6.3 выполнены с проверками. Задачи 5.1, 5.2 и 6.2 закрыты **явным решением пользователя 2026-10-21 без проверки на macOS**: iOS-сборка Xcode, запуск в симуляторе, iOS-тесты и приёмка на устройстве фактически не выполнялись. Ниже это отмечено отдельно; закрытие задач не подтверждает эти сценарии.

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
- После выделения общего `AppContainer` и iOS-оболочки: `:app:assembleDebug`, `:app:testDebugUnitTest` (13 тестов) и `:shared:testAndroidHostTest` (37 тестов) — BUILD SUCCESSFUL, регрессий Android не выявлено.
- Общая зависимость ViewModel — `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.6`: артефакт `androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0` не содержит iOS-вариантов и ломал резолв iOS-таргетов.
- Осмотр кода iOS-видео: `UIDocumentPickerViewController` для `UTType.movie` с множественным выбором и `asCopy = false`; выбранные файлы сохраняются как Base64 security-scoped bookmark, имя берётся из последнего компонента пути; просмотр — `UIDocumentInteractionController` (preview, затем open-in) с освобождением security-scoped доступа после закрытия; ошибки `Missing`/`Permission`/`InvalidLink`/`NoViewer` возвращаются в общий UI; `deviceScan = false`, файл не копируется и не удаляется.
- Осмотр кода iOS-оболочки: общий `AppContainer` принимает БД, настройки и `VideoLibrary`; iOS-фабрика использует Application Support/`children.db`, `BundledSQLiteDriver`, `NSUserDefaults` и пустой источник автопоиска; `MainViewController` оборачивает общий `App` в `ComposeUIViewController`; `iosApp` — SwiftUI + `UIViewControllerRepresentable`, framework `Shared` статический; выход `onExit = null`, экранное «Назад» включено. Xcode-проект написан вручную и ещё не открывался в Xcode.
- `openspec validate --all --strict` — 19 спецификаций, 0 ошибок; `git diff --check` — без ошибок (только предупреждения о CRLF). Ссылки в обновлённых `PACKAGE.md` и `iosApp/README.md` проверены скриптом.
- Запуск на эмуляторе Pixel_7a (Android 14, API 34): APK собирался, но падал при старте с `MissingResourceException` для `drawable/icon1024.png` — AGP-KMP не передавал сгенерированные Compose-ресурсы в assets приложения. В `shared/build.gradle.kts` включено `androidResources.enable = true`; после `:app:installDebug` приложение открывает «Расписание», в logcat нет падений. `:app:connectedDebugAndroidTest` по-прежнему не запускался.

### Незавершённые проверки и приёмка

- **Не подтверждено, задачи закрыты решением пользователя 2026-10-21.** Инструкция продолжения на Mac mini: [подготовка среды, сборка, симулятор, отладка и iPhone](../../src/slt-mobile/iosApp/README.md). Подготовлена 2026-10-20; команды macOS не выполнялись. Отдельного `iosTest` миграций пока нет, общий `iosSimulatorArm64Test` их не подтверждает. Задачи 5.1, 5.2 и 6.2 отмечены выполненными, хотя перечисленные ниже проверки не выполнены.
- iOS на macOS: собрать `iosApp` в Xcode (статический `Shared`, фаза `embedAndSignAppleFrameworkForXcode`), запустить в симуляторе с пустым `children.db`, проверить миграции 1→4 и сохранение ребёнка/занятия/настроек после перезапуска. Вручную написанный `project.pbxproj` не проверен Xcode.
- iOS-видео на macOS/устройстве: системный выбор нескольких видео, сохранение bookmark после перезапуска, открытие в системном просмотрщике, открепление без удаления файла, недоступный/отозванный файл, частичный отказ при выборе. На Windows проверена только компиляция и осмотр кода.
- iOS-возврат: экранное «Назад» в заголовке для вложенных экранов и корневых разделов, отсутствие подтверждения выхода; системный жест уводит приложение в фон.
- `:app:connectedDebugAndroidTest` не запускался; установка debug APK и запуск на эмуляторе Pixel_7a (Android 14, API 34) выполнены вручную 2026-10-20, приложение открывается без падений.
- Ручная приёмка Android после перехода: открытие существующей `children.db` v4, сохранение настроек и логотип drawer, навигация/Back, черновики и блокировки записи, Android-видео (SAF, сохранение grant после перезапуска, `ACTION_VIEW`).


## auto-attach-lesson-videos

Реализовано: 2026-09-30.

### Выполненные проверки

- Unit-тесты `:app:testDebugUnitTest` — 13/13 без сбоев, включая [FindLessonVideosUseCaseTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/domain/usecase/FindLessonVideosUseCaseTest.kt) (границы интервала, дубликаты, пустой результат, занятие через полночь) и [MediaStoreTimeToMillisTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/data/media/MediaStoreTimeToMillisTest.kt) (нормализация секунд/миллисекунд).
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
- Unit-тесты `:app:testDebugUnitTest` — 16/16 без сбоев, включая [LessonValidationTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/data/local/LessonValidationTest.kt) (занятие только с историческими участниками валидно; пустой список и дубли ID — InvalidData).
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
  - новые [RecurringLessonPlannerTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/domain/usecase/RecurringLessonPlannerTest.kt) (11): пример 8 дат октября 2026 (8, 9, 15, 16, 22, 23, 29, 30), текущее время на последнем дне (15:00:30 → только 16:00; ровно 15:00:00 → обе строки), повтор без дублей и существующее групповое занятие с другой длительностью, конфликты со всем расписанием (16:20–16:50 против 16:00–16:40 пропущен, 16:40–17:10 создан), соприкосновение границ, переход через полночь (23:50+40 против 00:10), пересекающиеся строки шаблона (первая побеждает), одинаковое начало (вторая — «уже существует»), перенесённое занятие освобождает старое время, невалидные шаблоны (нет даты, день 8, длительность ≤ 0, непредставимая).
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
  - обновлённые [RecurringLessonPlannerTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/domain/usecase/RecurringLessonPlannerTest.kt) (11): пример 8 дат октября 2026 (8, 9, 15, 16, 22, 23, 29, 30), текущее время на последнем дне, повтор без дублей и существующее групповое занятие с другой длительностью, пересечения со всем расписанием создаются (16:20–16:50 и 16:40–17:10 против чужого 16:00–16:40), соприкосновение границ, переход через полночь (23:50+40 против 00:10 — создано), пересекающиеся строки шаблона (обе созданы), одинаковое начало (вторая — «уже существует»), перенесённое занятие, невалидные шаблоны (нет даты, день 8, длительность ≤ 0, непредставимая).
  - обновлённые [LocalDatabaseIntegrationTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/data/local/LocalDatabaseIntegrationTest.kt) (5): генерация (создано 2, уже существуют 0) с пересечением занятия другого ребёнка, повтор (0, 2) без дублей, сохранение прежних занятий, rollback пакетной вставки через trigger `RAISE(ABORT)` — частичного набора нет, повтор после снятия причины (4, 0).
  - [ChildrenViewModelCardTagsTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/ui/screen/children/ChildrenViewModelCardTagsTest.kt) (1) — мок генерации с двухсчётчиком `GenerationCounts()`.
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

- Unit-тесты `:app:testDebugUnitTest` (`-Pksp.incremental=false`, из-за смешения путей WSL/Windows в кэше сборки) — 35/35 без сбоев, включая новые [WeekTimelineTest](../../src/slt-mobile/app/src/test/java/com/logoped_plus/ui/screen/schedule/WeekTimelineTest.kt) (8): фильтр диапазона — span внутри сохраняется, пересекающий границу и полностью вне диапазона исключаются, прилегание к границам сохраняется.
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
