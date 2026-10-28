# com.logoped_plus.ui.theme

Тема Material 3 для Compose в визуальном стиле Bento: плотная плоская вёрстка, белая поверхность на серой подложке, минимальная декорация. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл / символ | Ответственность |
|---|---|
| [Theme.kt](Theme.kt) — `SpeechStudioTheme` | Выбор тёмной/светлой схемы (динамический цвет отключён) и передача `Typography`/`Shapes` в `MaterialTheme` |
| [Theme.kt](Theme.kt) — `expect platformColorScheme` | Точка расширения для системной палитры (не используется, возвращает `null` на обеих платформах) |
| [Theme.android.kt](../../../../../../androidMain/kotlin/com/speech_studio/ui/theme/Theme.android.kt) | Всегда `null` (динамические цвета Android 12+ отключены) |
| [Theme.ios.kt](../../../../../../iosMain/kotlin/com/speech_studio/ios/Theme.ios.kt) | Всегда `null` |
| [Color.kt](Color.kt) | Светлая/тёмная палитры Bento: teal `#11675A` primary, серая подложка `#F1F2F4`, белая surface; контейнер выделения занятия/смены `#C6E2DA` заметно отличается от фона; контраст ≥4.5 (WCAG AA) |
| [Type.kt](Type.kt) — `Typography` | Шрифт Merriweather для `titleLarge`, уменьшенные размеры тела 14/13/12sp; рабочие подписи 11–13sp с минимальным трекингом, микро-заголовки `SectionLabel` — капсом с увеличенным трекингом |
| [Theme.kt](Theme.kt) — `Shapes` | `extraSmall=6.dp`, `small=8.dp`, `medium=12.dp`, `large=16.dp`, `extraLarge=20.dp` (плавная шкала без резких скачков) |

## Связи

- Подключается в Android [MainActivity.kt](../../../../../../../../app/src/main/java/com/speech_studio/MainActivity.kt) и iOS [MainViewController.kt](../../../../../../iosMain/kotlin/com/speech_studio/ios/MainViewController.kt).
- Компоненты Bento ([ui/components/BentoComponents.kt](../components/BentoComponents.kt)): `BentoCard`, `BentoStatTile`, `SectionLabel`, `bentoTextFieldColors()`.
- Применена во всех экранах: расписание (месяц/неделя/день), дети, карточка ребёнка, занятие, тэги, настройки, смены.

## Ограничения

- Динамический цвет Android 12+ намеренно отключён: приложение всегда использует фиксированную палитру Bento.
- Формы объявлены в центральной шкале `Theme.kt`; хардкод `RoundedCornerShape` в компонентах устранён.
- Палитра данных (`TagPalette.kt`) сохраняет цвета тэгов без изменений: Bento влияет только на контейнеры и фоны интерфейса.
