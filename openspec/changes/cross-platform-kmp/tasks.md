# Tasks

## 1. Базовая линия и каркас сборки

- [x] 1.1 Зафиксировать исходное состояние Android: выполнить `:app:assembleDebug` и `:app:testDebugUnitTest`, сохранить число тестов и результат; отделить посторонние изменения рабочего дерева, чтобы этапы перехода были отдельными коммитами.
- [x] 1.2 Добавить `:shared` с таргетами Android и iOS, версиями из design.md и зависимостью `:app` от `:shared`; проверить `:shared:compileKotlinMetadata`, `:app:assembleDebug` и `:app:testDebugUnitTest` без изменения поведения Android.

## 2. Domain и общие правила

- [x] 2.1 Перенести domain-модели, контракты репозиториев, use case и чистые функции в `commonMain`; заменить `java.time`, `java.util.UUID`, `java.util.Locale` и `java.text.Collator` по design.md. Проверить компиляцию `:shared` и существующие unit-тесты domain.
- [x] 2.2 Добавить детерминированные русские форматирование дат/месяцев и сортировку детей/тэгов; проверить unit-тестами русский алфавит, `ё`, регистр, тёзок и независимость от системной локали.

## 3. Room KMP, настройки и сохранность

- [x] 3.1 Перенести сущности, DAO и `ChildrenDatabase` в `commonMain`, добавить `@ConstructedBy`/`RoomDatabaseConstructor`, вынести SQL миграций в общие функции и платформенные адаптеры (`SupportSQLiteDatabase` на Android, `SQLiteConnection` на iOS), настроить KSP-схемы в `shared/schemas`; проверить экспорт схемы v4, совпадение DDL с прежней схемой и сборку `:shared`. `:app` проверяется после 4.2.
- [x] 3.2 Добавить платформенные builder БД (Android — прежний системный драйвер, iOS — bundled) и `SettingsRepository` для Android/iOS; проверить сохранение настроек, открытие непустой Android-БД v4, миграции 1→4 и отсутствие destructive fallback через Robolectric; iOS-миграции проверить на macOS.

## 4. Общий Compose UI

- [x] 4.1 Перенести тему, компоненты, ресурсы логотипа и общие экраны в `commonMain`; заменить dynamic color и `R.drawable` платформенными адаптерами. Проверить осмотром кода сохранение светлой/тёмной темы и логотипа drawer; сборка `:app` — после 4.2.
- [x] 4.2 Перенести ViewModels и оставшиеся экраны, заменить `LocalContext`, `Activity` и `BackHandler` на общие контракты и `PlatformBackHandler`; на iOS добавить экранное действие «Назад» по app-navigation. Проверить сборку, адресные unit-тесты ViewModel и осмотром кода возвраты, блокировки записи, черновики и отсутствие подтверждения выхода на iOS.
- [x] 4.3 Реализовать платформенные адаптеры видео: Android SAF/MediaStore/ACTION_VIEW без регрессии, iOS document picker/bookmark/системный просмотр, скрытие автопоиска при `deviceScan = false`. Проверить существующие тесты видео и осмотром кода частичный отказ, недоступный bookmark и открепление без удаления файла.

## 5. iOS-оболочка

- [ ] 5.1 Добавить `iosApp` с SwiftUI-оболочкой, `ComposeUIViewController` и статическим framework; на macOS собрать Xcode-проект и запустить приложение в симуляторе с пустым `children.db`. Код оболочки и проект добавлены; сборка Xcode и запуск симулятора не выполнялись (нет macOS).
- [ ] 5.2 Подключить iOS-адаптеры БД, настроек, видео и возврата; на macOS проверить сохранение ребёнка/занятия после перезапуска, сохранение настроек, выбор и открытие видео после перезапуска, открепление и поведение при недоступном файле. Адаптеры подключены и iOS klib компилируется; сценарии на macOS не проверены.

## 6. Проверки и документация

- [x] 6.1 Выполнить доступные на Windows проверки из design.md (`:shared:compileKotlinMetadata`, `:app:assembleDebug`, `:app:testDebugUnitTest`), зафиксировать результаты; эмулятор, установку APK и `connectedDebugAndroidTest` не запускать.
- [ ] 6.2 На macOS выполнить iOS-компиляцию, симуляторные тесты и ручную приёмку на устройстве для системного выбора файлов и просмотрщика; незавершённые сценарии явно оставить открытыми в `openspec/verification/README.md`. Не выполнено: доступна только Windows-сборка iOS klib.
- [x] 6.3 Обновить `PACKAGES.md` и затронутые `PACKAGE.md`, проверить отсутствие `android.*`/`androidx.*` в `commonMain` и `java.time`/`Collator` в domain, выполнить `openspec validate cross-platform-kmp --strict` и `git diff --check`.
