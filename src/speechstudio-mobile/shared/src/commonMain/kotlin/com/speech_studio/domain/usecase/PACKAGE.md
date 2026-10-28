# com.logoped_plus.domain.usecase

Бизнес-правила без Android. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [FindLessonVideosUseCase.kt](FindLessonVideosUseCase.kt) | Интервал занятия `[start, start+duration)` в эпохе через зону устройства; видео из [VideoLibrary](../repository/PACKAGE.md) без URI, уже находящихся в черновике |
| [FindShiftVideosUseCase.kt](FindShiftVideosUseCase.kt) | Пакетный поиск видео по занятиям смены: окно смены в эпохе, вызов `VideoLibrary.findVideos` и сопоставление через `ShiftVideoMatcher` |
| [ShiftVideoMatcher.kt](ShiftVideoMatcher.kt) — `ShiftVideoMatcher`, `ShiftLessonVideoTarget`, `ShiftVideoEntry`, `ShiftVideoReview` | Чистое сопоставление видео с интервалами занятий смены `[start, start+duration)`: неоднозначные (несколько занятий) не получают предложения, при равном числе видео и занятий — порядок времени, дубликаты из существующих привязок отмечаются, применение только после подтверждения |
| [RecurringLessonPlanner.kt](RecurringLessonPlanner.kt) — `RecurringLessonPlanner`, `PlannedLesson`, `RecurringPlanResult` | Чистый планировщик: даты от сегодня до endDate включительно, начало не раньше now; существующие — по паре «ребёнок/точное начало» (любая длительность, групповые занятия); пересечения со всем расписанием и кандидатами текущего запуска не проверяются — занятия создаются; порядок — дата, время, затем порядок строк шаблона; при одинаковом начале первая строка побеждает, следующая — «уже существует»; переполнение и невалидный шаблон → Invalid без записи |
| [GenerateChildLessonsUseCase.kt](GenerateChildLessonsUseCase.kt) | now один раз из внедрённого Clock в локальной зоне устройства; вызывает [generateChildLessons](../repository/PACKAGE.md) |
| [ShiftGenerationPlanner.kt](ShiftGenerationPlanner.kt) — `ShiftGenerationPlanner` | Чистый план генерации пустых смен: даты от `from` до `endDate` шаблона по дням недели ISO без существующих дат; занятия не создаются |
| [GenerateShiftShiftsUseCase.kt](GenerateShiftShiftsUseCase.kt) | today один раз из внедрённого Clock в зоне устройства; вызывает `generateShifts` репозитория |

Зависит только от [domain.model](../model/PACKAGE.md) и [domain.repository](../repository/PACKAGE.md). Создаётся в [AppContainer](../../../../../../../../app/src/main/java/com/speech_studio/AppContainer.kt) и общем [AppContainer](../../app/PACKAGE.md); планировщик потребляет [LessonDao](../../data/local/PACKAGE.md) в транзакции генерации, use case — [ChildrenViewModel](../../ui/screen/children/PACKAGE.md) и [ShiftViewModel](../../ui/screen/shift/PACKAGE.md).

## Проверки

- [FindLessonVideosUseCaseTest.kt](../../../../../../commonTest/kotlin/com/speech_studio/domain/usecase/FindLessonVideosUseCaseTest.kt) — test: границы интервала (старт включительно, конец не включительно), дубликаты из черновика, пустой результат, занятие через полночь.
- [RecurringLessonPlannerTest.kt](../../../../../../commonTest/kotlin/com/speech_studio/domain/usecase/RecurringLessonPlannerTest.kt) — test: пример 8 дат октября 2026, текущее время и последний день периода, повтор без дублей и существующее групповое занятие, создание пересечений со всем расписанием, соприкосновение границ, пересечение через полночь, пересекающиеся строки шаблона, одинаковое начало, перенесённое занятие, невалидные шаблоны.
- [ShiftGenerationPlannerTest.kt](../../../../../../commonTest/kotlin/com/speech_studio/domain/usecase/ShiftGenerationPlannerTest.kt) — test: генерация по дням недели, диапазон дат, пропуск существующих, пустой шаблон и отсутствие endDate.
- [ShiftVideoMatcherTest.kt](../../../../../../commonTest/kotlin/com/speech_studio/domain/usecase/ShiftVideoMatcherTest.kt) — test: попадание в один интервал, неоднозначность (пересечение нескольких интервалов), порядок по времени при равном количестве видео и занятий, исключение дубликатов, несопоставленные видео.
