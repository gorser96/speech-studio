# com.logoped_plus.ios

iOS-оболочка общего приложения: точка входа для SwiftUI, контейнер зависимостей и ViewModels. [Общий указатель](../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [MainViewController.kt](MainViewController.kt) | `MainViewController()` — экспортируемая в Swift точка входа; создаёт ленивый контейнер на процесс и оборачивает общий `App` в `ComposeUIViewController` и `LogopedPlusTheme` |
| [IosApp.kt](IosApp.kt) | Создаёт `ScheduleViewModel`, `ChildrenViewModel`, `TagsViewModel`, `SettingsViewModel` общими фабриками и вызывает `com.logoped_plus.ui.App` с `onExit = null`, `showSectionBack = true` |
| [IosAppContainer.kt](IosAppContainer.kt) | `createIosAppContainer()`: `children.db` в Application Support через `createChildrenDatabase`, настройки `NSUserDefaults` через `createSettingsRepository`, пустой `VideoLibrary` (автопоиск не поддерживается) |

Связи: [общий контейнер](../../../../../commonMain/kotlin/com/logoped_plus/app/PACKAGE.md), [Room](../../../../../commonMain/kotlin/com/logoped_plus/data/local/PACKAGE.md), [настройки](../../../../../commonMain/kotlin/com/logoped_plus/data/preferences/PACKAGE.md), [видео](../../../../../commonMain/kotlin/com/logoped_plus/platform/video/PACKAGE.md), [общая оболочка](../../../../../commonMain/kotlin/com/logoped_plus/ui/PACKAGE.md). SwiftUI-обёртка лежит в [iosApp](../../../../../../../iosApp/README.md).

Ограничения: системный жест iOS не перехватывается (`PlatformBackHandler` — no-op), возврат выполняет экранная стрелка; выход из приложения — системный жест, подтверждение не показывается. Автопоиск видео скрыт (`deviceScan = false`). Контейнер не закрывается при уходе в фон и живёт до завершения процесса. Для `ComposeUIViewController` в `iosApp/iosApp/Info.plist` обязателен `CADisableMinimumFrameDurationOnPhone = true`: без него Compose завершает процесс при старте.

## Проверки

- `:shared:compileKotlinIosSimulatorArm64`, `:shared:compileKotlinIosArm64` и `:shared:compileTestKotlinIosSimulatorArm64` — BUILD SUCCESSFUL на Windows и повторно на macOS arm64 без флага кросс-компиляции.
- На macOS с Xcode 27.1: `:shared:linkDebugFrameworkIosSimulatorArm64` и `xcodebuild`-сборка `iosApp` для Apple Silicon-симулятора (`ARCHS=arm64`) — BUILD SUCCESSFUL, Compose-ресурсы попадают в `iosApp.app`.
- `:shared:iosSimulatorArm64Test` — 41 тест без падений и пропусков, включая `IosDatabasePersistenceTest` (переоткрытие БД, миграции 1→4 и 3→4) и `IosSettingsRepositoryTest` (`NSUserDefaults` после пересоздания репозитория).
- Первый запуск на iPhone 18 Pro (iOS 27.0): пустое «Расписание» без демо-данных, `children.db` версии 4 в Application Support, падений нет. Ручные UI-сценарии видео и возвратов остаются открытыми в [статусе проверок](../../../../../../../../../openspec/verification/README.md).
