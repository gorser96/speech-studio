# Design

## Context

Цель и границы — в [proposal.md](proposal.md), наблюдаемое поведение — в дельтах [cross-platform](specs/cross-platform/spec.md), [lesson-videos](specs/lesson-videos/spec.md) и [app-navigation](specs/app-navigation/spec.md).

Текущее состояние проверено по коду: `:app` — единственный модуль, Kotlin 2.2.10, AGP 9.4.0 со встроенным Kotlin, Compose BOM 2026.02.01, Room 2.8.4 + KSP, minSdk 34. DI и навигация ручные (`AppContainer`, `AppScreen` + `when`); Hilt и Navigation Compose не подключены. Domain использует `java.time`, `java.util.UUID`, `java.util.Locale`, `java.text.Collator`. Data использует `Context`, `SharedPreferences`, `SupportSQLiteDatabase`, `MediaStore`. UI использует `LocalContext`, `Activity`, `BackHandler`, `R.drawable.icon1024`, dynamic color и Android-ресурсы. DAO уже асинхронны: записи и `@Transaction`-методы — `suspend`, чтения — `Flow`; `@RawQuery`, `withTransaction` и Room-callback не используются.

Применяются ARCHITECTURE.md: Presentation → Domain ← Data, Room как source of truth, Flow, offline-first, бизнес-правила вне ViewModel, минимальные абстракции. Отклонение по DI и навигации сохраняется: Hilt и Navigation Compose не внедряются, переход на них не требуется для кроссплатформенности и увеличил бы риск. Платформенные API выносятся в адаптеры, а не протекают в domain.

Ограничение среды: проект разрабатывается на Windows. Android-сборка и common metadata доступны локально; iOS-таргеты компилируются и проверяются только на macOS с Xcode. Эмулятор Android и `connectedDebugAndroidTest` отключены конфигурацией проекта.

## Goals / Non-Goals

**Goals:** общий `commonMain` для domain, data, ViewModel и Compose UI; идентичное поведение и сохранность данных Android; рабочий iOS-паритет основных сценариев; схема БД v4 без изменений; Android собирается и проходит тесты после каждого этапа.

**Non-Goals:** автопоиск видео на iOS и `PHPhotoLibrary`; синхронизация и перенос данных между платформами; iPad-раскладка; Compose Desktop/Web; Hilt, Navigation Compose, use-case-слой ради шаблона; изменение бизнес-правил, схемы v4 или формата существующих записей; release-подпись и публикация в магазинах.

## Decisions

### 1. Модули и плагины

```text
src/speechstudio-mobile/
├── shared/   Kotlin Multiplatform: commonMain / androidMain / iosMain
├── app/      com.android.application, тонкая оболочка
└── iosApp/   Xcode-проект, SwiftUI + ComposeUIViewController
```

`:shared` применяет `org.jetbrains.kotlin.multiplatform`, `com.android.kotlin.multiplatform.library` (официальный Android-таргет KMP в AGP 9), compose-compiler и KSP. Таргеты: `android`, `iosArm64`, `iosSimulatorArm64`; framework `Shared` статический. `iosX64` не добавляется: сборка и симулятор предполагают Apple Silicon, а лишний таргет удваивает время KSP.

`:app` остаётся `com.android.application` и зависит от `:shared`; в нём остаются `MainActivity`, manifest, Android-иконки, debug-probe-сервисы и Android-адаптеры, которым нужен `Context`. `com.android.application` нельзя применять к KMP-модулю, поэтому выделение `:shared` — не стилистический выбор, а условие iOS-сборки. Альтернатива «один модуль с source sets» не даёт iOS-приложения и повышает риск сломать Android.

Пакеты и имена ключевых классов сохраняются, в частности `com.logoped_plus.data.local.ChildrenDatabase`. Это сохраняет имена Room-схем, identity hash и относительные пути миграционных проверок.

KSP Room-компилятор подключается для Android и обоих iOS-таргетов; схемы экспортируются в `shared/schemas`, а Android test assets ссылаются на этот каталог. Debug-probe-сервисы остаются в `:app/src/debug`, так как зависят от Android `Service` и контейнера.

### 2. Версии и совместимость

Фиксируются: Kotlin 2.2.10 (совпадает со встроенным Kotlin AGP 9.4.0), AGP 9.4.0, Gradle 9.6.0, Compose Multiplatform 1.10.3, Room 2.8.5, `androidx.sqlite:sqlite-bundled` 2.6.2 (версия, согласованная с Room 2.8.5), KSP 2.3.10, coroutines 1.9.0, `kotlinx-datetime` 0.7.1, lifecycle KMP 2.9.6, `ui-backhandler` 1.10.3. Lifecycle зафиксирован на 2.9.6: артефакт `androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0` не содержит iOS-вариантов, а `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.6` резолвится для всех таргетов.

CMP 1.12.2 отложен: для iOS он требует Kotlin 2.3, а AGP 9.4/9.5-alpha10 используют встроенный Kotlin 2.2.10; смешивать версии KGP в `:app` и `:shared` нельзя. Room 2.8.x уже поддерживает KMP, поэтому переход на room3 alpha не нужен. На iOS используется `BundledSQLiteDriver`, чтобы не зависеть от системной версии SQLite; Android остаётся на системном драйвере ради сохранности существующего файла. Если CMP 1.10.3 не содержит используемого API Material3, приложение остаётся на версиях Compose, поставляемых CMP, вместо BOM 2026.02.01; бизнес-правила это не затрагивает.

### 3. Room KMP и сохранность Android

Сущности, DAO, `ChildrenDatabase` и миграции переносятся в `commonMain`. Для KMP добавляются `@ConstructedBy(ChildrenDatabaseConstructor::class)` и `expect object ChildrenDatabaseConstructor : RoomDatabaseConstructor<ChildrenDatabase>`; Room генерирует `actual`. Схема остаётся v4, `exportSchema = true`, набор таблиц и SQL не меняются.

Builder создаётся на платформе, потому что файловые API и драйверы разные:

- Android: `Room.databaseBuilder<ChildrenDatabase>(context, "children.db")` в прежнем режиме совместимости с системным SQLite — тот же файл и тот же драйвер, что сейчас. Это главная гарантия сохранности существующей установки; Android не переводится на bundled-драйвер.
- iOS: каталог Application Support приложения (с созданием при необходимости) + `/children.db`, `setDriver(BundledSQLiteDriver())`.

Общая часть добавляет `setQueryCoroutineContext(Dispatchers.IO)`. `BundledSQLiteDriver` выбран для iOS, чтобы не зависеть от версии системного SQLite; Android сохраняет системный драйвер, поэтому `room_master_table` и identity hash остаются прежними. SQL миграций общий, различия движков ограничены стандартным SQL.

SQL миграций и порядок 1→2→3→4 сохраняются. Так как Android остаётся без драйвера, Room вызывает `migrate(SupportSQLiteDatabase)`, а на iOS с драйвером — `migrate(SQLiteConnection)`. Общие функции `migrateToV2/V3/V4(execSQL: (String) -> Unit)` содержат один и тот же SQL, а платформенный `migrations()` возвращает объекты `Migration`, переопределяющие доступную на платформе сигнатуру. Это дублирует только адаптер вызова, не SQL. `fallbackToDestructiveMigration` и seed не добавляются. Все DAO-функции, компилируемые для iOS, должны быть `suspend` либо возвращать `Flow`; текущие DAO уже соответствуют, но при переносе это проверяется компиляцией. `withTransaction` не используется; при необходимости транзакций в общем коде применяется `useWriterConnection { immediateTransaction { … } }`.

Android-тест миграции продолжает открывать непустую БД v4 через прежний Android-builder и системный драйвер, поэтому проверяет тот же путь, что у пользователей. Миграции и сборка запросов на iOS проверяются на macOS; проверка на Android-устройстве невозможна в текущем режиме и фиксируется как незавершённая.

### 4. Domain: даты, локаль, UUID

`java.time` заменяется на `kotlinx-datetime`: `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`, `TimeZone`, `Clock.System`. `java.time.YearMonth` заменяется общим представлением года и месяца (например, первый день месяца + операции над `LocalDate`), `DayOfWeek` — ISO-номером 1–7, как уже хранится в БД. Перевод в epoch day/nano of day остаётся на границе хранения, поэтому формат БД не меняется.

Русские названия месяцев и дней недели формируются детерминированным общим форматтером, а не `DateTimeFormatter`/`TextStyle`: `kotlinx-datetime` не локализует, а платформенные локали дали бы разный результат. Форматы `dd.MM.yyyy` и `d MMMM yyyy` воспроизводятся явно.

`Collator` заменяется общей функцией ключа сортировки: приведение к нижнему регистру без локали, нормализация `ё` к `е` (соответствует PRIMARY-силе прежнего `Collator` для русской локали), сравнение по кодовым точкам и стабильный fallback по технической позиции/ID. `Locale.ROOT` в `lowercase()` заменяется locale-invariant `lowercase()`; существующие `nameKey` в БД не пересчитываются, так как для кириллицы результат совпадает.

`java.util.UUID` заменяется `expect fun randomUuidString(): String`: Android — `UUID.randomUUID()`, iOS — `NSUUID().UUIDString.lowercase()`. Это сохраняет строковый формат ID и не требует opt-in экспериментального API. Альтернатива `kotlin.uuid.Uuid` допустима, но потребовала бы opt-in во всех модулях.

### 5. Настройки

Общий контракт `SettingsRepository` хранит те же четыре `StateFlow` и сеттеры, что текущий `SettingsStore`. Android-реализация использует прежний файл `settings` и прежние ключи через `SharedPreferences`, поэтому значения существующих установок читаются без миграции. iOS-реализация использует `NSUserDefaults.standardUserDefaults` с теми же ключами и значениями по умолчанию. Хранилище передаётся в контейнер, ViewModels остаются общими.

### 6. Видео и платформенные возможности

`lesson_videos.uri` остаётся строкой без изменения схемы. Android хранит `content://`-URI, iOS — Base64 security-scoped bookmark. Ссылки не переносятся между платформами; это явно указано в спецификации.

Общие контракты:

- `VideoLibrary` (`findVideos`) — существующий контракт автопоиска; Android-реализация остаётся на MediaStore, iOS-реализация возвращает пустой результат.
- `VideoPicker` — выбор нескольких видео и возврат выбранных ссылок с числом неудачных прикреплений.
- `VideoAccess` — имя для отображения и открытие ссылки с типизированным результатом ошибки.
- `VideoCapabilities(deviceScan = true/false)` — управляет видимостью кнопки автопоиска; на iOS кнопка отсутствует, а не показывается нерабочей.

Выбор запускается из общего Compose UI через `expect @Composable fun rememberVideoEnvironment()`: Android actual использует `ActivityResultContracts.OpenMultipleDocuments` и `takePersistableUriPermission`; iOS actual показывает `UIDocumentPickerViewController` для `UTType.movie` с `allowsMultipleSelection = true`, `asCopy = false` и сохраняет bookmark. Открытие: Android — прежний `ACTION_VIEW` с `FLAG_GRANT_READ_URI_PERMISSION`; iOS — разрешение bookmark, `startAccessingSecurityScopedResource`, системный `UIDocumentInteractionController`/`QLPreviewController`; доступ освобождается после закрытия. Имя на iOS берётся из имени файла после разрешения bookmark, при ошибке — «Видео».

`PHPhotoLibrary` не подключается: автопоиск по времени занятия — отдельное изменение, а ручной выбор файлов покрывает базовый паритет. Ошибки bookmark (файл удалён, доступ отозван) показываются по общим правилам, ссылка сохраняется и доступна для открепления.

### 7. UI, тема, ресурсы, возврат

Все экраны, компоненты и темы переносятся в `commonMain` на Compose Multiplatform. `LocalContext` удаляется из общего кода: выбор видео, открытие файла, настройки и завершение приложения приходят через контейнер и `expect`-composable. `AppScreen` и ручная навигация сохраняются.

Dynamic color остаётся функцией Android: общий `SpeechStudioTheme` получает `ColorScheme?` от `expect`-функции, Android возвращает dynamic-схему на API 31+, iOS — `null`, затем используются общие светлая и тёмная схемы. Логотип из `R.drawable.icon1024` переносится в Compose Resources (`shared/src/commonMain/composeResources`), чтобы drawer не зависел от Android-ресурсов; иконки приложения остаются платформенными.

`BackHandler` заменяется общим `PlatformBackHandler`: Android actual использует `androidx.compose.ui.backhandler.BackHandler` из `ui-backhandler` 1.10.3, iOS actual не перехватывает системный жест. На iOS в заголовке показывается экранное действие «Назад» для вложенных экранов и корневых разделов, возвращающее тем же обработчикам, что Android-back; доступ к остальным разделам сохраняется через меню (отдельное действие в заголовке или прежний drawer). На корневом расписании iOS действие выхода отсутствует; приложение уходит в фон системным жестом. Android сохраняет диалог подтверждения и `finish()`.

### 8. Контейнер, ViewModel и оболочки

`AppContainer` становится общим и принимает платформенные зависимости: готовую `ChildrenDatabase`, `SettingsRepository`, `VideoLibrary`, `CoroutineScope` и `Clock`. `VideoPicker`/`VideoAccess` остаются Compose-адаптерами (`rememberVideoEnvironment`), а выход из приложения — функцией оболочки. Android-фабрика создаёт контейнер из `Application` и сохраняет текущие имена репозиториев/use case; iOS-фабрика — из `NSUserDefaults` и каталога Application Support. Ручная DI сохраняется; Hilt не добавляется.

`MainActivity` остаётся тонкой: edge-to-edge и `setContent { SpeechStudioTheme { App(container) } }`. `iosApp` содержит минимальное SwiftUI-приложение и `UIViewControllerRepresentable`, возвращающий `ComposeUIViewController { App(container) }`; контейнер живёт столько же, сколько приложение. ViewModel создаются существующими фабриками, но на общем `lifecycle-viewmodel-compose` KMP.

### 9. Этапы и обратимость

1. **Базовая линия.** Зафиксировать текущие `:app:assembleDebug` и `:app:testDebugUnitTest`; рабочее дерево с посторонними файлами не смешивать с изменением.
2. **Каркас сборки.** Добавить `:shared`, Android- и iOS-таргеты, зависимости и пустой общий модуль; `:app` начинает зависеть от `:shared`. Android-поведение не меняется.
3. **Domain и утилиты.** Перенести модели, контракты репозиториев, use case и чистые функции; заменить `java.time`, `Collator`, `Locale`, `UUID`; перенести соответствующие unit-тесты в `commonTest`.
4. **Data и Room.** Перенести сущности, DAO, БД, миграции и реализации репозиториев; добавить платформенные builder и настройки; проверить миграцию непустой БД и открытие прежнего файла.
5. **UI.** Перенести тему, компоненты, экраны и ViewModels; заменить `LocalContext`, ресурсы, dynamic color и `BackHandler`; подключить платформенные адаптеры видео.
6. **iOS-оболочка.** Добавить `iosApp`, iOS-адаптеры БД, настроек, видео и возврата; собрать и проверить на macOS.
7. **Приёмка.** Прогнать доступные проверки, обновить карты пакетов и verification, зафиксировать недоступные сценарии.

Этапы 1 и 4 завершаются отдельным коммитом с зелёными `:app:assembleDebug` и `:app:testDebugUnitTest`. На этапах 2–3 промежуточная Android-сборка не является воротами: domain и data уже используют `kotlinx-datetime`, а UI ещё `java.time`, поэтому `:app` временно не компилируется. Интеграционная проверка Android выполняется после задачи 4.2, когда все потребители типов перенесены; до этого проверяются `:shared` и адресные общие тесты. Схема БД не меняется, поэтому откат Android-приложения на предыдущую версию не требует очистки данных; при появлении схемы v5 понадобится явная обратная совместимость, destructive downgrade запрещён. Перед этапом 4 сохраняется копия тестовой БД v4.

### 10. Проверки

- Локально (Windows): `:shared:compileKotlinMetadata`, `:app:assembleDebug`, `:app:testDebugUnitTest`. Существующие тесты сохраняются; новые добавляются только на риск: детерминированная русская сортировка/форматирование, миграции и открытие прежней Android-БД v4, разбор платформенных ссылок там, где это тестируемо без iOS.
- `LocalDatabaseIntegrationTest` остаётся на Robolectric для Android-пути с прежним builder и системным драйвером; при переносе в `:shared` включается `androidHostTest`, иначе тест остаётся в `:app`.
- На macOS: `:shared:compileKotlinIosSimulatorArm64`, `:shared:iosSimulatorArm64Test`, сборка Xcode-проекта и проверка iOS-миграций на bundled-драйвере. Ручная проверка на устройстве: выбор файла, bookmark после перезапуска, открытие, открепление, возврат.
- Эмулятор Android, установка APK и `connectedDebugAndroidTest` не запускаются по конфигурации проекта; это фиксируется в verification, а не выдаётся за успешную проверку.
- `openspec validate cross-platform-kmp --strict` и `git diff --check`; карты `PACKAGE.md`/`PACKAGES.md` обновляются при переносе пакетов.

## Risks / Trade-offs

- [Версии AGP/Kotlin/CMP/Room несовместимы] → версии зафиксированы и проверяются резолвом на этапе 2; CMP 1.10.3 и Room 2.8.5 выбраны под Kotlin 2.2.10; если native-klib CMP потребует более новый патч Kotlin, берётся совместимая версия CMP (не выше 1.10.x) или KGP 2.2.20 при поддержке AGP; при конфликте API Material3 понижается BOM, а не бизнес-код.
- [Смена драйвера повреждает прежний Android-файл] → Android остаётся на прежнем builder и системном SQLite; bundled-драйвер применяется только к новому iOS-хранилищу; Android-проверка миграции идёт по тому же пути, что у пользователя.
- [Ошибка миграции теряет данные] → SQL миграций переносятся без изменений, destructive fallback запрещён, проверка непустой БД и отката; перед этапом 4 — резервная копия.
- [iOS нельзя проверить на Windows] → этап 6 требует macOS; до этого iOS-код не объявляется проверенным, а приёмка остаётся открытой.
- [Security-scoped bookmark устаревает] → ошибка без потери ссылки, открепление доступно; файл не копируется и не удаляется.
- [Разные системные API выбора/просмотра дают расхождение UX] → общие тексты ошибок и результата, платформенные адаптеры скрыты за контрактами; на iOS кнопка автопоиска отсутствует вместо нерабочей.
- [Перенос UI ломает Android-поведение] → этапы с зелёной Android-сборкой и тестами, сохранение ручной навигации и ViewModel; ручная приёмка на устройстве откладывается из-за запрета эмулятора.
- [KMP-компиляция требует suspend-DAO] → текущие DAO уже соответствуют; компиляция iOS выявит нарушения, исправление не меняет контракты.
- [Compose Resources и Android-иконки расходятся] → общий логотип в Compose Resources, платформенные иконки приложения отдельно; визуальная проверка drawer на Android и iOS.

## Migration Plan

Порядок этапов и ворота описаны в решении 9. Перед переносом БД сохраняется копия непустой v4; после переноса проверяются миграции 1→4, открытие Android-файла, сохранность ID/порядка/связей и отказ от destructive fallback. Откат выполнения — возврат коммита этапа; откат установленного Android-приложения безопасен, пока версия схемы остаётся 4. iOS начинает с нового пустого `children.db`; перенос данных с Android не выполняется.

## Open Questions

- Bundle identifier, команда разработчика и подпись iOS определяются при настройке `iosApp`; на архитектуру и задачи не влияют.
- Конкретный системный просмотрщик iOS (`UIDocumentInteractionController` или `QLPreviewController`) выбирается при реализации; оба удовлетворяют требованию открытия во внешнем/системном средстве.
