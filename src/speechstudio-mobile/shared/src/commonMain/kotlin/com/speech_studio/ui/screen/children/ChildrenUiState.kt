package com.speech_studio.ui.screen.children

import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.GenerationCounts
import com.speech_studio.domain.repository.ChildLoadState
import com.speech_studio.domain.repository.ChildWriteResult
import com.speech_studio.domain.repository.GenerateChildLessonsResult
import com.speech_studio.domain.repository.TagLoadState
import kotlinx.datetime.LocalDate

data class ChildEditor(
    val id: String,
    val name: String = "",
    val isNew: Boolean = true,
    val saving: Boolean = false,
    val error: ChildWriteResult.Reason? = null
)

data class SlotDraft(
    val id: String,
    val dayOfWeek: Int,
    val startTimeText: String = "09:00",
    val durationText: String = "40"
)

data class ChildCardDraft(
    val childId: String,
    val name: String,
    val tagIds: List<String> = emptyList(),
    val slots: List<SlotDraft> = emptyList(),
    val endDate: LocalDate? = null,
    val saving: Boolean = false,
    val error: ChildWriteResult.Reason? = null
)

data class ChildrenUiState(
    val loadState: ChildLoadState = ChildLoadState.Loading(),
    val tagsLoadState: TagLoadState = TagLoadState.Loading(),
    val visibleTagsLimit: Int = 3,
    val query: String = "",
    val cardId: String? = null,
    val card: Child? = null,
    val editing: Boolean = false,
    val draft: ChildCardDraft? = null,
    val generating: Boolean = false,
    val generationError: GenerateChildLessonsResult.Reason? = null,
    val generationCounts: GenerationCounts? = null,
    val editor: ChildEditor? = null,
    val deleteTarget: Child? = null,
    val deleting: Boolean = false,
    val deleteError: ChildWriteResult.Reason? = null
) {
    val children: List<Child> get() = loadState.children
    val busy: Boolean get() = generating || deleting || draft?.saving == true || editor?.saving == true
}
