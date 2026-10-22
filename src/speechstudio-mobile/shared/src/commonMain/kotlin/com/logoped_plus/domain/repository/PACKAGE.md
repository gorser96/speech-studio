# com.logoped_plus.domain.repository

Контракты данных без Android и Room. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [ChildRepository.kt](ChildRepository.kt) | StateFlow ChildLoadState, retryLoading, observeCard (Flow целой карточки, null при удалении), suspend addChild/updateChild/saveCard (атомарная карточка: имя + тэги + шаблон)/deleteChild |
| [ChildLoadState.kt](ChildLoadState.kt) | Loading/Ready/Error с последним списком детей |
| [ChildWriteResult.kt](ChildWriteResult.kt) | Success после commit либо типизированный отказ; причины включают InvalidSchedule |
| [LessonRepository.kt](LessonRepository.kt) | StateFlow LessonLoadState, retryLoading, suspend addLesson/updateLesson/deleteLesson, generateChildLessons (childId + now → типизированный результат); синхронных чтений нет |
| [LessonLoadState.kt](LessonLoadState.kt) | Loading/Ready/Error с целым предыдущим снимком |
| [LessonWriteResult.kt](LessonWriteResult.kt) | Success(lesson); Failure: NotReady, InvalidData, UnknownChild, NotFound, Conflict, StorageUnavailable |
| [GenerateChildLessonsResult.kt](GenerateChildLessonsResult.kt) | Success(GenerationCounts) либо Failure: NotReady, NotFound, InvalidSchedule, StorageUnavailable |
| [TagRepository.kt](TagRepository.kt) | StateFlow TagLoadState, retryLoading, suspend saveTag (стабильный ID черновика, идемпотентный повтор)/deleteTag (отвязка от детей)/reorderTags (новый порядок по списку id, NotFound при расхождении состава) |
| [TagLoadState.kt](TagLoadState.kt) | Loading/Ready/Error с последним списком тэгов |
| [TagWriteResult.kt](TagWriteResult.kt) | Success после commit либо типизированный отказ: InvalidName, DuplicateName, NotFound, StorageUnavailable, NotReady |
| [ShiftRepository.kt](ShiftRepository.kt) | StateFlow ShiftLoadState, retryLoading, observeShifts, observeShift, observeTemplates, findTemplate, suspend createShift/saveShift/deleteShift, createLessons, deleteShiftLessons, attachVideos, saveTemplate/deleteTemplate, generateShifts; `ShiftVideoAssignment` (uri + lessonId) |
| [ShiftLoadState.kt](ShiftLoadState.kt) | Loading/Ready/Error со списком смен |
| [ShiftWriteResult.kt](ShiftWriteResult.kt) | Success либо Failure: NotReady, InvalidData, NotFound, UnknownChild, StorageUnavailable |
| [CreateShiftLessonsResult.kt](CreateShiftLessonsResult.kt) | Success(created, linked) либо Failure: NotReady, NotFound, Conflict, StorageUnavailable |
| [ShiftGenerationResult.kt](ShiftGenerationResult.kt) | Success(created, existing) либо Failure: NotReady, InvalidTemplate, TemplateNotFound, StorageUnavailable |
| [VideoLibrary.kt](VideoLibrary.kt) | suspend findVideos(from, to) — видео устройства в интервале эпохи |

Модели — [domain.model](../model/PACKAGE.md), реализации — [data.repository](../../data/repository/PACKAGE.md) и [data.media](../../../../../../../../app/src/main/java/com/logoped_plus/data/media/PACKAGE.md) (`VideoLibrary`). Клиенты — [дети](../../ui/screen/children/PACKAGE.md), [тэги](../../ui/screen/tags/PACKAGE.md), [расписание](../../ui/screen/schedule/PACKAGE.md), [смена](../../ui/screen/shift/PACKAGE.md) и [use cases](../usecase/PACKAGE.md). retryLoading занятий и смен заменяет подписку из Ready/Error; в Loading игнорируется. Success подтверждает commit, а не последующую эмиссию. `generateChildLessons` и `createLessons` читают и пишут в одной Room-транзакции; ручные пересечения `addLesson` по-прежнему разрешены.

## Проверки

На диске тестов для пакета нет.
