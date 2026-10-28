package com.speech_studio.data.repository

import android.content.Context
import com.speech_studio.data.local.ChildrenDatabase
import com.speech_studio.data.local.createChildrenDatabase
import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.model.LessonParticipant
import com.speech_studio.domain.model.LessonType
import com.speech_studio.domain.model.ShiftSlot
import com.speech_studio.domain.model.ShiftTemplate
import com.speech_studio.domain.model.WorkShift
import com.speech_studio.domain.repository.CreateShiftLessonsResult
import com.speech_studio.domain.repository.LessonWriteResult
import com.speech_studio.domain.repository.ShiftGenerationResult
import com.speech_studio.domain.repository.ShiftLoadState
import com.speech_studio.domain.repository.ShiftVideoAssignment
import com.speech_studio.domain.repository.ShiftWriteResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomShiftRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: ChildrenDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: RoomShiftRepository

    private val date = LocalDate(2026, 9, 1)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(NAME)
        db = createChildrenDatabase(context, NAME)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repo = RoomShiftRepository(db.shiftDao(), scope)
        runBlocking {
            withTimeout(5_000) {
                while (repo.state.value !is ShiftLoadState.Ready) delay(50)
            }
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
        context.deleteDatabase(NAME)
    }

    private fun slot(id: String, childId: String, duration: Int = 30) =
        ShiftSlot(id, 0, LocalTime(0, 0), duration, childId)

    private suspend fun prepareShift(vararg childIds: String) {
        childIds.forEach { assertTrue(db.childDao().addOnce(it, "Ребёнок $it")) }
        assertEquals(ShiftWriteResult.Success, repo.createShift(WorkShift("s1", date)))
        assertEquals(
            ShiftWriteResult.Success,
            repo.saveShift(WorkShift("s1", date, slots = childIds.mapIndexed { i, c -> slot("sl$i", c) }))
        )
    }

    private suspend fun awaitShift(id: String, slotCount: Int): ShiftLoadState.Ready {
        var ready: ShiftLoadState.Ready? = null
        withTimeout(5_000) {
            while (ready == null) {
                val current = repo.state.value
                if (current is ShiftLoadState.Ready && current.shifts.any { it.id == id && it.slots.size == slotCount }) {
                    ready = current
                } else {
                    delay(50)
                }
            }
        }
        assertNotNull(ready)
        return ready!!
    }

    @Test
    fun stateExposesSavedShiftAfterWrite() = runBlocking {
        prepareShift("c1", "c2")

        val ready = awaitShift("s1", 2)

        assertEquals(listOf("c1", "c2"), ready.shifts.single().slots.map { it.childId })
    }

    @Test
    fun saveShiftWithUnknownChildIsRejected() = runBlocking {
        assertEquals(ShiftWriteResult.Success, repo.createShift(WorkShift("s1", date)))

        val result = repo.saveShift(WorkShift("s1", date, slots = listOf(slot("sl1", "ghost"))))

        assertEquals(ShiftWriteResult.Failure(ShiftWriteResult.Reason.UnknownChild), result)
    }

    @Test
    fun createLessonsMapsConflictToFailure() = runBlocking {
        prepareShift("c1", "c2")
        check(db.lessonDao().addOnce(
            Lesson(
                "group", listOf(LessonParticipant("c1"), LessonParticipant("c2")),
                LocalDateTime(2026, 9, 1, 8, 0), 30, type = LessonType.GROUP
            )
        ) is LessonWriteResult.Success)

        assertEquals(
            CreateShiftLessonsResult.Failure(CreateShiftLessonsResult.Reason.Conflict),
            repo.createLessons("s1")
        )

        assertTrue(db.lessonDao().observeLessons().first().none { it.lesson.shiftId == "s1" })
    }

    @Test
    fun attachVideosLinksConfirmedAssignments() = runBlocking {
        prepareShift("c1")
        assertEquals(CreateShiftLessonsResult.Success(1, 0), repo.createLessons("s1"))
        val lessonId = db.shiftDao().findSlots("s1").single().lessonId!!

        assertEquals(
            ShiftWriteResult.Success,
            repo.attachVideos("s1", listOf(ShiftVideoAssignment("content://v1", lessonId)))
        )

        assertEquals(listOf("content://v1"), db.shiftDao().videoUris(lessonId))
    }

    @Test
    fun generateShiftsCreatesEmptyShifts() = runBlocking {
        assertEquals(
            ShiftWriteResult.Success,
            repo.saveTemplate(ShiftTemplate(id = "t1", daysOfWeek = listOf(1), endDate = LocalDate(2026, 9, 14)))
        )

        assertEquals(ShiftGenerationResult.Success(2, 0), repo.generateShifts("t1", LocalDate(2026, 9, 1)))

        assertEquals(2, db.shiftDao().findShiftDates("t1").size)
        assertTrue(db.lessonDao().observeLessons().first().isEmpty())
    }

    private companion object {
        const val NAME = "test-shift-repository.db"
    }
}