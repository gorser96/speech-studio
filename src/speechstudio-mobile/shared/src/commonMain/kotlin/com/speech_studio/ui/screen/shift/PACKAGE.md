# com.logoped_plus.ui.screen.shift

Экран рабочей смены: дата, окно, слоты детей, создание занятий и разбор видео. [Общий указатель](../../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [ShiftModels.kt](ShiftModels.kt) | `ShiftSlotDraft` (ребёнок, длительность, вычисленное начало, связь `lessonId`), `ShiftLessonOption`, `ShiftVideoRow` (`effectiveLessonId`), `ShiftVideoReviewState`, `ShiftDialog` (RemoveSlot/ChangeChild/DeleteShift/DeleteLessons), `ShiftUiState` (черновик даты/окна/базы/перерыва, `canChangeDate`, `freeMinutes`/`overflows`/`geometryValid`, `dirty`, `saving`/`busy`/`working`, сообщения, `children`, `tags` для фильтра выбора, `videoReview`, `closeRequested`) |
| [ShiftViewModel.kt](ShiftViewModel.kt) | Владелец черновика смены: `open(shiftId)`/`close`, `changeDate`, окно/база/перерыв, `addChild`/`addChildren` (по слоту на каждого выбранного ребёнка), перестановка и длительность слотов, сохранение, создание и удаление занятий смены, поиск и подтверждение видео, диалоги; занятия смены берёт из `LessonRepository.state` по `lesson.shiftId`, тэги — из `TagRepository.state` |
| [ShiftViewModelFactory.kt](ShiftViewModelFactory.kt) | Фабрика с `ShiftRepository`, `ChildRepository`, `LessonRepository`, `TagRepository` и `FindShiftVideosUseCase` |
| [ShiftChildFilter.kt](ShiftChildFilter.kt) | `filterShiftChildren` — чистый фильтр списка детей: поиск по подстроке имени без учёта регистра, объединение выбранных тэгов (достаточно одного), исключение заданных `id` |
| [ShiftScreen.kt](ShiftScreen.kt) | Один экран смены: редактируемая дата смены, окно/база/перерыв, строки слотов с временем и кнопками вверх/вниз, свободное время, список созданных занятий («без слота» для потерявших слот), кнопки «Создать занятия», «Найти видео», «Сохранить смену» и удаление занятий смены; подтверждение опасных действий, запрос доступа к видео устройства. Шаблоны и генерация вынесены в [templates](templates/PACKAGE.md), удаление смены — в верхнюю панель `App.kt` |

Маршрут: [ScheduleScreen](../ScheduleScreen.kt) (`openShiftId`) → `ShiftScreen(shiftId)` → `ShiftViewModel` → [контракт](../../../domain/repository/PACKAGE.md) → [RoomShiftRepository](../../../data/repository/PACKAGE.md). Компоненты выбора детей и видео — [component](component/PACKAGE.md), шаблоны — [templates](templates/PACKAGE.md). Черновик не перезаписывается внешним снимком (`dirty`/`saving`); сохранение пересчитывает начала слотов через [ShiftTimeCalculator](../../../domain/model/PACKAGE.md) и переносит связанные занятия вместе с датой и слотами. Видео ищется через [FindShiftVideosUseCase](../../../domain/usecase/PACKAGE.md), применяется только после подтверждения (`attachVideos`).

## Проверки

- [ShiftViewModelTest.kt](../../../../../../../commonTest/kotlin/com/speech_studio/ui/screen/shift/ShiftViewModelTest.kt) — test: базовая длительность новых слотов, добавление нескольких детей за раз с сохранением порядка, пересчёт начал при перестановке, блокировка создания при выходе за окно, сохранение черновика перед созданием занятий, изменение даты до сохранения и перенос смены с занятиями, подтверждение удаления слота с занятием, применение видео только после подтверждения.
- [ShiftChildFilterTest.kt](../../../../../../../commonTest/kotlin/com/speech_studio/ui/screen/shift/ShiftChildFilterTest.kt) — test: поиск без учёта регистра и с обрезкой пробелов, объединение тэгов, совместное действие поиска и тэгов, исключение детей.
