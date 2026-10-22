package com.logoped_plus.data.local

import android.content.Context
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.model.ShiftSlot
import com.logoped_plus.domain.model.ShiftTemplate
import com.logoped_plus.domain.model.VideoAttachment
import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.domain.repository.CreateShiftLessonsResult
import com.logoped_plus.domain.repository.LessonWriteResult
import com.logoped_plus.domain.repository.ShiftGenerationResult
import com.logoped_plus.domain.repository.ShiftWriteResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalShiftDatabaseTest {
    private lateinit var context: Context
    private lateinit var db: ChildrenDatabase
    private lateinit var dao: ShiftDao

    private val date = LocalDate(2026, 9, 1)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(NAME)
        db = createChildrenDatabase(context, NAME)
        dao = db.shiftDao()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(NAME)
    }

    private fun slot(id: String, childId: String, duration: Int = 30, lessonId: String? = null) =
        ShiftSlot(id, 0, LocalTime(0, 0), duration, childId, lessonId)

    private suspend fun children(vararg ids: String) {
        ids.forEach { assertTrue(db.childDao().addOnce(it, "Ребёнок $it")) }
    }

    private suspend fun createShift(id: String) {
        assertEquals(ShiftWriteResult.Success, dao.createShiftOnce(WorkShift(id, date)))
    }

    private suspend fun saveSlots(shiftId: String, vararg slots: ShiftSlot) {
        assertEquals(ShiftWriteResult.Success, dao.saveShiftOnce(WorkShift(shiftId, date, slots = slots.toList())))
    }

    private suspend fun submitExisting(shiftId: String, slots: List<ShiftSlotEntity>) {
        val submitted = slots.map { ShiftSlot(it.id, 0, LocalTime(0, 0), it.durationMinutes, it.childId, it.lessonId) }
        assertEquals(ShiftWriteResult.Success, dao.saveShiftOnce(WorkShift(shiftId, date, slots = submitted)))
    }

    private fun count(query: String): Int = db.openHelper.readableDatabase.query(query).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }

    private suspend fun rejectCreateLessons(shiftId: String) {
        var failed = false
        try {
            dao.createLessonsOnce(shiftId)
        } catch (_: Exception) {
            failed = true
        }
        assertTrue("Ожидался откат транзакции", failed)
    }

    @Test
    fun createsIndividualLessonPerFilledSlotAndLinksIt() = runBlocking {
        children("c1", "c2", "c3")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"), slot("sl3", "c3", 40))

        assertEquals(CreateShiftLessonsResult.Success(3, 0), dao.createLessonsOnce("s1"))

        val lessons = db.lessonDao().observeLessons().first()
        assertEquals(3, lessons.size)
        val byStart = lessons.associateBy { it.lesson.scheduledNanoOfDay }
        val first = byStart.getValue(LocalTime(8, 0).toNanosecondOfDay())
        assertEquals(30, first.lesson.durationMinutes)
        assertEquals("c1", first.participants.single().childId)
        assertEquals(LessonType.INDIVIDUAL, first.toDomain().type)
        assertEquals("s1", first.lesson.shiftId)
        assertEquals(40, byStart.getValue(LocalTime(9, 10).toNanosecondOfDay()).lesson.durationMinutes)

        val slots = dao.findSlots("s1")
        assertEquals(3, slots.count { it.lessonId != null })
        assertEquals(3, slots.mapNotNull { it.lessonId }.toSet().size)
    }

    @Test
    fun repeatedCreationDoesNotDuplicateLessons() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        assertEquals(CreateShiftLessonsResult.Success(2, 0), dao.createLessonsOnce("s1"))

        assertEquals(CreateShiftLessonsResult.Success(0, 0), dao.createLessonsOnce("s1"))
        assertEquals(2, db.lessonDao().observeLessons().first().size)
    }

    @Test
    fun existingIndividualLessonWithSameStartAndDurationIsLinked() = runBlocking {
        children("c1")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"))
        check(db.lessonDao().addOnce(
            Lesson("l1", listOf(LessonParticipant("c1")), LocalDateTime(2026, 9, 1, 8, 0), 30, type = LessonType.INDIVIDUAL)
        ) is LessonWriteResult.Success)

        assertEquals(CreateShiftLessonsResult.Success(0, 1), dao.createLessonsOnce("s1"))

        assertEquals("s1", db.lessonDao().find("l1")!!.lesson.shiftId)
        assertEquals("l1", dao.findSlots("s1").single().lessonId)
        assertEquals(1, db.lessonDao().observeLessons().first().size)
    }

    @Test
    fun groupLessonConflictRollsBackWithoutPartialChanges() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        check(db.lessonDao().addOnce(
            Lesson(
                "group", listOf(LessonParticipant("c1"), LessonParticipant("c2")),
                LocalDateTime(2026, 9, 1, 8, 0), 30, type = LessonType.GROUP
            )
        ) is LessonWriteResult.Success)

        rejectCreateLessons("s1")

        assertEquals(1, db.lessonDao().observeLessons().first().size)
        assertNull(db.lessonDao().find("group")!!.lesson.shiftId)
        assertTrue(dao.findSlots("s1").all { it.lessonId == null })
    }

    @Test
    fun lessonOfAnotherShiftConflicts() = runBlocking {
        children("c1")
        createShift("s0")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"))
        check(db.lessonDao().addOnce(
            Lesson(
                "l1", listOf(LessonParticipant("c1")), LocalDateTime(2026, 9, 1, 8, 0), 30,
                type = LessonType.INDIVIDUAL, shiftId = "s0"
            )
        ) is LessonWriteResult.Success)

        rejectCreateLessons("s1")

        assertEquals("s0", db.lessonDao().find("l1")!!.lesson.shiftId)
        assertNull(dao.findSlots("s1").single().lessonId)
    }

    @Test
    fun reorderMovesLinkedLessonsAndKeepsCommentAndVideo() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        dao.createLessonsOnce("s1")

        val existing = dao.findSlots("s1").sortedBy { it.position }
        val first = existing.first { it.childId == "c1" }
        val lesson = db.lessonDao().find(first.lessonId!!)!!.toDomain()
        check(db.lessonDao().updateExisting(
            lesson.copy(comment = "особый", videoAttachments = listOf(VideoAttachment("content://v1")))
        ) is LessonWriteResult.Success)

        submitExisting("s1", listOf(existing[1], existing[0]))

        val reordered = dao.findSlots("s1").sortedBy { it.position }
        assertEquals(listOf("c2", "c1"), reordered.map { it.childId })
        assertEquals(LocalTime(8, 0).toNanosecondOfDay(), reordered[0].startNanoOfDay)
        val moved = db.lessonDao().find(first.lessonId!!)!!.toDomain()
        assertEquals(LocalDateTime(2026, 9, 1, 8, 35), moved.scheduledAt)
        assertEquals("особый", moved.comment)
        assertEquals("content://v1", moved.videoAttachments.single().uri)
        assertEquals(listOf("c1"), moved.participants.map { it.childId })
    }

    @Test
    fun durationChangeUpdatesLinkedLessonAndFollowingStart() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        dao.createLessonsOnce("s1")
        val existing = dao.findSlots("s1").sortedBy { it.position }

        submitExisting("s1", existing.map { if (it.childId == "c1") it.copy(durationMinutes = 40) else it })

        val firstLesson = existing.first { it.childId == "c1" }.lessonId!!
        val secondLesson = existing.first { it.childId == "c2" }.lessonId!!
        assertEquals(40, db.lessonDao().find(firstLesson)!!.lesson.durationMinutes)
        assertEquals(LocalTime(8, 45).toNanosecondOfDay(), db.lessonDao().find(secondLesson)!!.lesson.scheduledNanoOfDay)
    }

    @Test
    fun dateChangeMovesShiftAndLinkedLessonsKeepingVideo() = runBlocking {
        children("c1")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"))
        dao.createLessonsOnce("s1")
        val existing = dao.findSlots("s1").single()
        val lesson = db.lessonDao().find(existing.lessonId!!)!!.toDomain()
        check(db.lessonDao().updateExisting(
            lesson.copy(comment = "особый", videoAttachments = listOf(VideoAttachment("content://v1")))
        ) is LessonWriteResult.Success)

        val newDate = LocalDate(2026, 9, 2)
        val moved = WorkShift(
            id = "s1", date = newDate,
            slots = listOf(
                ShiftSlot(existing.id, 0, LocalTime(8, 0), existing.durationMinutes, existing.childId, existing.lessonId)
            )
        )
        assertEquals(ShiftWriteResult.Success, dao.saveShiftOnce(moved))

        assertEquals(newDate, dao.findShift("s1")!!.toDomain().date)
        val updated = db.lessonDao().find(existing.lessonId!!)!!.toDomain()
        assertEquals(LocalDateTime(2026, 9, 2, 8, 0), updated.scheduledAt)
        assertEquals("особый", updated.comment)
        assertEquals("content://v1", updated.videoAttachments.single().uri)
        assertEquals("s1", updated.shiftId)
    }

    @Test
    fun changingSlotChildUpdatesParticipantAndKeepsCommentAndVideo() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"))
        dao.createLessonsOnce("s1")
        val existing = dao.findSlots("s1").single()
        val lesson = db.lessonDao().find(existing.lessonId!!)!!.toDomain()
        check(db.lessonDao().updateExisting(
            lesson.copy(comment = "особый", videoAttachments = listOf(VideoAttachment("content://v1")))
        ) is LessonWriteResult.Success)

        submitExisting("s1", listOf(existing.copy(childId = "c2")))

        val updated = db.lessonDao().find(existing.lessonId!!)!!.toDomain()
        assertEquals(listOf("c2"), updated.participants.map { it.childId })
        assertEquals("особый", updated.comment)
        assertEquals("content://v1", updated.videoAttachments.single().uri)
        assertEquals("s1", updated.shiftId)
    }

    @Test
    fun removingSlotKeepsLessonWithoutShiftAndKeepsVideo() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        dao.createLessonsOnce("s1")
        val existing = dao.findSlots("s1").sortedBy { it.position }
        val removedLessonId = existing.first { it.childId == "c2" }.lessonId!!
        val lesson = db.lessonDao().find(removedLessonId)!!.toDomain()
        check(db.lessonDao().updateExisting(
            lesson.copy(videoAttachments = listOf(VideoAttachment("content://keep")))
        ) is LessonWriteResult.Success)

        submitExisting("s1", listOf(existing.first { it.childId == "c1" }))

        assertEquals(1, dao.findSlots("s1").size)
        val orphan = db.lessonDao().find(removedLessonId)!!.toDomain()
        assertNull(orphan.shiftId)
        assertEquals("content://keep", orphan.videoAttachments.single().uri)
    }

    @Test
    fun deletingShiftRemovesSlotsAndKeepsLessons() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        dao.createLessonsOnce("s1")

        assertEquals(ShiftWriteResult.Success, dao.deleteShiftOnce("s1"))

        assertNull(dao.findShiftRow("s1"))
        assertTrue(dao.findSlots("s1").isEmpty())
        val lessons = db.lessonDao().observeLessons().first()
        assertEquals(2, lessons.size)
        assertTrue(lessons.all { it.lesson.shiftId == null })
    }

    @Test
    fun deletingShiftLessonsRemovesRowsAndFreesSlots() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        dao.createLessonsOnce("s1")

        assertEquals(ShiftWriteResult.Success, dao.deleteShiftLessonsOnce("s1"))

        assertTrue(db.lessonDao().observeLessons().first().isEmpty())
        assertNotNull(dao.findShiftRow("s1"))
        assertEquals(2, dao.findSlots("s1").size)
        assertTrue(dao.findSlots("s1").all { it.lessonId == null })
    }

    @Test
    fun attachingVideosAddsOnlyNewLinks() = runBlocking {
        children("c1", "c2")
        createShift("s1")
        saveSlots("s1", slot("sl1", "c1"), slot("sl2", "c2"))
        dao.createLessonsOnce("s1")
        val lessonId = dao.findSlots("s1").first { it.childId == "c1" }.lessonId!!

        assertEquals(
            ShiftWriteResult.Success,
            dao.attachVideosOnce("s1", listOf("content://a" to lessonId, "content://b" to lessonId, "content://a" to lessonId))
        )
        assertEquals(2, count("SELECT COUNT(*) FROM lesson_videos WHERE lessonId = '$lessonId'"))

        assertEquals(
            ShiftWriteResult.Success,
            dao.attachVideosOnce("s1", listOf("content://a" to lessonId, "content://c" to lessonId))
        )
        assertEquals(3, count("SELECT COUNT(*) FROM lesson_videos WHERE lessonId = '$lessonId'"))
    }

    @Test
    fun generationCreatesEmptyShiftsAndSkipsExisting() = runBlocking {
        val template = ShiftTemplate(
            id = "t1", name = "Утро", daysOfWeek = listOf(1), endDate = LocalDate(2026, 9, 14)
        )
        assertEquals(ShiftWriteResult.Success, dao.saveTemplateOnce(template))

        assertEquals(
            ShiftGenerationResult.Success(2, 0),
            dao.generateShiftsOnce("t1", LocalDate(2026, 9, 1))
        )
        assertEquals(2, dao.findShiftDates("t1").size)
        assertTrue(db.lessonDao().observeLessons().first().isEmpty())

        assertEquals(
            ShiftGenerationResult.Success(0, 2),
            dao.generateShiftsOnce("t1", LocalDate(2026, 9, 1))
        )
    }

    @Test
    fun deletingTemplateKeepsGeneratedShifts() = runBlocking {
        val template = ShiftTemplate(
            id = "t1", name = "Утро", daysOfWeek = listOf(1), endDate = LocalDate(2026, 9, 14)
        )
        dao.saveTemplateOnce(template)
        dao.generateShiftsOnce("t1", LocalDate(2026, 9, 1))

        assertEquals(ShiftWriteResult.Success, dao.deleteTemplateOnce("t1"))

        assertEquals(2, count("SELECT COUNT(*) FROM work_shifts"))
        assertEquals(2, count("SELECT COUNT(*) FROM work_shifts WHERE templateId IS NULL"))
    }

    private companion object {
        const val NAME = "itest-shift.db"
    }
}