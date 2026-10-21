# com.logoped_plus.domain.usecase

Бизнес-правила без Android. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [FindLessonVideosUseCase.kt](FindLessonVideosUseCase.kt) | Интервал занятия `[start, start+duration)` в эпохе через зону устройства; видео из [VideoLibrary](../repository/PACKAGE.md) без URI, уже находящихся в черновике |
| [RecurringLessonPlanner.kt](RecurringLessonPlanner.kt) — `RecurringLessonPlanner`, `PlannedLesson`, `RecurringPlanResult` | Чистый планировщик: даты от сегодня до endDate включительно, начало не раньше now; существующие — по паре «ребёнок/точное начало» (любая длительность, групповые занятия); пересечения со всем расписанием и кандидатами текущего запуска не проверяются — занятия создаются; порядок — дата, время, затем порядок строк шаблона; при одинаковом начале первая строка побеждает, следующая — «уже существует»; переполнение и невалидный шаблон → Invalid без записи |
| [GenerateChildLessonsUseCase.kt](GenerateChildLessonsUseCase.kt) | now один раз из внедрённого Clock в локальной зоне устройства; вызывает [generateChildLessons](../repository/PACKAGE.md) |

Зависит только от [domain.model](../model/PACKAGE.md) и [domain.repository](../repository/PACKAGE.md). Создаётся в [AppContainer](../../../../../../../../app/src/main/java/com/logoped_plus/AppContainer.kt); планировщик потребляет [LessonDao](../../data/local/PACKAGE.md) в транзакции генерации, use case — [ChildrenViewModel](../../ui/screen/children/PACKAGE.md).

## Проверки

- [FindLessonVideosUseCaseTest.kt](../../../../../../commonTest/kotlin/com/logoped_plus/domain/usecase/FindLessonVideosUseCaseTest.kt) — test: границы интервала (старт включительно, конец не включительно), дубликаты из черновика, пустой результат, занятие через полночь.
- [RecurringLessonPlannerTest.kt](../../../../../../commonTest/kotlin/com/logoped_plus/domain/usecase/RecurringLessonPlannerTest.kt) — test: пример 8 дат октября 2026, текущее время и последний день периода, повтор без дублей и существующее групповое занятие, создание пересечений со всем расписанием, соприкосновение границ, пересечение через полночь, пересекающиеся строки шаблона, одинаковое начало, перенесённое занятие, невалидные шаблоны.
