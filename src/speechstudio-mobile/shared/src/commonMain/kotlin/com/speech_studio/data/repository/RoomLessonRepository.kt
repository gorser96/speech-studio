package com.speech_studio.data.repository

import com.speech_studio.data.local.LessonDao
import com.speech_studio.data.local.invalidReason
import com.speech_studio.data.local.toDomain
import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class RoomLessonRepository(private val dao: LessonDao, private val children: ChildRepository, scope: CoroutineScope) : LessonRepository {
    private val mutableState = MutableStateFlow<LessonLoadState>(LessonLoadState.Loading())
    override val state = mutableState.asStateFlow()
    private val retries = MutableStateFlow(0L)

    init {
        scope.launch {
            retries.collectLatest {
                try {
                    dao.observeLessons().collect { rows -> mutableState.value = LessonLoadState.Ready(rows.map { it.toDomain() }) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.value = LessonLoadState.Error(mutableState.value.lessons)
                }
            }
        }
    }

    override fun retryLoading() {
        val current = mutableState.value
        if (current !is LessonLoadState.Loading && mutableState.compareAndSet(current, LessonLoadState.Loading(current.lessons))) {
            retries.update { it + 1 }
        }
    }

    override suspend fun addLesson(lesson: Lesson) = write(lesson) { dao.addOnce(it) }
    override suspend fun updateLesson(lesson: Lesson) = write(lesson) { dao.updateExisting(it) }
    override suspend fun generateChildLessons(childId: String, now: LocalDateTime): GenerateChildLessonsResult {
        if (state.value !is LessonLoadState.Ready || children.state.value !is ChildLoadState.Ready)
            return GenerateChildLessonsResult.Failure(GenerateChildLessonsResult.Reason.NotReady)
        return try {
            dao.generateForChild(childId, now)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GenerateChildLessonsResult.Failure(GenerateChildLessonsResult.Reason.StorageUnavailable)
        }
    }

    override suspend fun deleteLesson(id: String): LessonWriteResult {
        if (state.value !is LessonLoadState.Ready || children.state.value !is ChildLoadState.Ready)
            return LessonWriteResult.Failure(LessonWriteResult.Reason.NotReady)
        return try {
            dao.deleteExisting(id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            LessonWriteResult.Failure(LessonWriteResult.Reason.StorageUnavailable)
        }
    }

    private suspend fun write(lesson: Lesson, action: suspend (Lesson) -> LessonWriteResult): LessonWriteResult {
        invalidReason(lesson)?.let { return LessonWriteResult.Failure(it) }
        if (state.value !is LessonLoadState.Ready || children.state.value !is ChildLoadState.Ready)
            return LessonWriteResult.Failure(LessonWriteResult.Reason.NotReady)
        // Own the submitted collections while the caller continues to receive snapshots.
        val submitted = lesson.copy(participants = lesson.participants.toList(), videoAttachments = lesson.videoAttachments.toList())
        return try {
            action(submitted)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            LessonWriteResult.Failure(LessonWriteResult.Reason.StorageUnavailable)
        }
    }
}
