package com.speech_studio.ui.screen.schedule

import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.model.LessonParticipant
import com.speech_studio.domain.model.LessonType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LessonTypeRulesTest {

    private val date = LocalDate(2026, 9, 1)

    @Test
    fun individualCreateNeedsExactlyOneParticipant() {
        val none = editor(LessonType.INDIVIDUAL, LessonEditorMode.CREATE, emptyList())
        val one = editor(LessonType.INDIVIDUAL, LessonEditorMode.CREATE, listOf("a"))
        val two = editor(LessonType.INDIVIDUAL, LessonEditorMode.CREATE, listOf("a", "b"))

        assertFalse(none.participantsValid)
        assertTrue(one.participantsValid)
        assertFalse(two.participantsValid)
    }

    @Test
    fun groupCreateNeedsAtLeastTwoParticipants() {
        val one = editor(LessonType.GROUP, LessonEditorMode.CREATE, listOf("a"))
        val two = editor(LessonType.GROUP, LessonEditorMode.CREATE, listOf("a", "b"))

        assertFalse(one.participantsValid)
        assertTrue(two.participantsValid)
    }

    @Test
    fun duplicatedParticipantsAreInvalid() {
        val duplicated = editor(LessonType.GROUP, LessonEditorMode.CREATE, listOf("a", "a"))
        assertFalse(duplicated.participantsValid)
    }

    @Test
    fun groupWithOneParticipantAfterEditingStaysGroupAndValid() {
        val grouped = Lesson(
            "l1", listOf(LessonParticipant("a"), LessonParticipant("b")),
            LocalDateTime(date, LocalTime(9, 0)), 40, type = LessonType.GROUP, shiftId = "s1"
        )
        val editor = LessonEditorState.from(grouped, LessonEditorMode.EDIT).copy(
            participants = listOf(LessonParticipant("a"))
        )

        assertTrue(editor.participantsValid)
        val lesson = assertNotNull(editor.toLesson())
        assertEquals(LessonType.GROUP, lesson.type)
        assertEquals("s1", lesson.shiftId)
    }

    @Test
    fun toLessonKeepsSavedTypeAndShiftForIndividual() {
        val individual = Lesson(
            "l2", listOf(LessonParticipant("a")),
            LocalDateTime(date, LocalTime(10, 0)), 30, type = LessonType.INDIVIDUAL, shiftId = "s2"
        )
        val editor = LessonEditorState.from(individual, LessonEditorMode.EDIT)

        val lesson = assertNotNull(editor.toLesson())
        assertEquals(LessonType.INDIVIDUAL, lesson.type)
        assertEquals("s2", lesson.shiftId)
    }

    @Test
    fun invalidTimeOrDurationProducesNoLesson() {
        val editor = editor(LessonType.INDIVIDUAL, LessonEditorMode.CREATE, listOf("a"))
        assertNull(editor.copy(time = "").toLesson())
        assertNull(editor.copy(duration = "0").toLesson())
    }

    private fun editor(type: LessonType, mode: LessonEditorMode, childIds: List<String>) = LessonEditorState(
        mode = mode,
        epochDay = date.toEpochDays(),
        time = "09:00",
        duration = "40",
        participants = childIds.map { LessonParticipant(it) },
        type = type
    )
}