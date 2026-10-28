package com.speech_studio.ui.screen.shift.templates

import com.speech_studio.domain.model.ShiftTemplate
import com.speech_studio.domain.repository.ShiftWriteResult
import com.speech_studio.domain.text.RussianText
import com.speech_studio.ui.components.parseTimeText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Черновик шаблона смены для редактора. */
data class ShiftTemplateDraft(
    val id: String,
    val name: String = "",
    val startTime: String = "08:00",
    val endTime: String = "12:00",
    val baseDuration: String = "30",
    val breakMinutes: String = "5",
    val daysOfWeek: Set<Int> = emptySet(),
    val endDate: LocalDate? = null
)

/** Состояние раздела шаблонов: список, редактор, генерация и удаление. */
data class ShiftTemplatesUiState(
    val loading: Boolean = true,
    val templates: List<ShiftTemplate> = emptyList(),
    val today: LocalDate = LocalDate(1970, 1, 1),
    val draft: ShiftTemplateDraft? = null,
    val draftError: ShiftWriteResult.Reason? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val deleteTarget: ShiftTemplate? = null
) {
    /** Любая незавершённая запись блокирует навигацию и повторные действия. */
    val working: Boolean get() = busy
}

/** Черновик из сохранённого шаблона. */
fun ShiftTemplate.toDraft(): ShiftTemplateDraft = ShiftTemplateDraft(
    id = id,
    name = name,
    startTime = RussianText.formatTime(startTime),
    endTime = RussianText.formatTime(endTime),
    baseDuration = baseDurationMinutes.toString(),
    breakMinutes = breakMinutes.toString(),
    daysOfWeek = daysOfWeek.toSet(),
    endDate = endDate
)

/** Шаблон из черновика; `null`, если время или числа не разбираются. */
fun ShiftTemplateDraft.toTemplate(): ShiftTemplate? {
    val start = parseDraftTime(startTime) ?: return null
    val end = parseDraftTime(endTime) ?: return null
    val base = baseDuration.toIntOrNull() ?: return null
    val breakMinutes = breakMinutes.toIntOrNull() ?: return null
    return ShiftTemplate(id, name.trim(), start, end, base, breakMinutes, daysOfWeek.toList().sorted(), endDate)
}

/** Подпись строки списка: дни недели и окно. */
fun ShiftTemplate.scheduleSummary(): String = buildString {
    if (daysOfWeek.isEmpty()) append("Дни не выбраны") else append(
        daysOfWeek.sorted().joinToString(", ") { RussianText.weekdayShort(it) }
    )
    append(" · ")
    append(RussianText.formatTime(startTime))
    append("–")
    append(RussianText.formatTime(endTime))
    append(" · ")
    append(baseDurationMinutes)
    append(" мин · перерыв ")
    append(breakMinutes)
    append(" мин")
}

/** Подпись диапазона генерации. */
fun ShiftTemplate.periodSummary(): String = endDate?.let { "до ${RussianText.formatLongDatePadded(it)}" }
    ?: "дата окончания не задана"

private fun parseDraftTime(text: String): LocalTime? = parseTimeText(text)
