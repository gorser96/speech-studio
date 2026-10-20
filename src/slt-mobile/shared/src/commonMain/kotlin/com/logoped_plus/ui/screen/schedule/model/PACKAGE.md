# com.logoped_plus.ui.screen.schedule.model

Назначение: модель занятия для Compose UI. [Общий указатель](../../../../../../../../../../PACKAGES.md).

| Файл / символ | Данные |
|---|---|
| [LessonUiModel.kt](LessonUiModel.kt) — `LessonUiModel`, `LessonParticipantUi` | `id`, дата/время, длительность, готовая строка `childNames` (сетки), комментарий, `participantCount`, `videoUris` и `participants: List<LessonParticipantUi>` (имя + тэги для деталей) |

Создание UI-модели выполняет приватное расширение `Lesson.toUiModel` в [ScheduleViewModel.kt](../ScheduleViewModel.kt), см. [schedule](../PACKAGE.md). Оно берёт имя участника из снимка `removedName` либо по ID через данные детей, пропускает ненайденных при построении строки, считает тип занятия по числу всех участников (включая удалённых) и преобразует вложения в URI. Для участников детали: имя (снимок либо актуальное) и тэги ребёнка в порядке справочника; у удалённого участника тэгов нет. Исходная сущность описана в [domain.model](../../../../domain/model/PACKAGE.md).

Потребители: `ScheduleUiState`, календарные карточки, детали и редактирование занятия, а также [ScheduleScreen](../../PACKAGE.md). Для изменения отображаемых полей проверь и модель, и mapper, и эти места использования. Сам класс не содержит бизнес-логики или Android API. Прямых тестов UI-модели и mapper пока нет.
