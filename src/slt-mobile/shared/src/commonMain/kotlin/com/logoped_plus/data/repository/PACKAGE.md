# com.logoped_plus.data.repository

Постоянное локальное хранение детей, занятий и тэгов. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [RoomChildRepository.kt](RoomChildRepository.kt) | StateFlow Loading/Ready/Error со списком детей (имя + tagIds из child_tags), повтор чтения и suspend-запись детей; observeCard — Flow целой карточки (null при удалении); saveCard — валидация имени и [шаблона](../../domain/model/PACKAGE.md), NotReady до Ready, атомарная запись через DAO; deleteChild по шаблону write() (NotFound при отсутствии) |
| [RoomLessonRepository.kt](RoomLessonRepository.kt) | Одна заменяемая подписка DAO; готовность обоих справочников; типизированный результат после commit; deleteLesson по правилам write(); generateChildLessons — NotReady до готовности справочников, транзакция DAO, исключение → StorageUnavailable |
| [RoomTagRepository.kt](RoomTagRepository.kt) | StateFlow Loading/Ready/Error с повтором чтения; saveTag/deleteTag/reorderTags — NotReady до Ready, типизированный результат DAO, CancellationException пробрасывается, исключение → StorageUnavailable |

Реализует [контракты](../../domain/repository/PACKAGE.md), использует [Room](../local/PACKAGE.md). Создаётся в [AppContainer](../../../../../../../../app/src/main/java/com/logoped_plus/AppContainer.kt). InMemoryLessonRepository удалён. CancellationException пробрасывается; ошибка наблюдения после commit не отменяет успех записи. deleteLesson — NotReady до готовности справочников, CancellationException пробрасывается, исключение → StorageUnavailable. Нет seed, сети и fallback в память.

Debug-сервисы защищены DUMP permission, работают в отдельных процессах только с probe-БД. LessonTransactionProbeService проверяет полный агрегат и контрольные данные. В release сервисов нет.

## Проверки

- [RoomChildRepositoryTest.kt](../../../../../../../../app/src/androidTest/java/com/logoped_plus/data/repository/RoomChildRepositoryTest.kt) — androidTest, удаление ребёнка: занятия сохраняются со снимком имени, повтор — NotFound.
- [RoomLessonRepositoryTest.kt](../../../../../../../../app/src/androidTest/java/com/logoped_plus/data/repository/RoomLessonRepositoryTest.kt) — androidTest, сценарии транзакционного удаления.
- [TransactionProbeService.kt](../../../../../../../../app/src/debug/java/com/logoped_plus/data/repository/TransactionProbeService.kt) — debug.
- [LessonTransactionProbeService.kt](../../../../../../../../app/src/debug/java/com/logoped_plus/data/repository/LessonTransactionProbeService.kt) — debug.
- [LocalDatabaseIntegrationTest.kt](../../../../../../../../app/src/test/java/com/logoped_plus/data/local/LocalDatabaseIntegrationTest.kt) — test на Robolectric по DAO того же уровня (карточка, тэги, генерация).
