# com.logoped_plus.domain.model

Назначение: доменные структуры данных без зависимостей от Android UI. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл / символ | Данные |
|---|---|
| [Child.kt](Child.kt) — `Child` | Строковые `id`, `name`, назначения `tagIds` и `schedule: ChildSchedule` (по умолчанию пусты — совместимо со старыми потребителями имени/id) |
| [Lesson.kt](Lesson.kt) — `Lesson`, `LessonParticipant` | UUID по умолчанию, `participants: List<LessonParticipant>` (`childId` + `removedName` — имя удалённого ребёнка, NULL у актуальных), `scheduledAt: LocalDateTime`, длительность в минутах, комментарий, список видео, сохранённый `type: LessonType` (INDIVIDUAL/GROUP) и `shiftId: String?` (связь со сменой) |
| [LessonType.kt](LessonType.kt) — `LessonType` | Перечисление INDIVIDUAL / GROUP; тип сохраняется и не выводится из числа участников (группа с 1 участником остаётся GROUP); маппинг `fromStorage` |
| [WorkShift.kt](WorkShift.kt) — `WorkShift`, `ShiftSlot`, `ShiftTemplate` | Смена на дату со снимком окна (по умолчанию 08:00–12:00), базы (30 мин) и перерыва (5 мин), `templateId` и упорядоченными слотами (`ShiftSlot`: ребёнок, начало, длительность, связь с созданным занятием `lessonId`); `ShiftTemplate` — только временнáя сетка, дни недели ISO и дата окончания, без состава детей |
| [ShiftTimeCalculator.kt](ShiftTimeCalculator.kt) — `ShiftTimeCalculator`, `ShiftGeometry` | Чистый расчёт времени слотов смены по правилу `начало + длительность + перерыв`: начала слотов, свободное время окна и признак выхода слотов за окно |
| [ShiftValidation.kt](ShiftValidation.kt) — `ShiftValidation`, `ShiftIssue` | Чистая проверка шаблона и смены: положительная база/длительность, неотрицательный перерыв, порядок окна, дни недели и дата окончания для генерации |
| [VideoAttachment.kt](VideoAttachment.kt) — `VideoAttachment` | URI видео в виде строки; сам файл здесь не хранится |
| [DeviceVideo.kt](DeviceVideo.kt) — `DeviceVideo` | URI видео на устройстве, имя, время создания в эпохе (мс) |
| [Tag.kt](Tag.kt) — `Tag`, `TagColorKey` | Справочник тэгов: `id`, `name`, цвет — стабильный ключ палитры из 8 значений с читаемым названием |
| [ChildSchedule.kt](ChildSchedule.kt) — `ChildSchedule`, `WeeklyLessonSlot` | Недельный шаблон: ordered-список строк (стабильный `id`, день ISO 1–7, `startTime: LocalTime`, длительность в минутах) и общая `endDate` включительно; пустой шаблон допустим |
| [ChildScheduleValidation.kt](ChildScheduleValidation.kt) — `ChildScheduleValidation`, `ScheduleIssue` | Чистая проверка: день 1–7, положительная длительность, непредставимые длительности/даты, обязательная endDate при строках |
| [GenerationCounts.kt](GenerationCounts.kt) — `GenerationCounts` | Счётчики генерации: создано / уже существуют |
| [LessonSpan.kt](LessonSpan.kt) — `LessonSpan` | Интервал существующего занятия: epochDay, nanoOfDay, длительность |
| [RecurringMath.kt](RecurringMath.kt) — `RecurringMath` | Чистая арифметика абсолютных наносекунд (epochDay + nanoOfDay) с null при переполнении Long |

Используется [контрактами repository](../repository/PACKAGE.md), [хранением](../../data/repository/PACKAGE.md) и ViewModel. Связь занятия с детьми задаётся ID в порядке занятия; имя актуального участника вычисляется в `ScheduleViewModel.toUiModel` по данным детей, имя удалённого — снимок `removedName`, см. [расписание](../../ui/screen/schedule/PACKAGE.md) и [LessonUiModel](../../ui/screen/schedule/model/PACKAGE.md). Названия и цвета тэгов живут только в справочнике; карточка хранит ID.

Здесь нет аннотаций Room, сериализации и проверок корректности полей (чистые функции — исключение: `ChildScheduleValidation`, `RecurringMath`, `ShiftTimeCalculator`, `ShiftValidation`, они не зависят от Android). При изменении структуры проверь контракты, реализации, преобразование UI-модели и формы.

## Проверки

- [ShiftTimeCalculatorTest.kt](../../../../../../commonTest/kotlin/com/logoped_plus/domain/model/ShiftTimeCalculatorTest.kt) — test: расчёт сетки смен, порядок слотов, перерыв, базовое и индивидуальное время, свободные минуты и признак выхода за окно.
