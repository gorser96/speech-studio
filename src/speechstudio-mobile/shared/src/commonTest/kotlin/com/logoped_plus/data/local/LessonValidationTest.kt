package com.logoped_plus.data.local

import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.repository.LessonWriteResult
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LessonValidationTest {
    private val at = LocalDateTime(2026, 9, 1, 9, 0)

    @Test
    fun lessonWithOnlyRemovedParticipantsIsValid() {
        val lesson = Lesson("id", listOf(LessonParticipant("gone-1", "Иван"), LessonParticipant("gone-2", "Мария")), at, 40)
        assertNull(invalidReason(lesson))
    }

    @Test
    fun lessonWithoutParticipantsIsInvalid() {
        val lesson = Lesson("id", emptyList(), at, 40)
        assertEquals(LessonWriteResult.Reason.InvalidData, invalidReason(lesson))
    }

    @Test
    fun blankOrDuplicatedParticipantIdsAreInvalid() {
        val blank = Lesson("id", listOf(LessonParticipant(" "), LessonParticipant("a")), at, 40)
        assertEquals(LessonWriteResult.Reason.InvalidData, invalidReason(blank))
        val duplicated = Lesson("id", listOf(LessonParticipant("a"), LessonParticipant("a")), at, 40)
        assertEquals(LessonWriteResult.Reason.InvalidData, invalidReason(duplicated))
    }

    @Test
    fun groupedLessonCannotBelongToShift() {
        val grouped = Lesson(
            "id", listOf(LessonParticipant("a"), LessonParticipant("b")), at, 40,
            type = LessonType.GROUP, shiftId = "s1"
        )
        assertEquals(LessonWriteResult.Reason.InvalidData, invalidReason(grouped))
    }

    @Test
    fun individualLessonWithShiftIsValid() {
        val individual = Lesson(
            "id", listOf(LessonParticipant("a")), at, 40,
            type = LessonType.INDIVIDUAL, shiftId = "s1"
        )
        assertNull(invalidReason(individual))
    }
}
