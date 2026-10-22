package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.ShiftTemplate
import com.logoped_plus.domain.model.WorkShift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/** Подтверждённая привязка видео к занятию смены. */
data class ShiftVideoAssignment(val uri: String, val lessonId: String)

interface ShiftRepository {

    val state: StateFlow<ShiftLoadState>
    fun retryLoading()

    fun observeShifts(): Flow<List<WorkShift>>
    fun observeShift(id: String): Flow<WorkShift?>
    fun observeTemplates(): Flow<List<ShiftTemplate>>
    suspend fun findTemplate(id: String): ShiftTemplate?

    suspend fun createShift(shift: WorkShift): ShiftWriteResult
    suspend fun saveShift(shift: WorkShift): ShiftWriteResult
    suspend fun deleteShift(id: String): ShiftWriteResult

    suspend fun createLessons(shiftId: String): CreateShiftLessonsResult
    suspend fun deleteShiftLessons(shiftId: String): ShiftWriteResult
    suspend fun attachVideos(shiftId: String, assignments: List<ShiftVideoAssignment>): ShiftWriteResult

    suspend fun saveTemplate(template: ShiftTemplate): ShiftWriteResult
    suspend fun deleteTemplate(id: String): ShiftWriteResult
    suspend fun generateShifts(templateId: String, today: LocalDate): ShiftGenerationResult
}