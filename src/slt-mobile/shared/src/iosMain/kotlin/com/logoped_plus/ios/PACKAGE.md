# com.logoped_plus.ios

iOS-оболочка общего приложения: точка входа для SwiftUI, контейнер зависимостей и ViewModels. [Общий указатель](../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [MainViewController.kt](MainViewController.kt) | `MainViewController()` — экспортируемая в Swift точка входа; создаёт ленивый контейнер на процесс и оборачивает общий `App` в `ComposeUIViewController` и `LogopedPlusTheme` |
| [IosApp.kt](IosApp.kt) | Создаёт `ScheduleViewModel`, `ChildrenViewModel`, `TagsViewModel`, `SettingsViewModel` общими фабриками и вызывает `com.logoped_plus.ui.App` с `onExit = null`, `showSectionBack = true` |
| [IosAppContainer.kt](IosAppContainer.kt) | `createIosAppContainer()`: `children.db` в Application Support через `createChildrenDatabase`, настройки `NSUserDefaults` через `createSettingsRepository`, пустой `VideoLibrary` (автопоиск не поддерживается) |

Связи: [общий контейнер](../../../../../commonMain/kotlin/com/logoped_plus/app/PACKAGE.md), [Room](../../../../../commonMain/kotlin/com/logoped_plus/data/local/PACKAGE.md), [настройки](../../../../../commonMain/kotlin/com/logoped_plus/data/preferences/PACKAGE.md), [видео](../../../../../commonMain/kotlin/com/logoped_plus/platform/video/PACKAGE.md), [общая оболочка](../../../../../commonMain/kotlin/com/logoped_plus/ui/PACKAGE.md). SwiftUI-обёртка лежит в [iosApp](../../../../../../../iosApp/README.md).

Ограничения: системный жест iOS не перехватывается (`PlatformBackHandler` — no-op), возврат выполняет экранная стрелка; выход из приложения — системный жест, подтверждение не показывается. Автопоиск видео скрыт (`deviceScan = false`). Контейнер не закрывается при уходе в фон и живёт до завершения процесса.

## Проверки

- `:shared:compileKotlinIosSimulatorArm64`, `:shared:compileKotlinIosArm64` и `:shared:compileTestKotlinIosSimulatorArm64` с `-Pkotlin.native.enableKlibsCrossCompilation=true` — BUILD SUCCESSFUL.
- Сборка Xcode-проекта, запуск в симуляторе и ручные сценарии на macOS не выполнялись: среда Windows. Остаются открытыми в [статусе проверок](../../../../../../../../../openspec/verification/README.md).
