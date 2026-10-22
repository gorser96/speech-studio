package com.logoped_plus.data.repository

import com.logoped_plus.data.local.ShiftDao
import com.logoped_plus.data.local.ShiftLessonConflictException
import com.logoped_plus.data.local.toDomain
import com.logoped_plus.domain.model.ShiftTemplate
import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.domain.repository.CreateShiftLessonsResult
import com.logoped_plus.domain.repository.ShiftGenerationResult
import com.logoped_plus.domain.repository.ShiftLoadState
import com.logoped_plus.domain.repository.ShiftRepository
import com.logoped_plus.domain.repository.ShiftVideoAssignment
import com.logoped_plus.domain.repository.ShiftWriteResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class RoomShiftRepository(private val dao: ShiftDao, scope: CoroutineScope) : ShiftRepository {
    private val mutableState = MutableStateFlow<ShiftLoadState>(ShiftLoadState.Loading())
    override val state = mutableState.asStateFlow()
    private val retries = MutableStateFlow(0L)

    init {
        scope.launch {
            retries.collectLatest {
                try {
                    dao.observeShifts().collect { rows ->
                        mutableState.value = ShiftLoadState.Ready(rows.map { it.toDomain() })
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.value = ShiftLoadState.Error(mutableState.value.shifts)
                }
            }
        }
    }

    override fun retryLoading() {
        val current = mutableState.value
        if (current !is ShiftLoadState.Loading && mutableState.compareAndSet(current, ShiftLoadState.Loading(current.shifts))) {
            retries.update { it + 1 }
        }
    }

    override fun observeShifts(): Flow<List<WorkShift>> = dao.observeShifts().map { rows -> rows.map { it.toDomain() } }

    override fun observeShift(id: String): Flow<WorkShift?> = dao.observeShift(id).map { it?.toDomain() }

    override fun observeTemplates(): Flow<List<ShiftTemplate>> =
        dao.observeTemplates().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findTemplate(id: String): ShiftTemplate? = try {
        dao.findTemplate(id)?.toDomain()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    override suspend fun createShift(shift: WorkShift): ShiftWriteResult = write { dao.createShiftOnce(shift) }

    override suspend fun saveShift(shift: WorkShift): ShiftWriteResult = write { dao.saveShiftOnce(shift) }

    override suspend fun deleteShift(id: String): ShiftWriteResult = write { dao.deleteShiftOnce(id) }

    override suspend fun createLessons(shiftId: String): CreateShiftLessonsResult {
        if (state.value !is ShiftLoadState.Ready) return CreateShiftLessonsResult.Failure(CreateShiftLessonsResult.Reason.NotReady)
        return try {
            dao.createLessonsOnce(shiftId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: ShiftLessonConflictException) {
            CreateShiftLessonsResult.Failure(CreateShiftLessonsResult.Reason.Conflict)
        } catch (_: Exception) {
            CreateShiftLessonsResult.Failure(CreateShiftLessonsResult.Reason.StorageUnavailable)
        }
    }

    override suspend fun deleteShiftLessons(shiftId: String): ShiftWriteResult = write { dao.deleteShiftLessonsOnce(shiftId) }

    override suspend fun attachVideos(shiftId: String, assignments: List<ShiftVideoAssignment>): ShiftWriteResult =
        write { dao.attachVideosOnce(shiftId, assignments.map { it.uri to it.lessonId }) }

    override suspend fun saveTemplate(template: ShiftTemplate): ShiftWriteResult = write { dao.saveTemplateOnce(template) }

    override suspend fun deleteTemplate(id: String): ShiftWriteResult = write { dao.deleteTemplateOnce(id) }

    override suspend fun generateShifts(templateId: String, today: LocalDate): ShiftGenerationResult {
        if (state.value !is ShiftLoadState.Ready) return ShiftGenerationResult.Failure(ShiftGenerationResult.Reason.NotReady)
        return try {
            dao.generateShiftsOnce(templateId, today)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ShiftGenerationResult.Failure(ShiftGenerationResult.Reason.StorageUnavailable)
        }
    }

    private suspend fun write(action: suspend () -> ShiftWriteResult): ShiftWriteResult {
        if (state.value !is ShiftLoadState.Ready) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.NotReady)
        return try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ShiftWriteResult.Failure(ShiftWriteResult.Reason.StorageUnavailable)
        }
    }
}