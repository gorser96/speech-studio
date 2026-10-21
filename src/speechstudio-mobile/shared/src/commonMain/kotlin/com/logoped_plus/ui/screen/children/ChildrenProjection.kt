package com.logoped_plus.ui.screen.children

import com.logoped_plus.domain.model.Child
import com.logoped_plus.domain.model.ChildSchedule
import com.logoped_plus.domain.model.WeeklyLessonSlot
import com.logoped_plus.domain.text.RussianNameSort
import com.logoped_plus.ui.components.parseTimeText

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
