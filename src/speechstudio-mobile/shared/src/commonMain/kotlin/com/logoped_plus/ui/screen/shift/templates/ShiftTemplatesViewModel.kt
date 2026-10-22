@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.shift.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logoped_plus.domain.model.ShiftTemplate
import com.logoped_plus.domain.repository.ShiftGenerationResult
import com.logoped_plus.domain.repository.ShiftRepository
import com.logoped_plus.domain.repository.ShiftWriteResult
import com.logoped_plus.platform.randomUuidString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Владелец раздела «Шаблоны смен»: список шаблонов, редактор, удаление и
 * генерация пустых смен. Состав детей в шаблоне не хранится.
 */
class ShiftTemplatesViewModel(
    private val shiftRepository: ShiftRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {

    private val mutableState = MutableStateFlow(ShiftTemplatesUiState(today = today()))
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            shiftRepository.observeTemplates().collect { templates ->
                mutableState.update { it.copy(templates = templates, loading = false) }
            }
        }
    }

    fun newTemplate() {
        if (mutableState.value.busy) return
        mutableState.update {
            it.copy(draft = ShiftTemplateDraft(id = randomUuidString()), draftError = null, message = null)
        }
    }

    fun editTemplate(id: String) {
        if (mutableState.value.busy) return
        val template = mutableState.value.templates.find { it.id == id } ?: return
        mutableState.update { it.copy(draft = template.toDraft(), draftError = null, message = null) }
    }

    fun dismissEditor() {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(draft = null, draftError = null) }
    }

    fun changeName(value: String) = mutateDraft { it.copy(name = value) }

    fun changeStart(value: String) = mutateDraft { it.copy(startTime = value) }

    fun changeEnd(value: String) = mutateDraft { it.copy(endTime = value) }

    fun changeBase(value: String) = mutateDraft { it.copy(baseDuration = value) }

    fun changeBreak(value: String) = mutateDraft { it.copy(breakMinutes = value) }

    fun toggleDay(day: Int) = mutateDraft { draft ->
        draft.copy(daysOfWeek = if (day in draft.daysOfWeek) draft.daysOfWeek - day else draft.daysOfWeek + day)
    }

    fun setEndDate(date: LocalDate?) = mutateDraft { it.copy(endDate = date) }

    /** Сохраняет шаблон, не создавая смен. */
    fun save() = persist(closeOnSuccess = true) { _ ->
        mutableState.update { it.copy(message = "Шаблон сохранён.") }
    }

    /** Сохраняет шаблон и создаёт пустые смены с текущей даты до даты окончания. */
    fun generate() = persist(closeOnSuccess = true) { template ->
        when (val result = shiftRepository.generateShifts(template.id, today())) {
            is ShiftGenerationResult.Success -> mutableState.update {
                it.copy(message = "Создано смен: ${result.created}, уже было: ${result.existing}.")
            }
            is ShiftGenerationResult.Failure -> mutableState.update {
                it.copy(message = generationErrorText(result.reason))
            }
        }
    }

    fun requestDelete(id: String) {
        if (mutableState.value.busy) return
        val template = mutableState.value.templates.find { it.id == id } ?: return
        mutableState.update { it.copy(deleteTarget = template) }
    }

    fun dismissDelete() {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val template = mutableState.value.deleteTarget ?: return
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = shiftRepository.deleteTemplate(template.id)) {
                ShiftWriteResult.Success -> mutableState.update {
                    it.copy(busy = false, deleteTarget = null, message = "Шаблон удалён.")
                }
                is ShiftWriteResult.Failure -> mutableState.update { it.copy(busy = false, deleteTarget = null) }
            }
        }
    }

    fun clearMessage() {
        mutableState.update { it.copy(message = null) }
    }

    private fun persist(closeOnSuccess: Boolean, afterSave: suspend (ShiftTemplate) -> Unit) {
        val draft = mutableState.value.draft ?: return
        val template = draft.toTemplate()
        if (template == null) {
            mutableState.update { it.copy(draftError = ShiftWriteResult.Reason.InvalidData) }
            return
        }
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, draftError = null) }
        viewModelScope.launch {
            when (val result = shiftRepository.saveTemplate(template)) {
                ShiftWriteResult.Success -> {
                    mutableState.update {
                        it.copy(busy = false, draft = if (closeOnSuccess) null else it.draft)
                    }
                    afterSave(template)
                }
                is ShiftWriteResult.Failure -> mutableState.update {
                    it.copy(busy = false, draftError = result.reason)
                }
            }
        }
    }

    private fun mutateDraft(transform: (ShiftTemplateDraft) -> ShiftTemplateDraft) {
        mutableState.update { state ->
            val draft = state.draft ?: return@update state
            if (state.busy) state else state.copy(draft = transform(draft), draftError = null)
        }
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(timeZone).date

    private fun generationErrorText(reason: ShiftGenerationResult.Reason): String = when (reason) {
        ShiftGenerationResult.Reason.NotReady -> "Дождитесь загрузки данных и повторите."
        ShiftGenerationResult.Reason.InvalidTemplate -> "Заполните дни недели, окно и дату окончания шаблона."
        ShiftGenerationResult.Reason.TemplateNotFound -> "Шаблон не найден. Сохраните его и повторите."
        ShiftGenerationResult.Reason.StorageUnavailable -> "Не удалось создать смены. Повторите попытку."
    }
}
