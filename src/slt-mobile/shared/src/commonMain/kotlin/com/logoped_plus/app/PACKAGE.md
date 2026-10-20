# com.logoped_plus.app

Общий контейнер зависимостей приложения, не зависящий от платформы. [Общий указатель](../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [AppContainer.kt](AppContainer.kt) | Принимает готовую `ChildrenDatabase`, `SettingsRepository`, `VideoLibrary`, `CoroutineScope` и `Clock`; создаёт `RoomChildRepository`, `RoomLessonRepository`, `RoomTagRepository`, `GenerateChildLessonsUseCase` и `FindLessonVideosUseCase`; `close()` отменяет scope и закрывает БД |

Платформенные фабрики передают свои зависимости: Android — [createAppContainer](../../../../../../../app/src/main/java/com/logoped_plus/AppContainer.kt), iOS — `createIosAppContainer` в [com.logoped_plus.ios](../../../../../iosMain/kotlin/com/logoped_plus/ios/PACKAGE.md). Потребители — Android- и iOS-оболочки, которые создают ViewModels существующими фабриками. Ручная DI сохраняется, Hilt не добавлен.

## Проверки

- Android: `:app:assembleDebug`, `:app:testDebugUnitTest` и `:shared:testAndroidHostTest` — без регрессий после выделения общего контейнера.
- iOS: `:shared:compileKotlinIosSimulatorArm64` и `:shared:compileKotlinIosArm64` (кросс-компиляция klib на Windows) — успешно; запуск на macOS остаётся в задачах 5.x/6.2 изменения `cross-platform-kmp`.
