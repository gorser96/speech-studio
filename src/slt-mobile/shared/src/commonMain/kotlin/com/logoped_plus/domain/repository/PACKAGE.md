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
| [VideoLibrary.kt](VideoLibrary.kt) | suspend findVideos(from, to) — видео устройства в интервале эпохи |

Модели — [domain.model](../model/PACKAGE.md), реализации — [data.repository](../../data/repository/PACKAGE.md) и [data.media](../../../../../../../../app/src/main/java/com/logoped_plus/data/media/PACKAGE.md) (`VideoLibrary`). Клиенты — [дети](../../ui/screen/children/PACKAGE.md), [тэги](../../ui/screen/tags/PACKAGE.md), [расписание](../../ui/screen/schedule/PACKAGE.md) и [use cases](../usecase/PACKAGE.md). retryLoading занятий заменяет подписку из Ready/Error; в Loading игнорируется. Success подтверждает commit, а не последующую эмиссию. `generateChildLessons` читает и пишет в одной Room-транзакции; ручные пересечения `addLesson` по-прежнему разрешены.

## Проверки

На диске тестов для пакета нет.
