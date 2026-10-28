## 1. Тема

- [x] 1.1 Заменить палитру `ui/theme/Color.kt` на токены светлой и тёмной схем и собрать полные `lightColorScheme`/`darkColorScheme` в `Theme.kt`.
- [x] 1.2 Отключить динамический цвет по умолчанию (`dynamicColor = false`), сохранив параметр и `platformColorScheme`.
- [x] 1.3 Задать шкалу `Shapes` (6/8/12/16/20 dp) и передать её в `MaterialTheme`.
- [x] 1.4 Переписать `Typography`: serif для заголовочных стилей, плотный sans для body и label.

## 2. Общие компоненты

- [x] 2.1 Добавить `ui/components/BentoComponents.kt` с `BentoCard`, `SectionLabel` и `BentoStatTile`.
- [x] 2.2 Добавить `RussianText.formatCount` и тест в `RussianTextTest`.

## 3. Разделы

- [x] 3.1 Оболочка: фон `background`, верхняя строка с serif-заголовком и цветами схемы (`App.kt`), плоское меню и бренд (`AppDrawer.kt`).
- [x] 3.2 Расписание: сегменты на шкале форм, календарь в карточке, выбранный день заливкой, сводка дня плитками, микро-заголовок «Занятия» (`ScheduleScreen.kt`, `MonthScheduleView.kt`, `ScheduleComponents.kt`).
- [x] 3.3 Недельная и дневная сетки: волосяные линии часов, фон блоков из ролей схемы (`WeekScheduleView.kt`, `DayScheduleView.kt`).
- [x] 3.4 Дети и карточка ребёнка: плоские карточки, поле поиска и FAB на шкале форм, секции микро-заголовками (`ChildrenScreen.kt`, `ChildCardView.kt`).
- [x] 3.5 Занятие: сведения в карточке с волосяными разделителями, подписи капсом (`LessonDetailsView.kt`).
- [x] 3.6 Тэги, шаблоны смен, смена, настройки: плоские карточки и секции, чипы на шкале форм (`TagsScreen.kt`, `ShiftTemplatesScreen.kt`, `ShiftScreen.kt`, `SettingsScreen.kt`).

## 4. Проверки и карты пакетов

- [x] 4.1 `:app:compileDebugKotlin` и unit-тест `RussianTextTest`.
- [x] 4.2 Адресный осмотр кода по сценариям delta: роли схемы, отсутствие литералов цвета вне темы и `TagPalette`, единая шкала форм, числа сводки соответствуют выбранной дате.
- [x] 4.3 Обновить `PACKAGE.md` затронутых пакетов (`ui/theme`, `ui/components`, `ui`, `ui/screen`, `ui/screen/schedule`, `domain/text`) и `git diff --check`.
- [ ] 4.4 Зафиксировать в `openspec/verification/README.md`, что внешний вид и тёмная тема требуют ручной приёмки на устройстве.
