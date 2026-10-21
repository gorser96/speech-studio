# com.logoped_plus.ui.screen

Верхнеуровневые экраны разделов. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [ChildrenScreen.kt](ChildrenScreen.kt) | Раздел «Дети»: список с поиском (один непробельный символ, без регистра), алфавитной сортировкой (`RussianNameSort`, тёзки — по позиции) и метками назначенных тэгов в одной строке с именем через `LimitedTagChips`: порядок справочника, не более `visibleTagsLimit` (настройка, coerceIn 0–10), лишние — метка «+N» (компактные чипы, текст с многоточием), «Ничего не найдено» отличается от пустого списка; диалог добавления имени, DeleteChildDialog; хост карточки (cardId ≠ null → ChildCardView); ChildrenLoadStatus; общие TagSwatch/TagChip/SmallTagChip/LimitedTagChips |
| [ScheduleScreen.kt](ScheduleScreen.kt) | Подписка, маршрутизация editor, Loading/Error/повтор внутри форм, Back; собственных черновиков нет |
| [SettingsScreen.kt](SettingsScreen.kt) | Экран настроек (`imePadding`): тумблер «Скрывать выходные», «Часы сетки» — «Начальный час»/«Конечный час» через `NumberField` (maxDigits 2) с ошибкой при начале вне 0–23, конце вне 1–24 или конце не больше начала, «Дети» — «Сколько тэгов показывать» (0–10, дефолт 3, ошибка вне диапазона) |
| [SettingsViewModel.kt](SettingsViewModel.kt) | Состояние `hideWeekend`, `gridStartHour`/`gridEndHour`, `visibleTagsLimit` (с флагом `visibleTagsLimitInvalid`), подписка на store, `setHideWeekend`, `setGridStartHour`/`setGridEndHour`, `setVisibleTagsLimit` |
| [SettingsViewModelFactory.kt](SettingsViewModelFactory.kt) | Фабрика с [SettingsRepository](../../data/preferences/PACKAGE.md) |
| [TagsScreen.kt](TagsScreen.kt) | Раздел «Тэги»: список названий с цветом и рукояткой перетаскивания (drag & drop: `detectDragGestures` по накопленному сдвигу, шаг = высота строки + зазор, высота строки через `onSizeChanged`; порядок на время жеста — `dragOrder`, запись одного `reorderTags` по окончании; подсветка строки во время перетаскивания), блок ошибки перестановки с «Повторить»/«Отменить»; диалог редактора (имя + палитра из 8 цветов с названием и отметкой выбора), подтверждение удаления с пояснением об отвязке от детей |

Экраны подключает [App](../App.kt). Бизнес-переходы — [children](children/PACKAGE.md), [tags](tags/PACKAGE.md) и [schedule](schedule/PACKAGE.md). Настройки хранятся в [data.preferences](../../data/preferences/PACKAGE.md) и переживают перезапуск; числовые поля настроек — общий [NumberField](../components/PACKAGE.md). Месяц, неделя и день прокручивают весь экран; недельная и дневная сетки дополнительно прокручивают часы внутри блока, в неделе заголовки дней закреплены. Ошибка чтения не подменяется пустым расписанием.

## Проверки

На диске тестов для пакета нет; экраны проверялись осмотром кода (см. [статус проверок](../../../../../../../../../../openspec/verification/README.md)).
