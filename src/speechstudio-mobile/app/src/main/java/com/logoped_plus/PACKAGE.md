# com.logoped_plus

Запуск Android, зависимости и тонкая оболочка общего приложения. [Общий указатель](../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [MainActivity.kt](MainActivity.kt) | Edge-to-edge, общий `SpeechStudioTheme` и Android-оболочка `App` |
| [App.kt](App.kt) | Создаёт Activity-scoped ViewModel с фабриками и передаёт их в общий `com.logoped_plus.ui.App`; логотип берётся из общих ресурсов, выход — `activity.finish()`, экранное действие «Назад» выключено (его роль выполняет системная кнопка) |
| [AppContainer.kt](AppContainer.kt) | `createAppContainer(context)`: Room БД v4 через `createChildrenDatabase`, `SettingsRepository` через `createSettingsRepository`, `MediaStoreVideoScanner`, IO scope на процесс; собирает общий `com.logoped_plus.app.AppContainer` |
| [SpeechStudioApplication.kt](SpeechStudioApplication.kt) | Application-владелец общего контейнера; контейнер создаётся лениво при первом обращении и живёт до завершения процесса |

Связи: [общий контейнер](../../../../../../shared/src/commonMain/kotlin/com/logoped_plus/app/PACKAGE.md), [репозитории](../../../../../../shared/src/commonMain/kotlin/com/logoped_plus/data/repository/PACKAGE.md), [настройки](../../../../../../shared/src/commonMain/kotlin/com/logoped_plus/data/preferences/PACKAGE.md), [видео устройства](data/media/PACKAGE.md), [экраны](../../../../../../shared/src/commonMain/kotlin/com/logoped_plus/ui/screen/PACKAGE.md), [общая оболочка](../../../../../../shared/src/commonMain/kotlin/com/logoped_plus/ui/PACKAGE.md).

Навигация, разделы, подтверждение выхода и блокировки записи живут в общем `ui.App`; Android добавляет только системный `BackHandler` через `PlatformBackHandler`. По умолчанию расписание. Back из корневых разделов (Дети, Тэги, Настройки) возвращает в расписание; вложенные экраны (карточка/редактор, диалоги) перехватывают Back раньше. Раздел сохраняется при повороте через `rememberSaveable`, записи и черновики — через ViewModel. После commit ожидание снимка не блокирует навигацию.

## Проверки

На диске тестов для пакета нет; навигация и оболочка проверялись осмотром кода. Сборка `:app:assembleDebug` и `:app:testDebugUnitTest` выполнялась в изменении `cross-platform-kmp`; результаты — [статус проверок](../../../../../../../../openspec/verification/README.md).
