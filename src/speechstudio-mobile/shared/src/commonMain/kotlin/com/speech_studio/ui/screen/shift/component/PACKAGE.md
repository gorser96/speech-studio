# com.logoped_plus.ui.screen.shift.component

Компоненты экрана смены: выбор детей и диалог разбора видео. [Общий указатель](../../../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [ShiftChildPicker.kt](ShiftChildPicker.kt) | `ShiftChildPickerButton` и `ShiftChildPickerSheet` — нижний лист выбора существующих детей: поиск по имени без учёта регистра, чипы фильтра по тэгам (объединение), флажки для добавления сразу нескольких детей с кнопкой «Добавить (N)», одиночный режим замены ребёнка в слоте с подтверждением по нажатию; новый ребёнок создаётся только в разделе «Дети», в листе показывается подсказка; содержимое прокручивается и учитывает клавиатуру (`imePadding`) |
| [ShiftVideoReview.kt](ShiftVideoReview.kt) | `ShiftVideoReviewDialog` — диалог подтверждения найденных видео смены: ссылка/просмотр `VideoAttachmentLink`, локальное время создания, признак неоднозначности (красный текст при попадании в несколько занятий), признак «Уже прикреплено», выпадающее меню выбора занятия и чекбокс «Исключить»; привязки применяются только по нажатию «Прикрепить» |

Потребитель — [ShiftScreen](../PACKAGE.md). Компоненты получают состояние черновика из [ShiftViewModel](../ShiftViewModel.kt), фильтрация списка детей — [ShiftChildFilter](../ShiftChildFilter.kt). Редактор шаблона вынесен в раздел [templates](../templates/PACKAGE.md).

## Проверки

- Тестируются в составе [ShiftViewModelTest.kt](../../../../../../../../commonTest/kotlin/com/speech_studio/ui/screen/shift/ShiftViewModelTest.kt), [ShiftChildFilterTest.kt](../../../../../../../../commonTest/kotlin/com/speech_studio/ui/screen/shift/ShiftChildFilterTest.kt) и сценариев разбора видео [ShiftVideoMatcherTest.kt](../../../../../../../../commonTest/kotlin/com/speech_studio/domain/usecase/ShiftVideoMatcherTest.kt).
