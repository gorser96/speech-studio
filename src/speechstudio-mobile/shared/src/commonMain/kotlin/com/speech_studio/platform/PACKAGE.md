# com.logoped_plus.platform

Платформенные адаптеры, не зависящие от UI. [Общий указатель](../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [PlatformUuid.kt](PlatformUuid.kt) | `expect fun randomUuidString(): String`; Android — `UUID.randomUUID()`, iOS — `NSUUID` |
| [PlatformBackHandler.kt](PlatformBackHandler.kt) | `expect @Composable PlatformBackHandler(enabled, onBack)`; Android — системный `BackHandler`, iOS — no-op (жест уводит приложение в фон) |

Потребители — ViewModel/экраны для генерации ID и общая оболочка [ui.App](../ui/App.kt) для системного «Назад».

## Проверки

Отдельных тестов нет: Android-ветка проверяется сборкой и ручным сценарием, iOS-ветка — на macOS.
