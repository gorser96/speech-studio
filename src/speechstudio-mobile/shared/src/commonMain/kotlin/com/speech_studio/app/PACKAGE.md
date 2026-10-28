# com.logoped_plus.app

Общий контейнер зависимостей приложения, не зависящий от платформы. [Общий указатель](../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [AppContainer.kt](AppContainer.kt) | Принимает готовую `ChildrenDatabase`, `SettingsRepository`, `VideoLibrary`, `CoroutineScope` и `Clock`; создаёт `RoomChildRepository`, `RoomLessonRepository`, `RoomTagRepository`, `RoomShiftRepository`, `GenerateChildLessonsUseCase`, `FindLessonVideosUseCase`, `FindShiftVideosUseCase` и `GenerateShiftShiftsUseCase`; `close()` отменяет scope и закрывает БД |

Платформенные фабрики передают свои зависимости: Android — [createAppContainer](../../../../../../../app/src/main/java/com/speech_studio/AppContainer.kt), iOS — `createIosAppContainer` в [com.logoped_plus.ios](../../../../../iosMain/kotlin/com/speech_studio/ios/PACKAGE.md). Потребители — Android- и iOS-оболочки, которые создают ViewModels существующими фабриками. Ручная DI сохраняется, Hilt не добавлен.

## Проверки

- Android: `:app:assembleDebug`, `:app:testDebugUnitTest` и `:shared:testAndroidHostTest` — без регрессий после выделения общего контейнера.
- iOS: klib собраны на Windows и macOS, framework и приложение для Apple Silicon-симулятора собираются, первый запуск выполнен; ручные UI-сценарии 5.2 и приёмка на устройстве 6.2 закрыты без фактической проверки — [статус проверок](../../../../../../../../../openspec/verification/README.md).
