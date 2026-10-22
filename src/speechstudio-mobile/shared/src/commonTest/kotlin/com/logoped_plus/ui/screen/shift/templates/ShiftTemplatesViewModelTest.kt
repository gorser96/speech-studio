@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.shift.templates

import com.logoped_plus.domain.model.ShiftTemplate
import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.domain.repository.CreateShiftLessonsResult
import com.logoped_plus.domain.repository.ShiftGenerationResult
import com.logoped_plus.domain.repository.ShiftLoadState
import com.logoped_plus.domain.repository.ShiftRepository
import com.logoped_plus.domain.repository.ShiftVideoAssignment
import com.logoped_plus.domain.repository.ShiftWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftTemplatesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate(2026, 9, 1)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun generateSavesTemplateAndCreatesShiftsFromToday() {
        val repository = FakeTemplatesRepository()
        val viewModel = viewModel(repository)
        viewModel.newTemplate()
        viewModel.changeName("  Утро  ")
        viewModel.toggleDay(1)
        viewModel.setEndDate(LocalDate(2026, 9, 30))

        viewModel.generate()

        val template = assertNotNull(repository.savedTemplate)
        assertEquals("Утро", template.name)
        assertEquals(listOf(1), template.daysOfWeek)
        assertEquals(template.id, repository.generated?.first)
        assertEquals(today, repository.generated?.second)
        assertEquals(null, viewModel.uiState.value.draft)
    }

    @Test
    fun saveWithUnreadableNumbersKeepsDraftAndShowsError() {
        val repository = FakeTemplatesRepository()
        val viewModel = viewModel(repository)
        viewModel.newTemplate()
        viewModel.changeBase("много")

        viewModel.save()

        assertEquals(ShiftWriteResult.Reason.InvalidData, viewModel.uiState.value.draftError)
        assertNotNull(viewModel.uiState.value.draft)
        assertEquals(null, repository.savedTemplate)
    }

    @Test
    fun deleteRequiresConfirmation() {
        val template = ShiftTemplate(id = "t1", name = "Утро", daysOfWeek = listOf(1))
        val repository = FakeTemplatesRepository(listOf(template))
        val viewModel = viewModel(repository)

        viewModel.requestDelete("t1")
        assertEquals(template, viewModel.uiState.value.deleteTarget)
        assertEquals(null, repository.deletedId)

        viewModel.confirmDelete()
        assertEquals("t1", repository.deletedId)
        assertEquals(null, viewModel.uiState.value.deleteTarget)
    }

    private fun viewModel(repository: FakeTemplatesRepository): ShiftTemplatesViewModel =
        ShiftTemplatesViewModel(
            shiftRepository = repository,
            clock = FixedClock,
            timeZone = kotlinx.datetime.TimeZone.UTC
        )

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.parse("2026-09-01T10:00:00Z")
    }

    private class FakeTemplatesRepository(
        private val templates: List<ShiftTemplate> = emptyList()
    ) : ShiftRepository {
        override val state: StateFlow<ShiftLoadState> =
            MutableStateFlow<ShiftLoadState>(ShiftLoadState.Ready(emptyList()))
        var savedTemplate: ShiftTemplate? = null
        var generated: Pair<String, LocalDate>? = null
        var deletedId: String? = null

        override fun retryLoading() {}
        override fun observeShifts(): Flow<List<WorkShift>> = flowOf(emptyList())
        override fun observeShift(id: String): Flow<WorkShift?> = flowOf(null)
        override fun observeTemplates(): Flow<List<ShiftTemplate>> = flowOf(templates)
        override suspend fun findTemplate(id: String): ShiftTemplate? = templates.find { it.id == id }

        override suspend fun createShift(shift: WorkShift): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun saveShift(shift: WorkShift): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun deleteShift(id: String): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun createLessons(shiftId: String): CreateShiftLessonsResult =
            CreateShiftLessonsResult.Success(0, 0)
        override suspend fun deleteShiftLessons(shiftId: String): ShiftWriteResult = ShiftWriteResult.Success
        override suspend fun attachVideos(
            shiftId: String,
            assignments: List<ShiftVideoAssignment>
        ): ShiftWriteResult = ShiftWriteResult.Success

        override suspend fun saveTemplate(template: ShiftTemplate): ShiftWriteResult {
            savedTemplate = template
            return ShiftWriteResult.Success
        }

        override suspend fun deleteTemplate(id: String): ShiftWriteResult {
            deletedId = id
            return ShiftWriteResult.Success
        }

        override suspend fun generateShifts(templateId: String, today: LocalDate): ShiftGenerationResult {
            generated = templateId to today
            return ShiftGenerationResult.Success(2, 1)
        }
    }
}
