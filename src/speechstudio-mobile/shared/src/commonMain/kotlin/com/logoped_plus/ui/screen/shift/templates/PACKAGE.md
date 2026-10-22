# com.logoped_plus.ui.screen.shift.templates

Раздел «Шаблоны смен»: список шаблонов, редактор, удаление и генерация пустых смен. [Общий указатель](../../../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [ShiftTemplatesModels.kt](ShiftTemplatesModels.kt) | `ShiftTemplateDraft` (название, окно, база, перерыв, дни недели, дата окончания), `ShiftTemplatesUiState` (загрузка списка, `draft`/`draftError`, `busy`/`working`, сообщение, `deleteTarget`, `today`), преобразования `toDraft`/`toTemplate` и подписи `scheduleSummary`/`periodSummary` |
| [ShiftTemplatesViewModel.kt](ShiftTemplatesViewModel.kt) | Владелец раздела: наблюдение `ShiftRepository.observeTemplates()`, `newTemplate`/`editTemplate`, изменение полей, `save`, `generate` (сохраняет шаблон и создаёт пустые смены от текущей даты до даты окончания), `requestDelete`/`confirmDelete`, сообщения |
| [ShiftTemplatesViewModelFactory.kt](ShiftTemplatesViewModelFactory.kt) | Фабрика с `ShiftRepository` |
| [ShiftTemplatesScreen.kt](ShiftTemplatesScreen.kt) | Экран раздела: список карточек с днями недели, окном и датой окончания, FAB «Новый шаблон», нижняя панель редактора, диалог удаления |
| [ShiftTemplateEditor.kt](ShiftTemplateEditor.kt) | `ShiftTemplateEditorSheet` — нижняя панель настройки шаблона: название, дни недели в `FlowRow`, чекбокс и отдельная строка даты `LessonDateField`, окно времени и числа в два столбца, прокручиваемое содержимое и кнопки «Отмена»/«Сохранить»/«Создать смены» вне прокрутки; состав детей в шаблоне не сохраняется |

Маршрут: [AppScreen](../../../AppScreen.kt) (`ShiftTemplates`) → [AppDrawer](../../../AppDrawer.kt) (пункт под «Дети») → [App](../../../App.kt) → `ShiftTemplatesScreen` → `ShiftTemplatesViewModel` → [ShiftRepository](../../../../domain/repository/PACKAGE.md). Экран смены шаблон не редактирует — [shift](../PACKAGE.md).

## Проверки

- [ShiftTemplatesViewModelTest.kt](../../../../../../../../commonTest/kotlin/com/logoped_plus/ui/screen/shift/templates/ShiftTemplatesViewModelTest.kt) — test: генерация сохраняет шаблон и начинает с текущей даты, нечитаемое число оставляет черновик с ошибкой, удаление требует подтверждения.
