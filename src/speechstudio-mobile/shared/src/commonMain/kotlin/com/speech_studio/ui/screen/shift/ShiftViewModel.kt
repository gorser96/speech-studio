package com.speech_studio.ui.screen.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.DEFAULT_SHIFT_BASE_DURATION_MINUTES
import com.speech_studio.domain.model.ShiftSlot
import com.speech_studio.domain.model.ShiftTimeCalculator
import com.speech_studio.domain.model.WorkShift
import com.speech_studio.domain.repository.ChildRepository
import com.speech_studio.domain.repository.CreateShiftLessonsResult
import com.speech_studio.domain.repository.LessonRepository
import com.speech_studio.domain.repository.ShiftRepository
import com.speech_studio.domain.repository.ShiftVideoAssignment
import com.speech_studio.domain.repository.ShiftWriteResult
import com.speech_studio.domain.repository.TagLoadState
import com.speech_studio.domain.repository.TagRepository
import com.speech_studio.domain.text.RussianText
import com.speech_studio.domain.usecase.FindShiftVideosUseCase
import com.speech_studio.domain.usecase.ShiftLessonVideoTarget
import com.speech_studio.platform.randomUuidString
import com.speech_studio.ui.components.parseTimeText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Владелец черновика смены: дата, окно, база, перерыв, слоты, создание занятий и разбор видео.
 * Данные читаются/пишутся через `ShiftRepository`.
 */
class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val childRepository: ChildRepository,
    private val lessonRepository: LessonRepository,
    private val tagRepository: TagRepository,
    private val findShiftVideos: FindShiftVideosUseCase
) : ViewModel() {

    private val mutableState = MutableStateFlow(ShiftUiState(
        children = childRepository.state.value.children,
        tags = tagsOf(tagRepository.state.value),
        loadState = shiftRepository.state.value
    ))
    val uiState = mutableState.asStateFlow()

    private var shiftJob: Job? = null

    init {
        viewModelScope.launch {
            childRepository.state.collect { load ->
                mutableState.update { state ->
                    state.copy(children = load.children,
                        slots = state.slots.map { it.copy(childName = childName(load.children, it.childId)) })
                }
            }
        }
        viewModelScope.launch {
            lessonRepository.state.collect { load ->
                mutableState.update { state ->
                    state.copy(lessons = load.lessons.filter { it.shiftId == state.shiftId })
                }
            }
        }
        viewModelScope.launch {
            tagRepository.state.collect { load ->
                mutableState.update { state -> state.copy(tags = tagsOf(load)) }
            }
        }
    }

    fun open(shiftId: String) {
        if (mutableState.value.shiftId == shiftId) return
        shiftJob?.cancel()
        mutableState.value = ShiftUiState(
            shiftId = shiftId,
            children = childRepository.state.value.children,
            tags = tagsOf(tagRepository.state.value),
            loadState = shiftRepository.state.value,
            lessons = lessonRepository.state.value.lessons.filter { it.shiftId == shiftId }
        )
        shiftJob = viewModelScope.launch {
            shiftRepository.observeShift(shiftId).collect { shift -> applySnapshot(shift) }
        }
    }

    fun close() {
        shiftJob?.cancel()
        shiftJob = null
        mutableState.value = ShiftUiState(
            children = childRepository.state.value.children,
            tags = tagsOf(tagRepository.state.value),
            loadState = shiftRepository.state.value
        )
    }

    // --- Дата, окно, база, перерыв ---

    /** Переносит черновик смены на другую дату; связанные занятия последуют при сохранении. */
    fun changeDate(value: LocalDate) {
        if (!mutableState.value.canChangeDate) return
        mutate { it.copy(date = value) }
    }

    fun changeStartTime(value: String) = mutateWindow { it.copy(startTime = value) }

    fun changeEndTime(value: String) = mutateWindow { it.copy(endTime = value) }

    fun changeBaseDuration(value: String) = mutateWindow { it.copy(baseDuration = value) }

    fun changeBreakMinutes(value: String) = mutateWindow { it.copy(breakMinutes = value) }

    // --- Слоты ---

    fun addChild(childId: String, childName: String) {
        addChildren(listOf(Child(childId, childName)))
    }

    /** Добавляет по слоту на каждого выбранного ребёнка, сохраняя порядок выбора. */
    fun addChildren(children: List<Child>) {
        if (children.isEmpty()) return
        val base = mutableState.value.baseDuration.toIntOrNull()?.takeIf { it > 0 }
            ?: DEFAULT_SHIFT_BASE_DURATION_MINUTES
        val slots = children.map { child ->
            ShiftSlotDraft(randomUuidString(), child.id, child.name, base.toString())
        }
        mutate { it.copy(slots = it.slots + slots) }
    }

    fun changeSlotDuration(slotId: String, value: String) = mutate { state ->
        state.copy(slots = state.slots.map { if (it.id == slotId) it.copy(durationText = value) else it })
    }

    fun moveSlot(from: Int, to: Int) {
        val state = mutableState.value
        if (state.saving || from !in state.slots.indices || to !in state.slots.indices || from == to) return
        mutate { s ->
            val list = s.slots.toMutableList()
            list.add(to, list.removeAt(from))
            s.copy(slots = list)
        }
    }

    fun moveSlotUp(slotId: String) {
        val index = mutableState.value.slots.indexOfFirst { it.id == slotId }
        if (index > 0) moveSlot(index, index - 1)
    }

    fun moveSlotDown(slotId: String) {
        val index = mutableState.value.slots.indexOfFirst { it.id == slotId }
        if (index >= 0 && index < mutableState.value.slots.lastIndex) moveSlot(index, index + 1)
    }

    fun removeSlot(slotId: String) {
        val slot = mutableState.value.slots.find { it.id == slotId } ?: return
        if (slot.lessonId != null) {
            mutableState.update { it.copy(dialog = ShiftDialog.RemoveSlot(slotId, slot.childName, true)) }
        } else {
            mutate { it.copy(slots = it.slots.filter { s -> s.id != slotId }) }
        }
    }

    fun requestChangeChild(slotId: String, childId: String, childName: String) {
        val slot = mutableState.value.slots.find { it.id == slotId } ?: return
        if (slot.lessonId != null && slot.childId != childId) {
            mutableState.update { it.copy(dialog = ShiftDialog.ChangeChild(slotId, childId, childName)) }
        } else {
            applyChildChange(slotId, childId, childName)
        }
    }

    // --- Сохранение и занятия ---

    fun save() {
        val state = mutableState.value
        if (!state.canSave) return
        mutableState.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = persist()) {
                is PersistResult.Success -> mutableState.update {
                    it.copy(saving = false, dirty = false, shift = result.shift, message = "Смена сохранена.")
                }
                is PersistResult.Failure -> mutableState.update { it.copy(saving = false, error = result.reason) }
            }
        }
    }

    fun createLessons() {
        val state = mutableState.value
        if (!state.canCreateLessons) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            if (state.dirty) {
                when (val result = persist()) {
                    is PersistResult.Success -> mutableState.update { it.copy(shift = result.shift, dirty = false) }
                    is PersistResult.Failure -> {
                        mutableState.update { it.copy(busy = false, error = result.reason) }
                        return@launch
                    }
                }
            }
            val shiftId = mutableState.value.shiftId
            if (shiftId == null) {
                mutableState.update { it.copy(busy = false, error = ShiftWriteResult.Reason.NotFound) }
                return@launch
            }
            when (val result = shiftRepository.createLessons(shiftId)) {
                is CreateShiftLessonsResult.Success -> mutableState.update {
                    it.copy(busy = false, message = "Создано занятий: ${result.created}, связано: ${result.linked}.")
                }
                is CreateShiftLessonsResult.Failure -> mutableState.update {
                    it.copy(busy = false, message = createLessonsErrorText(result.reason))
                }
            }
        }
    }

    fun requestDeleteShift() {
        if (!mutableState.value.canDelete) return
        mutableState.update { it.copy(dialog = ShiftDialog.DeleteShift) }
    }

    fun requestDeleteLessons() {
        if (!mutableState.value.canDelete) return
        mutableState.update { it.copy(dialog = ShiftDialog.DeleteLessons) }
    }

    // --- Разбор видео ---

    fun findVideos() {
        val state = mutableState.value
        val shift = state.shift ?: return
        if (state.lessons.isEmpty() || state.videoScanInProgress) return
        mutableState.update { it.copy(videoScanInProgress = true, error = null, message = null) }
        val targets = state.lessons.map { lesson ->
            ShiftLessonVideoTarget(
                lessonId = lesson.id,
                childName = lesson.participants.firstOrNull()?.let { p ->
                    p.removedName ?: childName(state.children, p.childId)
                }.orEmpty(),
                start = lesson.scheduledAt,
                durationMinutes = lesson.durationMinutes
            )
        }
        val attached = state.lessons.associate { it.id to it.videoAttachments.map { v -> v.uri }.toSet() }
        viewModelScope.launch {
            val review = findShiftVideos(shift, targets, attached)
            val options = review.lessons.map { target ->
                ShiftLessonOption(target.lessonId, "${RussianText.formatTime(target.start.time)} · ${target.childName}".trim())
            }
            val rows = review.entries.map { entry ->
                ShiftVideoRow(
                    uri = entry.video.uri,
                    displayName = entry.video.displayName,
                    createdMillis = entry.video.createdMillis,
                    suggestedLessonId = entry.suggestedLessonId,
                    ambiguous = entry.ambiguous,
                    alreadyAttached = entry.alreadyAttached,
                    excluded = entry.ambiguous
                )
            }
            mutableState.update { it.copy(videoScanInProgress = false, videoReview = ShiftVideoReviewState(rows, options)) }
        }
    }

    fun setVideoLesson(uri: String, lessonId: String?) = updateReview { rows ->
        rows.map { if (it.uri == uri) it.copy(selectedLessonId = lessonId, excluded = lessonId == null && it.ambiguous) else it }
    }

    fun toggleVideoExcluded(uri: String) = updateReview { rows ->
        rows.map { if (it.uri == uri) it.copy(excluded = !it.excluded) else it }
    }

    fun confirmVideoReview() {
        val state = mutableState.value
        val review = state.videoReview ?: return
        val shiftId = state.shiftId ?: return
        val assignments = review.rows.mapNotNull { row ->
            row.effectiveLessonId?.let { ShiftVideoAssignment(row.uri, it) }
        }
        if (assignments.isEmpty()) {
            mutableState.update { it.copy(videoReview = null) }
            return
        }
        mutableState.update { it.copy(busy = true, videoReview = null, error = null, message = null) }
        viewModelScope.launch {
            when (val result = shiftRepository.attachVideos(shiftId, assignments)) {
                ShiftWriteResult.Success -> mutableState.update {
                    it.copy(busy = false, message = "Видео прикреплено: ${assignments.size}.")
                }
                is ShiftWriteResult.Failure -> mutableState.update { it.copy(busy = false, error = result.reason) }
            }
        }
    }

    fun dismissVideoReview() {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(videoReview = null) }
    }

    // --- Диалоги и сообщения ---

    fun dismissDialog() {
        if (mutableState.value.working) return
        mutableState.update { it.copy(dialog = null) }
    }

    fun confirmDialog() {
        val state = mutableState.value
        when (val dialog = state.dialog) {
            is ShiftDialog.RemoveSlot -> {
                mutate { it.copy(slots = it.slots.filter { s -> s.id != dialog.slotId }, dialog = null) }
                save()
            }
            is ShiftDialog.ChangeChild -> {
                applyChildChange(dialog.slotId, dialog.childId, dialog.childName)
                save()
            }
            ShiftDialog.DeleteShift -> {
                mutableState.update { it.copy(dialog = null) }
                deleteShift()
            }
            ShiftDialog.DeleteLessons -> {
                mutableState.update { it.copy(dialog = null) }
                deleteShiftLessons()
            }
            null -> Unit
        }
    }

    fun clearMessage() {
        mutableState.update { it.copy(message = null, error = null) }
    }

    fun consumeCloseRequest() {
        mutableState.update { it.copy(closeRequested = false) }
    }

    // --- Внутреннее ---

    private sealed interface PersistResult {
        data class Success(val shift: WorkShift) : PersistResult
        data class Failure(val reason: ShiftWriteResult.Reason) : PersistResult
    }

    private suspend fun persist(): PersistResult {
        val snapshot = mutableState.value
        val shift = snapshot.shift ?: return PersistResult.Failure(ShiftWriteResult.Reason.NotFound)
        val start = parseTimeText(snapshot.startTime) ?: return PersistResult.Failure(ShiftWriteResult.Reason.InvalidData)
        val end = parseTimeText(snapshot.endTime) ?: return PersistResult.Failure(ShiftWriteResult.Reason.InvalidData)
        val base = snapshot.baseDuration.toIntOrNull() ?: return PersistResult.Failure(ShiftWriteResult.Reason.InvalidData)
        val breakMinutes = snapshot.breakMinutes.toIntOrNull() ?: return PersistResult.Failure(ShiftWriteResult.Reason.InvalidData)
        val date = snapshot.date ?: shift.date
        val stores = snapshot.slots.mapIndexed { index, slot ->
            val duration = slot.durationText.toIntOrNull() ?: return PersistResult.Failure(ShiftWriteResult.Reason.InvalidData)
            val begin = slot.startTime ?: return PersistResult.Failure(ShiftWriteResult.Reason.InvalidData)
            ShiftSlot(slot.id, index, begin, duration, slot.childId, slot.lessonId)
        }
        val toSave = shift.copy(
            date = date, startTime = start, endTime = end,
            baseDurationMinutes = base, breakMinutes = breakMinutes, slots = stores
        )
        return when (val result = shiftRepository.saveShift(toSave)) {
            ShiftWriteResult.Success -> PersistResult.Success(toSave)
            is ShiftWriteResult.Failure -> PersistResult.Failure(result.reason)
        }
    }

    private fun deleteShift() {
        val shiftId = mutableState.value.shiftId ?: return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = shiftRepository.deleteShift(shiftId)) {
                ShiftWriteResult.Success -> mutableState.update { it.copy(busy = false, closeRequested = true) }
                is ShiftWriteResult.Failure -> mutableState.update { it.copy(busy = false, error = result.reason) }
            }
        }
    }

    private fun deleteShiftLessons() {
        val shiftId = mutableState.value.shiftId ?: return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = shiftRepository.deleteShiftLessons(shiftId)) {
                ShiftWriteResult.Success -> mutableState.update {
                    it.copy(busy = false, message = "Занятия смены удалены.")
                }
                is ShiftWriteResult.Failure -> mutableState.update { it.copy(busy = false, error = result.reason) }
            }
        }
    }

    private fun applySnapshot(shift: WorkShift?) {
        mutableState.update { state ->
            if (shift == null) {
                state.copy(shift = null)
            } else if (state.dirty || state.saving) {
                // Незавершённый черновик не перезаписывается внешним снимком, включая дату.
                state.copy(shift = shift)
            } else {
                recompute(state.copy(
                    shift = shift,
                    date = shift.date,
                    startTime = RussianText.formatTime(shift.startTime),
                    endTime = RussianText.formatTime(shift.endTime),
                    baseDuration = shift.baseDurationMinutes.toString(),
                    breakMinutes = shift.breakMinutes.toString(),
                    slots = shift.slots.map { slot ->
                        ShiftSlotDraft(slot.id, slot.childId, childName(state.children, slot.childId),
                            slot.durationMinutes.toString(), slot.startTime, slot.lessonId)
                    }
                ))
            }
        }
    }

    private fun mutateWindow(transform: (ShiftUiState) -> ShiftUiState) {
        if (mutableState.value.saving) return
        mutate(transform)
    }

    private fun mutate(transform: (ShiftUiState) -> ShiftUiState) {
        mutableState.update { state -> recompute(transform(state).copy(dirty = true, message = null, error = null)) }
    }

    private fun applyChildChange(slotId: String, childId: String, childName: String) {
        mutate { state ->
            state.copy(
                slots = state.slots.map { if (it.id == slotId) it.copy(childId = childId, childName = childName) else it },
                dialog = null
            )
        }
    }

    private fun updateReview(transform: (List<ShiftVideoRow>) -> List<ShiftVideoRow>) {
        mutableState.update { state ->
            val review = state.videoReview ?: return@update state
            state.copy(videoReview = review.copy(rows = transform(review.rows)))
        }
    }

    private fun recompute(state: ShiftUiState): ShiftUiState {
        val start = parseTimeText(state.startTime)
        val end = parseTimeText(state.endTime)
        val breakMinutes = state.breakMinutes.toIntOrNull()
        val durations = state.slots.map { it.durationText.toIntOrNull()?.takeIf { it > 0 } }
        if (start == null || end == null || breakMinutes == null || breakMinutes < 0 || durations.any { it == null }) {
            return state.copy(slots = state.slots.map { it.copy(startTime = null) },
                freeMinutes = 0, overflows = false, geometryValid = false)
        }
        val geometry = ShiftTimeCalculator.compute(start, end, breakMinutes, durations.map { it!! })
        if (geometry.starts.size != state.slots.size) {
            return state.copy(slots = state.slots.map { it.copy(startTime = null) },
                freeMinutes = 0, overflows = true, geometryValid = false)
        }
        return state.copy(
            slots = state.slots.mapIndexed { index, slot -> slot.copy(startTime = geometry.starts[index]) },
            freeMinutes = geometry.freeMinutes, overflows = geometry.overflows, geometryValid = true
        )
    }

    private fun childName(children: List<Child>, childId: String): String =
        children.find { it.id == childId }?.name.orEmpty()

    private fun tagsOf(state: TagLoadState) = when (state) {
        is TagLoadState.Ready -> state.tags
        else -> emptyList()
    }

    private fun createLessonsErrorText(reason: CreateShiftLessonsResult.Reason): String = when (reason) {
        CreateShiftLessonsResult.Reason.NotReady -> "Дождитесь загрузки смен и повторите."
        CreateShiftLessonsResult.Reason.NotFound -> "Смена не найдена. Обновите данные."
        CreateShiftLessonsResult.Reason.Conflict ->
            "Создание остановлено: у ребёнка уже есть занятие в это время — групповое, другой длительности или другой смены."
        CreateShiftLessonsResult.Reason.StorageUnavailable -> "Не удалось создать занятия. Повторите попытку."
    }
}