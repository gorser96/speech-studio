# com.logoped_plus.data.local

Общая Room БД детей, занятий, тэгов, шаблонов расписания и смен. Room-типы остаются в data. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [ChildDao.kt](ChildDao.kt) | Наблюдение списка (observeChildren по position, observeChildrenWithTags с child_tags) и Flow целой карточки observeCard; вставка по UUID, переименование; saveCardOnce — атомарная замена имени, назначений тэгов (только актуальные ID справочника) и шаблона/строк; транзакционное удаление deleteOnce (снимок имени участника + удаление, нет имени → false); связи тэгов и шаблона удаляются по CASCADE |
| [ChildEntity.kt](ChildEntity.kt) | UUID, имя, независимый порядок добавления |
| [ChildCardWithRelations.kt](ChildCardWithRelations.kt) | Целая карточка: ребёнок + child_tags + child_schedule + child_schedule_slots, преобразование в Child (tagIds, ChildSchedule) |
| [ChildWithTags.kt](ChildWithTags.kt) | Ребёнок + child_tags для списка, преобразование в Child с tagIds |
| [ChildrenDatabase.kt](ChildrenDatabase.kt) | Room v5, `@ConstructedBy`/`RoomDatabaseConstructor`, exportSchema в `shared/schemas`; DAO детей/занятий/тэгов/смен; без seed/destructive fallback |
| [DatabaseMigrations.kt](DatabaseMigrations.kt) | Общий SQL миграций через `execSQL`: 1→2 (без пересоздания children), 2→3 (пересоздание lesson_participants: поле childName, без FK на children), 3→4 (новые таблицы tags, child_tags, child_schedule, child_schedule_slots) и 4→5 (таблицы shift_templates, work_shifts, shift_slots; пересоздание lessons с полями type/shiftId и FK на work_shifts `SET NULL`; тип заполняется по числу участников ≥ 2 → GROUP, иначе INDIVIDUAL; shiftId = NULL) |
| [LessonEntity.kt](LessonEntity.kt) | UUID, position, epochDay/nanoOfDay, длительность, комментарий, сохранённый `type` и необязательный `shiftId` (FK `work_shifts` `SET NULL` — удаление смены оставляет занятие) |
| [LessonParticipantEntity.kt](LessonParticipantEntity.kt) | Уникальные упорядоченные childId; childName — NULL у актуального участника, имя на момент удаления — у исторического; FK lessons CASCADE, на children нет |
| [LessonVideoEntity.kt](LessonVideoEntity.kt) | Уникальные URI с ordinal; FK lessons CASCADE |
| [LessonWithRelations.kt](LessonWithRelations.kt) | Целый агрегат, преобразования в Lesson (участники — LessonParticipant с childName) и сортировка связей по ordinal |
| [LessonDao.kt](LessonDao.kt) | Транзакционное наблюдение; addOnce по UUID+агрегату (пересечения разрешены), updateExisting без смены position, deleteExisting; generateForChild — транзакционное чтение актуального шаблона и занятий ребёнка, чистый [планировщик](../../domain/usecase/PACKAGE.md), пакетная вставка целых занятий (единый участник, пустые комментарий/видео); LessonSpanRow — проекция интервалов занятий |
| [TagEntity.kt](TagEntity.kt) | UUID, имя, nameKey (lowercase ROOT, уникальный), colorKey — стабильный ключ палитры |
| [TagDao.kt](TagDao.kt) | Наблюдение по position; saveOnce — trim, пустое/дублирующее название → типизированный отказ, идемпотентный повтор, уникальный ключ БД защищает от гонок; deleteOnce — удаление тэга и отвязка назначений одной транзакцией; reorderOnce — перестановка по списку id: сверка множества (иначе NotFound), два прохода position (сдвиг на n, затем 0..n-1) в одной транзакции — прямой swap невозможен, position — первичный ключ |
| [ChildTagEntity.kt](ChildTagEntity.kt) | Составной PK (childId, tagId); FK на children и tags — CASCADE |
| [ChildScheduleEntity.kt](ChildScheduleEntity.kt) | childId (PK/FK CASCADE на children) и endEpochDay — дата окончания включительно |
| [ChildScheduleSlotEntity.kt](ChildScheduleSlotEntity.kt) | Стабильный id строки, день ISO 1–7, nanoOfDay начала, длительность в минутах, ordinal; FK на children и child_schedule CASCADE, уникальный (childId, ordinal) |
| [TagMapping.kt](TagMapping.kt) | Преобразования Tag ↔ TagEntity (`nameKey` через общий `lowercase()`) |
| [ShiftTemplateEntity.kt](ShiftTemplateEntity.kt) | UUID, название, окно, база, перерыв, дни недели битовой маской ISO (бит 0 — понедельник) и необязательная `endEpochDay` — состав детей в шаблоне не хранится |
| [WorkShiftEntity.kt](WorkShiftEntity.kt) | UUID, дата epochDay, снимок окна/базы/перерыва и необязательный `templateId` (FK `shift_templates` `SET NULL`) |
| [ShiftSlotEntity.kt](ShiftSlotEntity.kt) | PK id, position (уникальна внутри смены), вычисленное начало, длительность, childId (FK CASCADE) и необязательный `lessonId` (уникальный, FK `lessons` `SET NULL`); FK shiftId `work_shifts` CASCADE |
| [ShiftWithSlots.kt](ShiftWithSlots.kt) | Целая смена со слотами и преобразования `ShiftWithSlots`/`ShiftTemplateEntity` ↔ доменные модели, маска дней недели |
| [ShiftDao.kt](ShiftDao.kt) | Наблюдение шаблонов и смен; транзакции saveTemplateOnce/deleteTemplateOnce (смены остаются без шаблона), createShiftOnce/saveShiftOnce (пересчёт даты и начал, следование занятий за слотами и датой смены, отвязка потерявших слот), deleteShiftOnce (слоты удаляются, занятия остаются без смены), createLessonsOnce (одно индивидуальное занятие на слот; существующее с тем же началом и длительностью связывается, групповое/другое/чужой смены — конфликт с откатом), deleteShiftLessonsOnce, attachVideosOnce (только новые привязки), generateShiftsOnce (пустые смены по дням недели, пропуск существующих) |

Платформенные адаптеры (package тот же, описание общее):

| Файл | Назначение |
|---|---|
| [ChildrenDatabase.android.kt](../../../../../../androidMain/kotlin/com/speech_studio/data/local/ChildrenDatabase.android.kt) | Builder на прежнем системном драйвере и файле `children.db`; миграции через `migrate(SupportSQLiteDatabase)`, включая 4→5 (`migrateToV5`) |
| [ChildrenDatabase.ios.kt](../../../../../../iosMain/kotlin/com/speech_studio/data/local/ChildrenDatabase.ios.kt) | `BundledSQLiteDriver`, путь Application Support; миграции через `migrate(SQLiteConnection)`, включая 4→5 (`migrateToV5`) |

Потребители — [репозитории](../repository/PACKAGE.md): `RoomShiftRepository` использует `ShiftDao`. Время не переводится в UTC. REPLACE родителя не используется.

## Проверки

- [LessonValidationTest.kt](../../../../../../commonTest/kotlin/com/speech_studio/data/local/LessonValidationTest.kt) — test, правила валидации участников (`invalidReason`): исторические участники валидны, пустой список и дубли ID — InvalidData.
- [IosDatabasePersistenceTest.kt](../../../../../../iosTest/kotlin/com/speech_studio/data/local/IosDatabasePersistenceTest.kt) — iOS bundled SQLite: переоткрытие файла сохраняет ребёнка, занятие, участника и URI видео; миграции 1→5 и 3→5 сохраняют данные и создают таблицы v5.
- [LocalDatabaseIntegrationTest.kt](../../../../../../../../app/src/test/java/com/speech_studio/data/local/LocalDatabaseIntegrationTest.kt) — Robolectric (sdk 34), 11 тестов: миграции 1→5, v3→v5 и v4→v5 непустой БД (дети, тэги, шаблон, занятия, участники, комментарии и видео сохраняются; тип заполняется: ≥ 2 участников → GROUP, иначе INDIVIDUAL; shiftId = NULL), сохранённый тип не выводится из участников после редактирования, повторное открытие непустой v5, отсутствие destructive fallback (данные не стираются), атомарная замена карточки и отвязка тэга, повторный эмит Flow карточки при смене назначений тэгов, child_tags в наблюдении списка, каскад удаления ребёнка, генерация: повтор без дублей, сохранность прежних занятий, rollback пакетной вставки (trigger RAISE(ABORT)); reorderOnce: перестановка трёх тэгов меняет порядок observeTags, чужое/суженное множество id — NotFound без изменения порядка.
- [LocalShiftDatabaseTest.kt](../../../../../../../../app/src/test/java/com/speech_studio/data/local/LocalShiftDatabaseTest.kt) — Robolectric (sdk 34), транзакции смен: занятие на каждый заполненный слот со связью, повтор без дублей, связывание существующего индивидуального занятия с тем же началом/длительностью, конфликты (групповое, занятие другой смены) с откатом, перестановка/изменение длительности двигают связанные занятия с сохранением комментария и видео, перенос даты смены двигает занятия и смену, смена ребёнка в слоте, удаление слота/смены оставляет занятие без смены, удаление занятий смены освобождает слоты, привязка только новых видео, генерация пустых смен с пропуском существующих, удаление шаблона не удаляет смены.
- Совпадение экспортированной схемы v5 с прежней Android-схемой (identity hash и DDL) проверено.
- iOS: bundled SQLite; переоткрытие непустой БД и миграции 1→5 и 3→5 покрыты `IosDatabasePersistenceTest`. Ручные UI- и видео-сценарии задачи 5.2 закрыты без фактической проверки — [статус проверок](../../../../../../../../../../openspec/verification/README.md).
