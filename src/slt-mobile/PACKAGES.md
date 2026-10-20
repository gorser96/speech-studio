# Карта пакетов «Кабинет речи»

Kotlin Multiplatform приложение: общий модуль `shared` (Android и iOS) и Android-модуль `app`. Ниже перечислены все пакеты, объявленные в исходниках. Промежуточные каталоги без собственных классов не являются отдельными пунктами. Пакеты `commonMain`, `androidMain` и `iosMain` с одинаковым именем описаны одной картой в `commonMain`; тестовые пакеты описаны там же.

## Выбор пакета по задаче

| Package / описание | Что искать |
|---|---|
| [com.logoped_plus](app/src/main/java/com/logoped_plus/PACKAGE.md) | Android-запуск, `Application`, фабрика общего `AppContainer`, тонкая оболочка общего `App` |
| [com.logoped_plus.app](shared/src/commonMain/kotlin/com/logoped_plus/app/PACKAGE.md) | Общий контейнер зависимостей: репозитории, use case, настройки, scope, закрытие БД |
| [com.logoped_plus.data.local](shared/src/commonMain/kotlin/com/logoped_plus/data/local/PACKAGE.md) | Room-схема v4, миграции 1→2→3→4, DAO детей/занятий/тэгов/шаблонов, платформенные builder БД |
| [com.logoped_plus.data.media](app/src/main/java/com/logoped_plus/data/media/PACKAGE.md) | Видео устройства через MediaStore (Android-реализация `VideoLibrary`) |
| [com.logoped_plus.data.preferences](shared/src/commonMain/kotlin/com/logoped_plus/data/preferences/PACKAGE.md) | Общий контракт настроек; SharedPreferences на Android и `NSUserDefaults` на iOS |
| [com.logoped_plus.data.repository](shared/src/commonMain/kotlin/com/logoped_plus/data/repository/PACKAGE.md) | Room-хранение детей, занятий, тэгов и шаблонов, атомарная карточка, пакетная генерация |
| [com.logoped_plus.domain.model](shared/src/commonMain/kotlin/com/logoped_plus/domain/model/PACKAGE.md) | Структуры ребёнка, занятия, тэгов, видеовложений; чистая проверка шаблона |
| [com.logoped_plus.domain.repository](shared/src/commonMain/kotlin/com/logoped_plus/domain/repository/PACKAGE.md) | Контракты доступа к детям, занятиям, тэгам и видео устройства; результат генерации |
| [com.logoped_plus.domain.text](shared/src/commonMain/kotlin/com/logoped_plus/domain/text/PACKAGE.md) | Детерминированные русские даты/время и сортировка имён без системной локали |
| [com.logoped_plus.domain.usecase](shared/src/commonMain/kotlin/com/logoped_plus/domain/usecase/PACKAGE.md) | Use case выбора видео по интервалу занятия; чистый планировщик периодических занятий |
| [com.logoped_plus.ios](shared/src/iosMain/kotlin/com/logoped_plus/ios/PACKAGE.md) | iOS-точка входа `ComposeUIViewController`, контейнер на Application Support/`NSUserDefaults`, ViewModels |
| [com.logoped_plus.platform](shared/src/commonMain/kotlin/com/logoped_plus/platform/PACKAGE.md) | `expect`-адаптеры UUID и системного «Назад» |
| [com.logoped_plus.platform.video](shared/src/commonMain/kotlin/com/logoped_plus/platform/video/PACKAGE.md) | Общий контракт выбора/просмотра видео; Android SAF/ACTION_VIEW и iOS document picker/bookmark/системный просмотр |
| [com.logoped_plus.ui](shared/src/commonMain/kotlin/com/logoped_plus/ui/PACKAGE.md) | Общая оболочка `App`, боковое меню и список разделов |
| [com.logoped_plus.ui.components](shared/src/commonMain/kotlin/com/logoped_plus/ui/components/PACKAGE.md) | Единые контролы ввода времени «ЧЧ:ММ» и чисел |
| [com.logoped_plus.ui.screen](shared/src/commonMain/kotlin/com/logoped_plus/ui/screen/PACKAGE.md) | Экраны разделов: дети, тэги, расписание, настройки |
| [com.logoped_plus.ui.screen.children](shared/src/commonMain/kotlin/com/logoped_plus/ui/screen/children/PACKAGE.md) | Состояние списка и карточки ребёнка, добавление, тэги, шаблон, генерация, удаление |
| [com.logoped_plus.ui.screen.schedule](shared/src/commonMain/kotlin/com/logoped_plus/ui/screen/schedule/PACKAGE.md) | Календарь, неделя, день, действия, состояние, создание/просмотр/редактирование/удаление занятия |
| [com.logoped_plus.ui.screen.schedule.component](shared/src/commonMain/kotlin/com/logoped_plus/ui/screen/schedule/component/PACKAGE.md) | Выбор нескольких детей, поиск по имени, выбор/открытие/открепление видео |
| [com.logoped_plus.ui.screen.schedule.model](shared/src/commonMain/kotlin/com/logoped_plus/ui/screen/schedule/model/PACKAGE.md) | Данные занятия для отображения, имена детей и URI |
| [com.logoped_plus.ui.screen.tags](shared/src/commonMain/kotlin/com/logoped_plus/ui/screen/tags/PACKAGE.md) | Справочник «Тэги»: состояние, палитра цветов, редактор, удаление |
| [com.logoped_plus.ui.theme](shared/src/commonMain/kotlin/com/logoped_plus/ui/theme/PACKAGE.md) | Compose-тема; dynamic color — Android actual, iOS — общие схемы |

## Основные маршруты

- Запуск Android: `AndroidManifest.xml` → `MainActivity` → `LogopedPlusApplication`/`createAppContainer` → Android-оболочка `com.logoped_plus.App` → общий `com.logoped_plus.ui.App` → выбранный экран.
- Запуск iOS: `iosApp` SwiftUI → `MainViewControllerKt.MainViewController()` → `createIosAppContainer` → `IosApp` → общий `com.logoped_plus.ui.App` → выбранный экран.
- Изменение занятия: `ScheduleScreen` → форма из `ui.screen.schedule` → `ScheduleAction` → `ScheduleViewModel` → `LessonRepository` → `RoomLessonRepository` → обновлённый `ScheduleUiState`.
- Изменение имени/тэгов/шаблона ребёнка: `ChildrenScreen` → карточка (`ChildCardView`) → `ChildrenViewModel` → `ChildRepository.saveCard`/`TagRepository` → `RoomChildRepository` → `ChildDao.saveCardOnce` (одна транзакция: имя + child_tags + child_schedule(_slots)) → observeCard обновляет карточку, Flow обновляет список и расписание.
- Периодические занятия: карточка «Создать занятия» → `ChildrenViewModel.generateLessons` → `GenerateChildLessonsUseCase` (Clock) → `LessonRepository.generateChildLessons` → `RoomLessonRepository` → `LessonDao.generateForChild` (одна Room-транзакция: чтение актуального шаблона и занятий ребёнка, чистый `RecurringLessonPlanner`, пакетная вставка) → счётчики в карточке, расписание обновляется через Flow.
- Справочник тэгов: `TagsScreen` → `TagsViewModel` → `TagRepository` → `RoomTagRepository` → `TagDao` (saveOnce/deleteOnce); назначения в карточке читают тот же справочник, удалённый тэг отвязывается по CASCADE.
- Удаление ребёнка: карточка → `DeleteChildDialog` → `ChildrenViewModel.delete` → `ChildRepository.deleteChild` → `RoomChildRepository` → `ChildDao.deleteOnce` (снимок имени участников + удаление; тэги и шаблон — по CASCADE) → Flow обновляет список детей и расписание (занятия остаются, имена — снимки в `LessonUiModel`).
- Видеовложение: `VideoAttachmentsEditor` → черновик формы/экрана → `UpdateLesson` → `VideoAttachment`. Выбор выполняет платформенный `rememberVideoEnvironment` (`platform.video`), открытие URI — `VideoAttachmentLink`.
- Автоподкрепление видео: `VideoAttachmentsEditor` (кнопка, разрешение `READ_MEDIA_VIDEO` на Android) → `AutoAttachLessonVideos` → `ScheduleViewModel` → `FindLessonVideosUseCase` → `MediaStoreVideoScanner` → черновик через `ChangeVideos`. Автопоиск скрыт, если `VideoCapabilities.deviceScan = false`.
- Настройка «Скрывать выходные»: `SettingsScreen` → `SettingsViewModel` → `SettingsRepository` → SharedPreferences/`NSUserDefaults`; `ScheduleViewModel` подписывается на repository и передаёт флаг в `WeekScheduleView` (пять/семь столбцов).

## Вне пакетов

| Путь | Назначение |
|---|---|
| [OpenSpec](../../openspec/config.yaml), [изменения](../../openspec/changes), [спецификации](../../openspec/specs) | Разработка по спецификациям и общие правила проекта |
| [Статус проверок](../../openspec/verification/README.md) | Требования функций и незавершённая приёмка |
| [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) | Activity запуска, тема Android, backup, параметры приложения |
| [app/src/main/res](app/src/main/res) | Ресурсы Android: имя приложения, XML-тема, иконки, backup-правила |
| [shared/src/commonMain/composeResources](shared/src/commonMain/composeResources) | Общие ресурсы Compose, включая логотип drawer |
| [iosApp](iosApp/README.md) | Xcode-проект и SwiftUI-оболочка, статический framework `Shared` |
| [app/build.gradle.kts](app/build.gradle.kts), [shared/build.gradle.kts](shared/build.gradle.kts) | Android-приложение и KMP-модуль: таргеты, KSP, зависимости, статический framework |
| [settings.gradle.kts](settings.gradle.kts), [build.gradle.kts](build.gradle.kts), [gradle/libs.versions.toml](gradle/libs.versions.toml) | Модули, плагины, версии зависимостей |
| [ARCHITECTURE.md](ARCHITECTURE.md), [описание проекта.md](<../../описание проекта.md>) | Архитектурный план и продуктовый контекст |

Дети, занятия, тэги и шаблоны сохраняются в Room БД между запусками. Начальные списки пусты. Android использует прежний файл `children.db` и системный драйвер; iOS — bundled SQLite и путь в Application Support. Зависимости собираются общим `com.logoped_plus.app.AppContainer` из платформенных БД, настроек и источника видео. Hilt и Navigation Compose не подключены. Robolectric подключён только для локальных проверок Room на Android. iOS-код компилируется в klib на Windows, но Xcode-сборка и запуск выполняются только на macOS.
