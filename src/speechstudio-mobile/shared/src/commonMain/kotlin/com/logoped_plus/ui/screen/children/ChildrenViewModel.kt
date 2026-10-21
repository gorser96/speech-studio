@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logoped_plus.data.preferences.SettingsRepository
import com.logoped_plus.domain.model.Child
import com.logoped_plus.domain.model.ChildScheduleValidation
import com.logoped_plus.domain.repository.ChildLoadState
import com.logoped_plus.domain.repository.ChildRepository
import com.logoped_plus.domain.repository.ChildWriteResult
import com.logoped_plus.domain.repository.GenerateChildLessonsResult
import com.logoped_plus.domain.repository.TagLoadState
import com.logoped_plus.domain.repository.TagRepository
import com.logoped_plus.domain.text.RussianText
import com.logoped_plus.domain.usecase.GenerateChildLessonsUseCase
import com.logoped_plus.platform.randomUuidString
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ChildrenViewModel(
    private val childRepository: ChildRepository,
    private val tagRepository: TagRepository,
    private val generateChildLessons: GenerateChildLessonsUseCase,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ChildrenUiState(childRepository.state.value, tagRepository.state.value, settings.visibleTagsLimit.value)
    )
    val uiState = mutableState.asStateFlow()
    private var cardJob: Job? = null

    private fun today(): LocalDate = clock.now().toLocalDateTime(timeZone).date

    init {
        viewModelScope.launch {
            settings.visibleTagsLimit.collect { limit -> mutableState.update { it.copy(visibleTagsLimit = limit) } }
        }
        viewModelScope.launch {
            childRepository.state.collect { load ->
                mutableState.update { state ->
                    val closed = state.deleteTarget != null && load is ChildLoadState.Ready &&
                        load.children.none { it.id == state.deleteTarget?.id }
                    if (closed) {
                        val goneId = state.deleteTarget?.id
                        state.copy(loadState = load,
                            deleteTarget = null, deleteError = null, deleting = false,
                            cardId = if (state.cardId == goneId) null else state.cardId,
                            card = if (state.cardId == goneId) null else state.card,
                            editing = false, draft = null, generating = false, generationError = null,
                            editor = null)
                    } else {
                        state.copy(loadState = load)
                    }
                }
            }
        }
        viewModelScope.launch {
            tagRepository.state.collect { tags ->
                mutableState.update { state ->
                    val next = state.copy(tagsLoadState = tags)
                    if (tags is TagLoadState.Ready) {
                        val draft = state.draft
                        if (draft != null) {
                            val live = tags.tags.map { it.id }.toSet()
                            val kept = draft.tagIds.filter { it in live }
                            if (kept != draft.tagIds) next.copy(draft = draft.copy(tagIds = kept)) else next
                        } else next
                    } else next
                }
            }
        }
    }

    override fun onCleared() {
        cardJob?.cancel()
        super.onCleared()
    }

    fun setQuery(query: String) {
        mutableState.update { it.copy(query = query) }
    }

    fun openCard(id: String) {
        if (childRepository.state.value !is ChildLoadState.Ready || uiState.value.busy) return
        mutableState.update { it.copy(cardId = id, card = null, editing = false, draft = null,
            generating = false, generationError = null, generationCounts = null) }
        observeCard(id)
    }

    fun closeCard() {
        if (uiState.value.busy) return
        cardJob?.cancel()
        cardJob = null
        mutableState.update { it.copy(cardId = null, card = null, editing = false, draft = null,
            generating = false, generationError = null, generationCounts = null) }
    }

    private fun observeCard(id: String) {
        cardJob?.cancel()
        cardJob = viewModelScope.launch {
            childRepository.observeCard(id).collect { card ->
                mutableState.update { state ->
                    if (state.cardId != id) return@update state
                    if (card == null) state.copy(card = null, editing = false, draft = null,
                        generating = false, generationError = null, generationCounts = null)
                    else state.copy(card = card)
                }
            }
        }
    }

    fun startEditing() {
        val state = uiState.value
        if (state.card == null || state.busy) return
        val card = state.card
        mutableState.update {
            it.copy(editing = true, draft = ChildCardDraft(
                childId = card.id,
                name = card.name,
                tagIds = card.tagIds.toList(),
                slots = card.schedule.slots.map {
                    SlotDraft(it.id, it.dayOfWeek, RussianText.formatTime(it.startTime), it.durationMinutes.toString())
                },
                endDate = card.schedule.endDate
            ))
        }
    }

    fun cancelEditing() {
        if (uiState.value.draft?.saving == true) return
        mutableState.update { it.copy(editing = false, draft = null) }
    }

    fun changeDraftName(name: String) {
        mutableState.update { state ->
            val draft = state.draft
            if (draft == null || draft.saving) state else state.copy(draft = draft.copy(name = name, error = null))
        }
    }

    fun toggleTag(tagId: String) {
        mutableState.update { state ->
            val draft = state.draft
            if (draft == null || draft.saving) state
            else state.copy(draft = draft.copy(
                tagIds = if (tagId in draft.tagIds) draft.tagIds - tagId else draft.tagIds + tagId, error = null))
        }
    }

    fun addSlot() {
        mutableState.update { state ->
            val draft = state.draft
            if (draft == null || draft.saving) state
            else state.copy(draft = draft.copy(slots = draft.slots + SlotDraft(randomUuidString(), today().dayOfWeek.isoDayNumber), error = null))
        }
    }

    fun updateSlot(slotId: String, dayOfWeek: Int? = null, startTimeText: String? = null, durationText: String? = null) {
        mutableState.update { state ->
            val draft = state.draft
            if (draft == null || draft.saving) state
            else state.copy(draft = draft.copy(slots = draft.slots.map { slot ->
                if (slot.id != slotId) slot
                else slot.copy(
                    dayOfWeek = dayOfWeek ?: slot.dayOfWeek,
                    startTimeText = startTimeText ?: slot.startTimeText,
                    durationText = durationText ?: slot.durationText
                )
            }, error = null))
        }
    }

    fun removeSlot(slotId: String) {
        mutableState.update { state ->
            val draft = state.draft
            if (draft == null || draft.saving) state
            else state.copy(draft = draft.copy(slots = draft.slots.filter { it.id != slotId }, error = null))
        }
    }

    fun setEndDate(date: LocalDate?) {
        mutableState.update { state ->
            val draft = state.draft
            if (draft == null || draft.saving) state else state.copy(draft = draft.copy(endDate = date, error = null))
        }
    }

    fun draftIsValid(draft: ChildCardDraft): Boolean =
        draft.name.isNotBlank() && tagRepository.state.value is TagLoadState.Ready &&
            draftSchedule(draft)?.let { ChildScheduleValidation.validate(it) == null } == true

    fun saveCard() {
        val state = uiState.value
        val draft = state.draft
        if (draft == null || draft.saving || state.card == null || !draftIsValid(draft)) return
        val schedule = draftSchedule(draft)
        if (schedule == null) return
        mutableState.update { it.copy(draft = draft.copy(saving = true, error = null)) }
        viewModelScope.launch {
            val result = childRepository.saveCard(Child(draft.childId, draft.name, draft.tagIds, schedule))
            mutableState.update { current ->
                when (result) {
                    ChildWriteResult.Success -> current.copy(editing = false, draft = null)
                    is ChildWriteResult.Failure -> current.copy(draft = current.draft?.copy(saving = false, error = result.reason))
                }
            }
        }
    }

    fun canGenerate(card: Child): Boolean {
        val state = uiState.value
        if (state.generating || state.loadState !is ChildLoadState.Ready) return false
        val schedule = card.schedule
        if (schedule.isEmpty || schedule.endDate == null) return false
        if (schedule.endDate < today()) return false
        return ChildScheduleValidation.validate(schedule) == null
    }

    fun generateLessons() {
        val state = uiState.value
        val card = state.card ?: return
        if (!canGenerate(card)) return
        mutableState.update { it.copy(generating = true, generationError = null, generationCounts = null) }
        viewModelScope.launch {
            when (val result = generateChildLessons(card.id)) {
                is GenerateChildLessonsResult.Success ->
                    mutableState.update { it.copy(generating = false, generationCounts = result.counts) }
                is GenerateChildLessonsResult.Failure ->
                    mutableState.update { it.copy(generating = false, generationError = result.reason) }
            }
        }
    }

    fun openNew() {
        if (childRepository.state.value !is ChildLoadState.Ready || uiState.value.busy) return
        mutableState.update { it.copy(editor = ChildEditor(randomUuidString())) }
    }

    fun changeName(name: String) {
        mutableState.update { state ->
            val editor = state.editor
            if (editor == null || editor.saving) state
            else state.copy(editor = editor.copy(name = name, error = null))
        }
    }

    fun cancel() {
        if (uiState.value.editor?.saving == true) return
        mutableState.update { it.copy(editor = null) }
    }

    fun retryLoading() {
        if (uiState.value.busy) return
        childRepository.retryLoading()
    }

    fun retryTags() {
        if (uiState.value.busy) return
        tagRepository.retryLoading()
    }

    fun save() {
        val editor = uiState.value.editor ?: return
        if (editor.saving || editor.name.isBlank() || childRepository.state.value !is ChildLoadState.Ready) return
        mutableState.update { it.copy(editor = editor.copy(saving = true, error = null)) }
        viewModelScope.launch {
            val child = Child(editor.id, editor.name.trim())
            val result = childRepository.addChild(child)
            mutableState.update { state ->
                when (result) {
                    ChildWriteResult.Success -> state.copy(editor = null)
                    is ChildWriteResult.Failure -> state.copy(editor = editor.copy(error = result.reason))
                }
            }
        }
    }

    fun openDeleteConfirm(child: Child) {
        if (childRepository.state.value !is ChildLoadState.Ready || uiState.value.busy) return
        mutableState.update { it.copy(deleteTarget = child, deleteError = null) }
    }

    fun cancelDelete() {
        if (uiState.value.deleting) return
        mutableState.update { it.copy(deleteTarget = null, deleteError = null) }
    }

    fun delete() {
        val child = uiState.value.deleteTarget ?: return
        if (uiState.value.deleting || childRepository.state.value !is ChildLoadState.Ready) return
        mutableState.update { it.copy(deleting = true, deleteError = null) }
        viewModelScope.launch {
            val result = childRepository.deleteChild(child)
            mutableState.update { state ->
                when (result) {
                    ChildWriteResult.Success -> state.copy(deleting = false)
                    is ChildWriteResult.Failure -> state.copy(deleting = false, deleteError = result.reason)
                }
            }
        }
    }
}
