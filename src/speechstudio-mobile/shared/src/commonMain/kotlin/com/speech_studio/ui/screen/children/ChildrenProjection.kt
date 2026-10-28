package com.speech_studio.ui.screen.children

import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.ChildSchedule
import com.speech_studio.domain.model.WeeklyLessonSlot
import com.speech_studio.domain.text.RussianNameSort
import com.speech_studio.ui.components.parseTimeText

fun parseSlot(slot: SlotDraft): WeeklyLessonSlot? {
    val start = parseTimeText(slot.startTimeText) ?: return null
    val minutes = slot.durationText.toIntOrNull() ?: return null
    if (minutes <= 0) return null
    return WeeklyLessonSlot(slot.id, slot.dayOfWeek, start, minutes)
}

fun draftSchedule(draft: ChildCardDraft): ChildSchedule? {
    val slots = ArrayList<WeeklyLessonSlot>(draft.slots.size)
    for (slot in draft.slots) {
        val parsed = parseSlot(slot) ?: return null
        slots.add(parsed)
    }
    return ChildSchedule(slots, draft.endDate)
}

fun projectChildren(children: List<Child>, query: String): List<Child> {
    val effective = query.trim()
    val filtered = if (effective.isEmpty()) children
        else children.filter { it.name.contains(effective, ignoreCase = true) }
    return RussianNameSort.sortedBy(filtered) { it.name }
}
