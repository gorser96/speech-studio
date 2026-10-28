package com.speech_studio.ui.screen

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.speech_studio.MainActivity
import com.speech_studio.ui.screen.children.formatEndDate
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class ChildCardTagsUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun resetDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf("children.db", "children.db-wal", "children.db-shm").forEach {
            context.getDatabasePath(it).delete()
        }
    }

    @Test
    fun cardTagsAndRecurringScheduleScenario() {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val endDate = today.plusDays(14)
        val candidateDays = (0..14).map { today.plusDays(it.toLong()) }
            .filter { it.dayOfWeek == DayOfWeek.THURSDAY && !it.atTime(16, 0).isBefore(now) }
        assertTrue("тест требует хотя бы одного будущего четверга в окне", candidateDays.isNotEmpty())
        val expectedCreated = candidateDays.size
        val firstCandidate = candidateDays.first()

        openDrawer()
        click("Тэги")
        assertShown("Тэгов пока нет")

        createTag("Группа А", "Красный")
        createTag("Б", null)
        assertShown("Группа А")
        compose.onNodeWithText("Б", substring = false).assertIsDisplayed()

        compose.onNodeWithContentDescription("Добавить тэг").performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Название").isDisplayed() }
        editable(0).performTextReplacement("группа а")
        click("Добавить")
        compose.waitUntil(5_000) { compose.onNodeWithText("Тэг с таким названием уже есть").isDisplayed() }
        click("Отмена")
        compose.waitUntil(5_000) { absent("Тэгов пока нет") }

        pressBack()
        openDrawer()
        click("Дети")
        assertShown("Детей пока нет")

        addChild("Анна")
        addChild("Борис")
        run {
            val nodes = compose.onAllNodesWithText("Анна", substring = false).fetchSemanticsNodes(false, null)
            println("DIAG nodes 'Анна': ${nodes.size}")
            nodes.forEachIndexed { i, n ->
                val text = (n.config[androidx.compose.ui.semantics.SemanticsProperties.Text] as? List<*>)
                    ?.map { (it as? androidx.compose.ui.text.AnnotatedString)?.text }
                println("DIAG node#$i deactivated=${n.layoutInfo.isDeactivated} text=$text")
            }
        }
        compose.onNodeWithText("Анна", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Борис", substring = false).assertIsDisplayed()

        click("Анна")
        compose.waitUntil(5_000) { compose.onNodeWithText("Редактировать").isDisplayed() }
        assertShown("Теги не назначены")
        assertShown("Периодического расписания нет")

        click("Редактировать")
        compose.waitUntil(5_000) { compose.onNodeWithText("Добавить строку").isDisplayed() }
        assertChildNameIs("Анна")

        compose.activity.recreate()
        compose.waitUntil(10_000) { compose.onNodeWithText("Добавить строку").isDisplayed() }
        assertChildNameIs("Анна")

        compose.onNodeWithText("Группа А").performClick()
        compose.waitUntil(5_000) { compose.onNodeWithContentDescription("Назначен").isDisplayed() }

        click("Добавить строку")
        compose.waitUntil(5_000) { compose.onNodeWithText(shortDayName(today.dayOfWeek.value), substring = false).isDisplayed() }
        click(shortDayName(today.dayOfWeek.value))
        compose.waitUntil(5_000) { compose.onNodeWithText("Четверг").isDisplayed() }
        click("Четверг")
        editable(1).performTextReplacement("16:00")

        val initialMonth = today.year * 12 + today.monthValue
        val targetMonth = endDate.year * 12 + endDate.monthValue
        click("Указать дату окончания")
        compose.waitUntil(5_000) { compose.onNodeWithText("ОК").isDisplayed() }
        for (i in 0 until (targetMonth - initialMonth)) {
            clickNextMonthInDatePicker()
        }
        compose.onNodeWithText(endDate.dayOfMonth.toString(), substring = false).performClick()
        click("ОК")
        compose.waitUntil(5_000) { compose.onNodeWithText("Дата окончания: ${formatEndDate(endDate)}").isDisplayed() }

        click("Сохранить")
        compose.waitUntil(5_000) { compose.onNodeWithText("Создать занятия").isDisplayed() }
        assertShown("Группа А")
        assertShown("Четверг · 16:00 · 40 мин")
        assertShown("Дата окончания: ${formatEndDate(endDate)}")

        click("Создать занятия")
        compose.waitUntil(10_000) {
            compose.onNodeWithText("Создано: $expectedCreated; уже существуют: 0", substring = false).isDisplayed()
        }
        click("Создать занятия")
        compose.waitUntil(10_000) {
            compose.onNodeWithText("Создано: 0; уже существуют: $expectedCreated", substring = false).isDisplayed()
        }

        pressBack()
        compose.waitUntil(5_000) { compose.onNodeWithText("Поиск по имени").isDisplayed() }
        editable(0).performTextReplacement("а")
        compose.waitUntil(5_000) { compose.onNodeWithText("Анна", substring = false).isDisplayed() }
        compose.onNodeWithText("Борис", substring = false).assertDoesNotExist()
        editable(0).performTextReplacement("zzz")
        compose.waitUntil(5_000) { compose.onNodeWithText("Ничего не найдено").isDisplayed() }
        editable(0).performTextReplacement("")
        compose.waitUntil(5_000) { compose.onNodeWithText("Анна", substring = false).isDisplayed() }
        compose.onNodeWithText("Борис", substring = false).assertIsDisplayed()

        openDrawer()
        click("Тэги")
        compose.onAllNodesWithContentDescription("Удалить тэг")[0].performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Удалить тэг?").isDisplayed() }
        assertShown("будет удалён из справочника и отвязан от детей")
        click("Удалить")
        compose.waitUntil(5_000) { absent("Группа А") }

        openDrawer()
        click("Дети")
        click("Анна")
        compose.waitUntil(5_000) { compose.onNodeWithText("Редактировать").isDisplayed() }
        assertShown("Теги не назначены")

        click("Удалить")
        compose.waitUntil(5_000) { compose.onNodeWithText("Удалить ребёнка?").isDisplayed() }
        assertShown("Его занятия сохранятся в истории с именем.")
        compose.onAllNodesWithText("Удалить", substring = false)[lastIndex("Удалить", substring = false)].performClick()
        compose.waitUntil(5_000) { absent("Анна") }
        compose.onNodeWithText("Борис", substring = false).assertIsDisplayed()

        pressBack()
        compose.waitUntil(10_000) { compose.onNodeWithText(monthYearLabel(today), substring = false).isDisplayed() }
        if (firstCandidate.year != today.year || firstCandidate.monthValue != today.monthValue) {
            compose.onNodeWithText("›").performClick()
            compose.waitUntil(10_000) { compose.onNodeWithText(monthYearLabel(firstCandidate), substring = false).isDisplayed() }
        }
        compose.onNodeWithText(firstCandidate.dayOfMonth.toString(), substring = false).performClick()
        compose.waitUntil(15_000) { compose.onNodeWithText("Анна", substring = false).isDisplayed() }

        openDrawer()
        click("Тэги")
        compose.onAllNodesWithContentDescription("Удалить тэг")[lastIndex("Удалить тэг", null)].performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Удалить тэг?").isDisplayed() }
        click("Удалить")
        compose.waitUntil(5_000) { compose.onNodeWithText("Тэгов пока нет").isDisplayed() }

        openDrawer()
        click("Дети")
        click("Борис")
        compose.waitUntil(5_000) { compose.onNodeWithText("Редактировать").isDisplayed() }
        click("Редактировать")
        compose.waitUntil(5_000) { compose.onNodeWithText("Тэгов пока нет. Создайте их в разделе «Тэги».").isDisplayed() }
        click("Отмена")
        compose.waitUntil(5_000) { compose.onNodeWithText("Редактировать").isDisplayed() }
        pressBack()
        compose.waitUntil(5_000) { compose.onNodeWithText("Поиск по имени").isDisplayed() }
    }

    @Test
    fun backArrowReturnsFromCardToList() {
        val uniqueName = "ТестВозврат" + System.currentTimeMillis()
        openDrawer()
        click("Дети")
        compose.waitUntil(10_000) { compose.onNodeWithText("Поиск по имени").isDisplayed() }
        addChild(uniqueName)

        compose.onNodeWithText(uniqueName, substring = false).performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Редактировать").isDisplayed() }

        compose.onNodeWithContentDescription("К списку детей").assertIsDisplayed()
        compose.onNodeWithContentDescription("К списку детей").performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Поиск по имени").isDisplayed() }
        compose.onNodeWithText(uniqueName, substring = false).assertIsDisplayed()

        compose.onNodeWithContentDescription("К списку детей").assertDoesNotExist()
        compose.onNodeWithContentDescription("Открыть меню").assertIsDisplayed()
    }

    private fun openDrawer() {
        compose.onNodeWithContentDescription("Открыть меню").performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Настройки").isDisplayed() }
    }

    private fun click(text: String) {
        compose.onNodeWithText(text, substring = false).performClick()
    }

    private fun assertShown(text: String) {
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun pressBack() {
        Espresso.pressBack()
    }

    private fun absent(text: String): Boolean =
        compose.onAllNodesWithText(text).fetchSemanticsNodes(false, null).isEmpty()

    private fun lastIndex(text: String, substring: Boolean?): Int =
        (compose.onAllNodesWithText(text, substring == true).fetchSemanticsNodes(false, null).size - 1)

    private fun editable(index: Int) = compose.onAllNodes(isEditable())[index]

    private fun assertChildNameIs(name: String) {
        compose.waitUntil(5_000) { compose.onNodeWithText("Имя").isDisplayed() }
        val node = editable(0).fetchSemanticsNode()
        val texts = (node.config[SemanticsProperties.Text] as? List<*>)
            ?.joinToString(" ") { (it as? androidx.compose.ui.text.AnnotatedString)?.text ?: it.toString() } ?: ""
        assertTrue("поле имени должно содержать «$name», факт: «$texts»", texts.contains(name))
    }

    private fun createTag(name: String, color: String?) {
        compose.onNodeWithContentDescription("Добавить тэг").performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Название").isDisplayed() }
        editable(0).performTextReplacement(name)
        if (color != null) {
            click("Синий")
            compose.waitUntil(5_000) { compose.onNodeWithText(color, substring = false).isDisplayed() }
            click(color)
            compose.waitUntil(5_000) { absent("Цвет") }
        }
        click("Добавить")
        compose.waitUntil(5_000) { absent("Название") }
    }

    private fun addChild(name: String) {
        compose.onNodeWithContentDescription("Добавить ребёнка").performClick()
        compose.waitUntil(5_000) { compose.onNodeWithText("Имя").isDisplayed() }
        editable(0).performTextReplacement(name)
        click("Добавить")
        compose.waitUntil(5_000) { compose.onNodeWithText(name, substring = false).isDisplayed() }
    }

    private fun clickNextMonthInDatePicker() {
        for (desc in listOf("Next month", "Следующий месяц")) {
            val nodes = compose.onAllNodesWithContentDescription(desc, true).fetchSemanticsNodes(false, null)
            if (nodes.isNotEmpty()) {
                compose.onAllNodesWithContentDescription(desc, true)[nodes.size - 1].performClick()
                return
            }
        }
        throw AssertionError("Кнопка перехода на следующий месяц в DatePicker не найдена")
    }

    private fun shortDayName(isoDay: Int): String = when (isoDay) {
        1 -> "Пн"; 2 -> "Вт"; 3 -> "Ср"; 4 -> "Чт"; 5 -> "Пт"; 6 -> "Сб"; else -> "Вс"
    }

    private fun monthYearLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("ru")))

    private fun createComposeRule() =
        androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()
}
