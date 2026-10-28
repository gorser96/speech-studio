@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.ui.screen.children

import com.speech_studio.data.preferences.SettingsRepository
import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.ChildSchedule
import com.speech_studio.domain.model.GenerationCounts
import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.model.Tag
import com.speech_studio.domain.model.TagColorKey
import com.speech_studio.domain.repository.ChildLoadState
import com.speech_studio.domain.repository.ChildRepository
import com.speech_studio.domain.repository.ChildWriteResult
import com.speech_studio.domain.repository.GenerateChildLessonsResult
import com.speech_studio.domain.repository.LessonLoadState
import com.speech_studio.domain.repository.LessonRepository
import com.speech_studio.domain.repository.LessonWriteResult
import com.speech_studio.domain.repository.TagLoadState
import com.speech_studio.domain.repository.TagRepository
import com.speech_studio.domain.repository.TagWriteResult
import com.speech_studio.domain.usecase.GenerateChildLessonsUseCase
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
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChildrenViewModelCardTagsTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openCardExposesAssignedTagsAndDirectory() {
        val child = Child("c1", "Анна", listOf("t1", "t2"), ChildSchedule())
        val tags = listOf(Tag("t1", "Группа А", TagColorKey.RED), Tag("t2", "Бис", TagColorKey.BLUE))
        val childRepo = FakeChildRepository(listOf(child), child)
        val tagRepo = FakeTagRepository(tags)
        val viewModel = ChildrenViewModel(
            childRepo, tagRepo, GenerateChildLessonsUseCase(FakeLessonRepository()),
            FakeSettingsRepository()
        )

        viewModel.openCard("c1")

        val state = viewModel.uiState.value
        assertEquals("c1", state.cardId)
        assertEquals(child, state.card)
        assertEquals(listOf("t1", "t2"), state.card?.tagIds)
        assertTrue(state.tagsLoadState is TagLoadState.Ready)
        assertEquals(tags, (state.tagsLoadState as TagLoadState.Ready).tags)
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val hideWeekendFlow = MutableStateFlow(false)
        private val gridStartHourFlow = MutableStateFlow(0)
        private val gridEndHourFlow = MutableStateFlow(24)
        private val visibleTagsLimitFlow = MutableStateFlow(3)

        override val hideWeekend: StateFlow<Boolean> = hideWeekendFlow.asStateFlow()
        override val gridStartHour: StateFlow<Int> = gridStartHourFlow.asStateFlow()
        override val gridEndHour: StateFlow<Int> = gridEndHourFlow.asStateFlow()
        override val visibleTagsLimit: StateFlow<Int> = visibleTagsLimitFlow.asStateFlow()

        override fun setHideWeekend(value: Boolean) { hideWeekendFlow.value = value }
        override fun setGridStartHour(value: Int) { gridStartHourFlow.value = value }
        override fun setGridEndHour(value: Int) { gridEndHourFlow.value = value }
        override fun setVisibleTagsLimit(value: Int) { visibleTagsLimitFlow.value = value }
    }

    private class FakeChildRepository(children: List<Child>, private val card: Child?) : ChildRepository {
        private val _state = MutableStateFlow<ChildLoadState>(ChildLoadState.Ready(children))
        override val state: StateFlow<ChildLoadState> = _state.asStateFlow()
        override fun retryLoading() {}
        override fun observeCard(id: String): Flow<Child?> = flowOf(card?.takeIf { it.id == id })
        override suspend fun addChild(child: Child): ChildWriteResult = ChildWriteResult.Success
        override suspend fun updateChild(child: Child): ChildWriteResult = ChildWriteResult.Success
        override suspend fun saveCard(child: Child): ChildWriteResult = ChildWriteResult.Success
        override suspend fun deleteChild(child: Child): ChildWriteResult = ChildWriteResult.Success
    }

    private class FakeTagRepository(tags: List<Tag>) : TagRepository {
        private val _state = MutableStateFlow<TagLoadState>(TagLoadState.Ready(tags))
        override val state: StateFlow<TagLoadState> = _state.asStateFlow()
        override fun retryLoading() {}
        override suspend fun saveTag(tag: Tag): TagWriteResult = TagWriteResult.Success
        override suspend fun deleteTag(tag: Tag): TagWriteResult = TagWriteResult.Success
        override suspend fun reorderTags(orderedIds: List<String>): TagWriteResult = TagWriteResult.Success
    }

    private class FakeLessonRepository : LessonRepository {
        private val _state = MutableStateFlow<LessonLoadState>(LessonLoadState.Ready(emptyList()))
        override val state: StateFlow<LessonLoadState> = _state.asStateFlow()
        override fun retryLoading() {}
        override suspend fun addLesson(lesson: Lesson): LessonWriteResult = LessonWriteResult.Failure(LessonWriteResult.Reason.NotReady)
        override suspend fun updateLesson(lesson: Lesson): LessonWriteResult = LessonWriteResult.Failure(LessonWriteResult.Reason.NotReady)
        override suspend fun deleteLesson(id: String): LessonWriteResult = LessonWriteResult.Failure(LessonWriteResult.Reason.NotReady)
        override suspend fun generateChildLessons(childId: String, now: LocalDateTime): GenerateChildLessonsResult =
            GenerateChildLessonsResult.Success(GenerationCounts())
    }
}
