## Context

Мотивация и границы — [proposal.md](proposal.md). Фактическое состояние проверено по коду:

- `ui/theme/Theme.kt` передаёт в `MaterialTheme` только `colorScheme` и `typography`; параметр `shapes` не задан, поэтому действуют дефолтные формы M3 (4/8/12/16/28 dp). `dynamicColor: Boolean = true`, на Android 12+ `Theme.android.kt` подменяет схему системной, на iOS `platformColorScheme` возвращает `null`.
- `ui/theme/Color.kt` — шаблонная палитра студии (`Purple40 0xFF6650a4`), `Type.kt` переопределяет только `bodyLarge`.
- Экраны читают роли `MaterialTheme.colorScheme.*` и `MaterialTheme.shapes.*`, хардкода цветов в UI нет: единственные литералы — `theme/Color.kt` и `screen/tags/TagPalette.kt`. Значит смена вида достигается токенами темы и точечной разметкой.
- Общего компонента карточки нет: «карточка-строка» скопирована в `ScheduleComponents.kt` (`LessonItem`, `ShiftContainerRow`), карточки списков — дефолтные `Card` в `ChildrenScreen.kt` и `TagsScreen.kt`; заголовки секций — голый `Text(style = titleMedium)` в шести файлах.
- Хардкод радиусов: `RoundedCornerShape(8.dp)` в сегментах расписания, `14.dp` у логотипа в меню, `2.dp` у пункта меню, `5.dp`/`6.dp`/`12.dp` в тэгах и карточке ребёнка.
- `MonthScheduleView` уже получает все занятия и смены и сам фильтрует их по выбранной дате, поэтому сводка дня не требует изменений `ScheduleViewModel`.

Изменения относятся к Presentation ([ARCHITECTURE.md, разделы 4 и 25](../../../src/speechstudio-mobile/ARCHITECTURE.md)): данных, repository и use case не добавляем; единственное исключение — формат числа в существующем `domain/text/RussianText.kt`.

## Goals / Non-Goals

**Goals:** собственный узнаваемый вид, не зависящий от системной палитры; плоская плиточная композиция на всех разделах; один набор форм и типографики; общие компоненты вместо копирования разметки.

**Non-Goals:** новые экраны, переходы, данные и настройки; смена состава информации на экранах (кроме сводки дня); новые шрифтовые файлы, иконки и сетевые ресурсы; тёмная схема как отдельная тема с переключателем.

## Decisions

- Отключить динамический цвет по умолчанию: `SpeechStudioTheme(dynamicColor: Boolean = false)`. Параметр и `expect platformColorScheme` сохраняются, чтобы поведение можно было вернуть без переделки темы. Альтернатива «оставить динамический цвет» противоречит `visual-style`: акцент и фон перестают быть предсказуемыми.
- Задать роли схемы явно в `lightColorScheme`/`darkColorScheme`: `background #F1F2F4`, `surface #FFFFFF`, `surfaceVariant #EDEEF1`, `onSurface #111318`, `onSurfaceVariant #5F6774`, `outlineVariant #E4E6EA`, `primary #11675A`, `onPrimary #FFFFFF`, `primaryContainer #E8F2EF`, `onPrimaryContainer #0C4E45`, `secondaryContainer #E8F2EF`, `onSecondaryContainer #0C4E45`, ошибка — существующая `#B3261E`. Экраны уже используют эти роли, поэтому перекраска не требует правок в каждом файле. Контраст вторичного текста `#5F6774` на фоне `#F1F2F4` — 5.1:1, в тёмной схеме `#A8AFBA` на `#17191C` — 7.9:1.
- Ввести шкалу форм `Shapes(extraSmall 6, small 8, medium 12, large 16, extraLarge 20 dp)` и заменить хардкоды радиусов ссылками на шкалу: `baseShape` сегментов и пункт меню — `small`, логотип — `medium`, чипы тэгов — `small`, поля ввода — `small`. Дефолтную овальную форму FAB заменить на `large`: плиточная композиция держится на прямоугольных скруглениях.
- Типографика: `titleLarge`/`headlineSmall`/`headlineMedium`/`displaySmall` — `FontFamily.Serif` с отрицательным трекингом; `bodyLarge`/`bodyMedium` — sans 15/14 sp; `titleMedium` — sans 15 sp полужирный для плотных строк. Serif платформенный, файлы шрифтов не добавляются.
- Капс для микро-заголовков делать в компоненте `SectionLabel` (`uppercase()`, `labelSmall`, трекинг 1 sp, цвет `onSurfaceVariant`): в Compose нет text-transform, поэтому стиль текста капс не задаёт.
- Карточки делать плоскими: `Surface(color = surface, shape = shapes.large, border = BorderStroke(1.dp, outlineVariant), shadowElevation = 0.dp)`. Дефолтные `Card` в списках детей и тэгов, а также `CardDefaults.cardElevation` заменяются этим компонентом; вложенные блоки (занятие внутри дня, смена) различаются фоном `primaryContainer`, а не тенью.
- Добавить в `ui/components/BentoComponents.kt` три компонента — `BentoCard`, `SectionLabel`, `BentoStatTile` — и использовать их вместо копий разметки в `ScheduleComponents.kt`, `ChildrenScreen.kt`, `TagsScreen.kt`, `ShiftTemplatesScreen.kt`, `ChildCardView.kt`, `ShiftScreen.kt`, `SettingsScreen.kt`.
- Сводку дня считать внутри `MonthScheduleView` из уже переданных `lessons` и `shifts`: `lessonsForSelectedDate` и `shiftsForSelectedDate` вычисляются там же, состояние и ViewModel не меняются. Подписи чисел — новый `RussianText.formatCount(value, one, few, many)`.
- Сегменты «Месяц / Неделя / День» сохраняют `SegmentedButton` и требование «умеренного скругления»: меняются только `baseShape` (шкала `small`) и цвета — выбранный сегмент `surface`/`onSurface`, остальные `surfaceVariant`.
- `TagPalette` и `tagLabelColor` не меняются: цвет остаётся маркером данных, контраст подписи на чипе уже обеспечен выбором чёрного или белого текста.

## Risks / Trade-offs

- Платформенный serif различается на Android и iOS (Noto Serif против системного Times): метрики заголовков расходятся → проверить по коду, что длинные названия («Шаблоны смен», «Настройки») не вытесняют кнопки верхней строки; при переполнении вернуться к sans одним значением в `Type.kt`.
- Отказ от теней снижает различимость вложенных блоков → границей служат фон `#F1F2F4` и волосяная линия `outlineVariant`; проверить контраст линии на белом (`#E4E6EA` — декоративная, не текст) и в тёмной схеме.
- Отключение динамического цвета заметно пользователям, привыкшим к системной палитре → это осознанно следует из `visual-style`, параметр темы сохранён.
- Плитки сводки дублируют часть сведений дня → считать числа один раз и переиспользовать отфильтрованные списки, чтобы не фильтровать дважды и не показывать данные другого дня.
- Полная перекраска затрагивает много файлов без автотестов UI → риск регрессии навигации и обработчиков: правки ограничить оформлением, не менять параметры и колбэки существующих composable.

## Проверки

- Сборка `:app:compileDebugKotlin` подтверждает, что общий Compose-код и новые компоненты совместимы с Android-таргетом и iOS-исходниками.
- Unit-тест `RussianTextTest` на формат числа: 1, 2, 5, 11, 21, 0.
- Адресный осмотр кода по сценариям delta `visual-style` и `browse-schedule`: роли схемы в светлой и тёмной темах, отсутствие `Color(0x…)` вне темы и палитры тэгов, единая шкала форм, плоские карточки, микро-заголовки, соответствие чисел сводки выбранной дате.
- `git diff --check`; обновить карты `ui/theme`, `ui/components`, `ui/screen`, `ui/screen/schedule`, `domain/text`.
- По действующему ограничению проекта эмулятор, установка и connected-тесты не выполняются: фактический внешний вид, тёмная тема и увеличенный системный шрифт требуют ручной приёмки на устройстве и сборкой не подтверждаются.
