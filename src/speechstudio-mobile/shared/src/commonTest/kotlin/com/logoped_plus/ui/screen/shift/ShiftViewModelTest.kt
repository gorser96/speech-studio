@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.shift

import com.logoped_plus.domain.model.Child
import com.logoped_plus.domain.model.DeviceVideo
import com.logoped_plus.domain.model.GenerationCounts
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.model.ShiftSlot
import com.logoped_plus.domain.model.ShiftTemplate
import com.logoped_plus.domain.model.Tag
import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.domain.repository.ChildLoadState
import com.logoped_plus.domain.repository.ChildRepository
import com.logoped_plus.domain.repository.ChildWriteResult
import com.logoped_plus.domain.repository.CreateShiftLessonsResult
import com.logoped_plus.domain.repository.GenerateChildLessonsResult
import com.logoped_plus.domain.repository.LessonLoadState
import com.logoped_plus.domain.repository.LessonRepository
import com.logoped_plus.domain.repository.LessonWriteResult
import com.logoped_plus.domain.repository.ShiftGenerationResult
import com.logoped_plus.domain.repository.ShiftLoadState
import com.logoped_plus.domain.repository.ShiftRepository
import com.logoped_plus.domain.repository.ShiftVideoAssignment
import com.logoped_plus.domain.repository.ShiftWriteResult
import com.logoped_plus.domain.repository.TagLoadState
import com.logoped_plus.domain.repository.TagRepository
import com.logoped_plus.domain.repository.TagWriteResult
import com.logoped_plus.domain.repository.VideoLibrary
import com.logoped_plus.domain.usecase.FindShiftVideosUseCase
import com.logoped_plus.ui.screen.schedule.plusDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val date = LocalDate(2026, 5, 4)
    private val zone = TimeZone.UTC

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addedSlotUsesBaseDurationAndChangingBaseKeepsExistingSlots() {
        val viewModel = viewModel()
        viewModel.open("s1")

        viewModel.changeBaseDuration("30")
        viewModel.addChild("c1", "Аня")
        assertEquals("30", viewModel.uiState.value.slots.single().durationText)

        viewModel.changeBaseDuration("40")
        assertEquals("30", viewModel.uiState.value.slots.single().durationText)
        viewModel.addChild("c2", "Максим")
        assertEquals(listOf("30", "40"), viewModel.uiState.value.slots.map { it.durationText })
    }

    @Test
    fun reorderRecomputesStartsFollowingSlotDurations() {
        val viewModel = viewModel()
        viewModel.open("s1")
        viewModel.addChild("c1", "Аня")
        viewModel.addChild("c2", "Максим")
        val second = viewModel.uiState.value.slots[1]
        viewModel.changeSlotDuration(second.id, "40")

        viewModel.moveSlot(0, 1)

        val slots = viewModel.uiState.value.slots
        assertEquals(listOf("Максим", "Аня"), slots.map { it.childName })
        assertEquals(LocalTime(8, 0), slots[0].startTime)
        assertEquals(LocalTime(8, 45), slots[1].startTime)
    }

    @Test
    fun slotsOutsideWindowDisableLessonCreation() {
        val viewModel = viewModel(repository = FakeShiftRepository(listOf(shift(end = LocalTime(8, 30), breakMinutes = 0))))
        viewModel.open("s1")
        viewModel.changeBreakMinutes("0")
        viewModel.addChild("c1", "Аня")
        viewModel.addChild("c2", "Максим")

        assertTrue(viewModel.uiState.value.overflows)
        assertFalse(viewModel.uiState.value.canCreateLessons)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun createLessonsPersistsDirtyDraftBeforeCreating() {
        val repository = FakeShiftRepository(listOf(shift()))
        val viewModel = viewModel(repository = repository)
        viewModel.open("s1")
        viewModel.changeBaseDuration("35")
        viewModel.addChild("c1", "Аня")

        viewModel.createLessons()

        val saved = assertNotNull(repository.saved)
        assertEquals(35, saved.baseDurationMinutes)
        assertEquals(1, saved.slots.size)
        assertEquals(1, repository.createLessonsCalls)
        assertFalse(viewModel.uiState.value.dirty)
    }

    @Test
    fun changingDateKeepsDraftUntilSaveAndPersistsNewDate() {
        val repository = FakeShiftRepository(listOf(shift()))
        val viewModel = viewModel(repository = repository)
        viewModel.open("s1")
        val newDate = date.plusDays(1)

        viewModel.changeDate(newDate)

        assertEquals(newDate, viewModel.uiState.value.date)
        assertTrue(viewModel.uiState.value.dirty)
        assertNull(repository.saved)

        viewModel.save()

        assertEquals(newDate, assertNotNull(repository.saved).date)
        assertFalse(viewModel.uiState.value.dirty)
    }

    @Test
    fun dateChangeAvailableWhenShiftAlreadyHasLessons() {
        val lesson = Lesson(
            "l1", listOf(LessonParticipant("c1")), LocalDateTime(date, LocalTime(8, 0)), 30,
            type = LessonType.INDIVIDUAL, shiftId = "s1"
        )
        val viewModel = viewModel(repository = FakeShiftRepository(listOf(shiftWithLinkedSlot())), lessons = listOf(lesson))
        viewModel.open("s1")

        assertTrue(viewModel.uiState.value.canChangeDate)
    }

    @Test
    fun removingSlotWithLessonNeedsConfirmationAndKeepsLesson() {
        val repository = FakeShiftRepository(listOf(shiftWithLinkedSlot()))
        val viewModel = viewModel(repository = repository)
        viewModel.open("s1")

        viewModel.removeSlot("sl1")
        assertNotNull(viewModel.uiState.value.dialog)
        assertNull(repository.saved)

        viewModel.confirmDialog()
        val saved = assertNotNull(repository.saved)
        assertEquals(listOf("sl2"), saved.slots.map { it.id })
        assertEquals(listOf("c2"), saved.slots.map { it.childId })
    }

    @Test
    fun videoAssignmentsAppliedOnlyAfterConfirmation() {
        val videoMillis = LocalDateTime(date, LocalTime(8, 5)).toInstant(zone).toEpochMilliseconds()
        val lesson = Lesson(
            "l1", listOf(LessonParticipant("c1")), LocalDateTime(date, LocalTime(8, 0)), 30,
            type = LessonType.INDIVIDUAL, shiftId = "s1"
        )
        val repository = FakeShiftRepository(listOf(shift()))
        val viewModel = viewModel(
            repository = repository,
            lessons = listOf(lesson),
            videos = listOf(DeviceVideo("uri://v1", "v1", videoMillis))
        )
        viewModel.open("s1")

        viewModel.findVideos()
        val review = assertNotNull(viewModel.uiState.value.videoReview)
        assertEquals("l1", review.rows.single().suggestedLessonId)
        assertTrue(repository.attachCalls.isEmpty())

        viewModel.confirmVideoReview()
        assertEquals(
            listOf(ShiftVideoAssignment("uri://v1", "l1")),
            repository.attachCalls.single().second
        )
        assertNull(viewModel.uiState.value.videoReview)
    }

    private fun viewModel(
        repository: FakeShiftRepository = FakeShiftRepository(listOf(shift())),
        children: FakeChildRepository = FakeChildRepository(emptyList()),
        lessons: List<Lesson> = emptyList(),
        videos: List<DeviceVideo> = emptyList(),
        tags: List<Tag> = emptyList()
    ): ShiftViewModel {
        val lessonRepository = FakeLessonRepository(lessons)
        return ShiftViewModel(
            shiftRepository = repository,
            childRepository = children,
            lessonRepository = lessonRepository,
            tagRepository = FakeTagRepository(tags),
            findShiftVideos = FindShiftVideosUseCase(FakeVideoLibrary(videos), zone)
        )
    }

    private fun shift(
        start: LocalTime = LocalTime(8, 0),
        end: LocalTime = LocalTime(12, 0),
        breakMinutes: Int = 5,
        slots: List<ShiftSlot> = emptyList()
    ) = WorkShift("s1", date, start, end, baseDurationMinutes = 30, breakMinutes = breakMinutes, slots = slots)

    private fun shiftWithLinkedSlot() = shift(
        slots = listOf(
            ShiftSlot("sl1", 0, LocalTime(8, 0), 30, "c1", "l1"),
            ShiftSlot("sl2", 1, LocalTime(8, 35), 30, "c2", null)
        )
    )

    private class FakeShiftRepository(private val initial: List<WorkShift>) : ShiftRepository {
        private val _state = MutableStateFlow<ShiftLoadState>(ShiftLoadState.Ready(initial))
        override val state: StateFlow<ShiftLoadState> = _state.asStateFlow()
        var saved: WorkShift? = null
        var savedTemplate: ShiftTemplate? = null
        var generated: Pair<String, LocalDate>? = null
        var createLessonsCalls = 0
        val attachCalls = mutableListOf<Pair<String, List<ShiftVideoAssignment>>>()

        override fun retryLoading() {}
        override fun observeShifts(): Flow<List<WorkShift>> = flowOf(initial)
        override fun observeShift(id: String): Flow<WorkShift?> = flowOf(initial.find { it.id == id })
        override fun observeTemplates(): Flow<List<ShiftTemplate>> = flowOf(emptyList())
        override suspend fun findTemplate(id: String): ShiftTemplate? = null
        override suspend fun createShift(shift: WorkShift): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun saveShift(shift: WorkShift): ShiftWriteResult {
            saved = shift
            return ShiftWriteResult.Success
        }
        override suspend fun deleteShift(id: String): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun createLessons(shiftId: String): CreateShiftLessonsResult {
            createLessonsCalls++
            return CreateShiftLessonsResult.Success(1, 0)
        }
        override suspend fun deleteShiftLessons(shiftId: String): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun attachVideos(shiftId: String, assignments: List<ShiftVideoAssignment>): ShiftWriteResult {
            attachCalls += shiftId to assignments
            return ShiftWriteResult.Success
        }
        override suspend fun saveTemplate(template: ShiftTemplate): ShiftWriteResult {
            savedTemplate = template
            return ShiftWriteResult.Success
        }
        override suspend fun deleteTemplate(id: String): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun generateShifts(templateId: String, today: LocalDate): ShiftGenerationResult {
            generated = templateId to today
            return ShiftGenerationResult.Success(2, 1)
        }
    }

    private class FakeChildRepository(initial: List<Child>) : ChildRepository {
        private val _state = MutableStateFlow<ChildLoadState>(ChildLoadState.Ready(initial))
        override val state: StateFlow<ChildLoadState> = _state.asStateFlow()
        var added: Child? = null

        override fun retryLoading() {}
        override fun observeCard(id: String): Flow<Child?> = flowOf(_state.value.children.find { it.id == id })
        override suspend fun addChild(child: Child): ChildWriteResult {
            added = child
            _state.value = ChildLoadState.Ready(_state.value.children + child)
            return ChildWriteResult.Success
        }
        override suspend fun updateChild(child: Child): ChildWriteResult = ChildWriteResult.Success
        override suspend fun saveCard(child: Child): ChildWriteResult = ChildWriteResult.Success
        override suspend fun deleteChild(child: Child): ChildWriteResult = ChildWriteResult.Success
    }

    @Test
    fun addChildrenAddsSlotPerChildInOrder() {
        val viewModel = viewModel()
        viewModel.open("s1")
        viewModel.changeBaseDuration("40")

        viewModel.addChildren(listOf(Child("c1", "Аня"), Child("c2", "Максим"), Child("c3", "Ваня")))

        val slots = viewModel.uiState.value.slots
        assertEquals(listOf("Аня", "Максим", "Ваня"), slots.map { it.childName })
        assertEquals(listOf("40", "40", "40"), slots.map { it.durationText })
        assertEquals(LocalTime(8, 0), slots[0].startTime)
        assertEquals(LocalTime(8, 45), slots[1].startTime)
        assertEquals(LocalTime(9, 30), slots[2].startTime)
    }

    @Test
    fun addChildrenWithEmptyListKeepsSlots() {
        val viewModel = viewModel()
        viewModel.open("s1")

        viewModel.addChildren(emptyList())

        assertTrue(viewModel.uiState.value.slots.isEmpty())
        assertFalse(viewModel.uiState.value.dirty)
    }

    private class FakeTagRepository(initial: List<Tag>) : TagRepository {
        private val _state = MutableStateFlow<TagLoadState>(TagLoadState.Ready(initial))
        override val state: StateFlow<TagLoadState> = _state.asStateFlow()

        override fun retryLoading() {}
        override suspend fun saveTag(tag: Tag): TagWriteResult = TagWriteResult.Success
        override suspend fun deleteTag(tag: Tag): TagWriteResult = TagWriteResult.Success
        override suspend fun reorderTags(orderedIds: List<String>): TagWriteResult = TagWriteResult.Success
    }

    private class FakeLessonRepository(initial: List<Lesson>) : LessonRepository {
        private val _state = MutableStateFlow<LessonLoadState>(LessonLoadState.Ready(initial))
        override val state: StateFlow<LessonLoadState> = _state.asStateFlow()

        override fun retryLoading() {}
        override suspend fun addLesson(lesson: Lesson): LessonWriteResult = LessonWriteResult.Success(lesson)
        override suspend fun updateLesson(lesson: Lesson): LessonWriteResult = LessonWriteResult.Success(lesson)
        override suspend fun deleteLesson(id: String): LessonWriteResult = LessonWriteResult.Failure(LessonWriteResult.Reason.NotReady)
        override suspend fun generateChildLessons(childId: String, now: LocalDateTime): GenerateChildLessonsResult =
            GenerateChildLessonsResult.Success(GenerationCounts())
    }

    private class FakeVideoLibrary(private val videos: List<DeviceVideo>) : VideoLibrary {
        override suspend fun findVideos(from: Instant, to: Instant): List<DeviceVideo> = videos
    }
}