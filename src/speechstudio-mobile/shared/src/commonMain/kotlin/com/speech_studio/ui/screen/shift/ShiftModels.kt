package com.speech_studio.ui.screen.shift

import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.model.Tag
import com.speech_studio.domain.model.WorkShift
import com.speech_studio.domain.repository.ShiftLoadState
import com.speech_studio.domain.repository.ShiftWriteResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Строка слота смены в черновике: ребёнок, длительность и вычисленное начало. */
data class ShiftSlotDraft(
    val id: String,
    val childId: String,
    val childName: String,
    val durationText: String,
    val startTime: LocalTime? = null,
    val lessonId: String? = null
)

/** Занятие смены, предлагаемое для ручного выбора в разборе видео. */
data class ShiftLessonOption(val lessonId: String, val label: String)

/** Предложение сопоставления одного видео в разборе смены. */
data class ShiftVideoRow(
    val uri: String,
    val displayName: String?,
    val createdMillis: Long,
    val suggestedLessonId: String? = null,
    val ambiguous: Boolean = false,
    val alreadyAttached: Boolean = false,
    val excluded: Boolean = false,
    val selectedLessonId: String? = null
) {
    /** Занятие, к которому видео будет привязано после подтверждения. */
    val effectiveLessonId: String? get() = if (excluded) null else selectedLessonId ?: suggestedLessonId
}

data class ShiftVideoReviewState(
    val rows: List<ShiftVideoRow>,
    val lessons: List<ShiftLessonOption>
)

/** Диалог подтверждения опасного действия смены. */
sealed interface ShiftDialog {
    data class RemoveSlot(val slotId: String, val childName: String, val hasLesson: Boolean) : ShiftDialog
    data class ChangeChild(val slotId: String, val childId: String, val childName: String) : ShiftDialog
    data object DeleteShift : ShiftDialog
    data object DeleteLessons : ShiftDialog
}

data class ShiftUiState(
    val shiftId: String? = null,
    val date: LocalDate? = null,
    val shift: WorkShift? = null,
    val loadState: ShiftLoadState = ShiftLoadState.Loading(),
    val startTime: String = "08:00",
    val endTime: String = "12:00",
    val baseDuration: String = "30",
    val breakMinutes: String = "5",
    val slots: List<ShiftSlotDraft> = emptyList(),
    val freeMinutes: Int = 0,
    val overflows: Boolean = false,
    val geometryValid: Boolean = true,
    val dirty: Boolean = false,
    val children: List<Child> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
    val saving: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val error: ShiftWriteResult.Reason? = null,
    val dialog: ShiftDialog? = null,
    val videoScanInProgress: Boolean = false,
    val videoReview: ShiftVideoReviewState? = null,
    val closeRequested: Boolean = false
) {
    val shiftReady: Boolean get() = loadState is ShiftLoadState.Ready && shift != null
    val canSave: Boolean
        get() = shiftReady && geometryValid && !saving && !busy && slots.mapNotNull { it.durationText.toIntOrNull()?.takeIf { it > 0 } }.size == slots.size
    /** Дата меняется в черновике; связанные занятия переезжают вместе со сменой при сохранении. */
    val canChangeDate: Boolean get() = shiftReady && !saving && !busy
    val canCreateLessons: Boolean get() = canSave && slots.isNotEmpty() && !overflows
    val canFindVideos: Boolean get() = shiftReady && lessons.isNotEmpty() && !videoScanInProgress && !saving
    val canDelete: Boolean get() = shiftReady && !saving && !busy
    /** Текущий блокирующий статус: используется оболочкой для запрета ухода и записи. */
    val working: Boolean get() = saving || busy
}