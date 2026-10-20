# com.logoped_plus.domain.text

Детерминированные русские текст, даты и сортировка без системной локали. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [RussianText.kt](RussianText.kt) | Названия месяцев (именительный/родительный/краткий), дни недели, `formatNumericDate`, `formatLongDate`, `formatLongDatePadded`, `formatDayHeader`, `formatWeekRange`, `formatHour`, `formatTime`, `formatDateTime`; используется вместо `DateTimeFormatter`/`Locale` |
| [RussianNameSort.kt](RussianNameSort.kt) | Ключ сравнения имён: нижний регистр, `ё` сравнивается как `е`, порядок по code points; `sortedBy` сохраняет исходный порядок для равных ключей (тёзки) |

Потребители — [экран детей](../../ui/screen/children/PACKAGE.md), [расписание](../../ui/screen/schedule/PACKAGE.md) и [тэги](../../ui/screen/tags/PACKAGE.md). Форматирование не зависит от языка устройства: при смене системной локали подписи не меняются.

## Проверки

- [RussianTextTest.kt](../../../../../../commonTest/kotlin/com/logoped_plus/domain/text/RussianTextTest.kt) — даты, месяцы, дни недели, время.
- [RussianNameSortTest.kt](../../../../../../commonTest/kotlin/com/logoped_plus/domain/text/RussianNameSortTest.kt) — регистр, `ё`, тёзки и стабильный порядок.
