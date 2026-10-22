package com.logoped_plus.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.logoped_plus.domain.model.ChildSchedule
import com.logoped_plus.domain.model.GenerationCounts
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonSpan
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.model.WeeklyLessonSlot
import com.logoped_plus.domain.repository.GenerateChildLessonsResult
import com.logoped_plus.domain.repository.LessonWriteResult
import com.logoped_plus.domain.usecase.RecurringLessonPlanner
import com.logoped_plus.domain.usecase.RecurringPlanResult
import com.logoped_plus.platform.randomUuidString
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

data class LessonSpanRow(
    @ColumnInfo(name = "scheduledEpochDay") val epochDay: Long,
    @ColumnInfo(name = "scheduledNanoOfDay") val nanoOfDay: Long,
    val durationMinutes: Int
) {
    fun toDomain() = LessonSpan(epochDay, nanoOfDay, durationMinutes)
}

@Dao
abstract class LessonDao {
    @Transaction
    @Query("SELECT * FROM lessons ORDER BY position ASC")
    abstract fun observeLessons(): Flow<List<LessonWithRelations>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    abstract suspend fun find(id: String): LessonWithRelations?

    @Query("SELECT id FROM children WHERE id IN (:ids)")
    abstract suspend fun knownChildren(ids: List<String>): List<String>

    @Insert abstract suspend fun insertLesson(lesson: LessonEntity)
    @Update abstract suspend fun updateScalars(lesson: LessonEntity): Int
    @Insert abstract suspend fun insertParticipants(rows: List<LessonParticipantEntity>)
    @Insert abstract suspend fun insertVideos(rows: List<LessonVideoEntity>)
    @Query("DELETE FROM lesson_participants WHERE lessonId = :id")
    abstract suspend fun clearParticipants(id: String)
    @Query("DELETE FROM lesson_videos WHERE lessonId = :id")
    abstract suspend fun clearVideos(id: String)
    @Query("DELETE FROM lessons WHERE id = :id")
    abstract suspend fun deleteLesson(id: String)

    @Query("SELECT endEpochDay FROM child_schedule WHERE childId = :id")
    abstract suspend fun findScheduleEndEpochDay(id: String): Long?

    @Query("SELECT * FROM child_schedule_slots WHERE childId = :id ORDER BY ordinal ASC")
    abstract suspend fun findScheduleSlots(id: String): List<ChildScheduleSlotEntity>

    @Query("SELECT l.scheduledEpochDay, l.scheduledNanoOfDay, l.durationMinutes FROM lessons l JOIN lesson_participants p ON p.lessonId = l.id WHERE p.childId = :id")
    abstract suspend fun findChildLessonSpans(id: String): List<LessonSpanRow>

    @Transaction
    open suspend fun addOnce(lesson: Lesson): LessonWriteResult {
        invalidReason(lesson)?.let { return LessonWriteResult.Failure(it) }
        val liveIds = lesson.participants.filter { it.removedName == null }.map { it.childId }
        if (liveIds.isNotEmpty() && knownChildren(liveIds).toSet() != liveIds.toSet())
            return LessonWriteResult.Failure(LessonWriteResult.Reason.UnknownChild)
        val existing = find(lesson.id)
        if (existing != null) return if (existing.toDomain() == lesson) LessonWriteResult.Success(lesson)
            else LessonWriteResult.Failure(LessonWriteResult.Reason.Conflict)
        insertLesson(lesson.toEntity())
        insertRelations(lesson)
        return LessonWriteResult.Success(lesson)
    }

    @Transaction
    open suspend fun generateForChild(childId: String, now: LocalDateTime): GenerateChildLessonsResult {
        if (knownChildren(listOf(childId)).isEmpty()) return GenerateChildLessonsResult.Failure(GenerateChildLessonsResult.Reason.NotFound)
        val endEpochDay = findScheduleEndEpochDay(childId) ?: return GenerateChildLessonsResult.Success(GenerationCounts())
        val schedule = ChildSchedule(
            slots = findScheduleSlots(childId).map {
                WeeklyLessonSlot(it.id, it.dayOfWeek, LocalTime.fromNanosecondOfDay(it.startNanoOfDay), it.durationMinutes)
            },
            endDate = LocalDate.fromEpochDays(endEpochDay)
        )
        when (val plan = RecurringLessonPlanner.plan(
            schedule = schedule,
            childId = childId,
            now = now,
            childLessonSpans = findChildLessonSpans(childId).map { it.toDomain() }
        )) {
            is RecurringPlanResult.Invalid -> return GenerateChildLessonsResult.Failure(GenerateChildLessonsResult.Reason.InvalidSchedule)
            is RecurringPlanResult.Valid -> {
                plan.lessons.forEach { planned ->
                    val id = randomUuidString()
                    insertLesson(LessonEntity(id = id, scheduledEpochDay = planned.startEpochDay,
                        scheduledNanoOfDay = planned.startNanoOfDay, durationMinutes = planned.durationMinutes,
                        comment = "", type = LessonType.INDIVIDUAL.name))
                    insertParticipants(listOf(LessonParticipantEntity(id, childId, null, 0)))
                }
                return GenerateChildLessonsResult.Success(plan.counts)
            }
        }
    }

    protected suspend fun insertRelations(lesson: Lesson) {
        insertParticipants(lesson.participants.mapIndexed { index, p -> LessonParticipantEntity(lesson.id, p.childId, p.removedName, index) })
        insertVideos(lesson.videoAttachments.mapIndexed { index, video -> LessonVideoEntity(lesson.id, video.uri, index) })
    }

    @Transaction
    open suspend fun updateExisting(lesson: Lesson): LessonWriteResult {
        invalidReason(lesson)?.let { return LessonWriteResult.Failure(it) }
        val existing = find(lesson.id) ?: return LessonWriteResult.Failure(LessonWriteResult.Reason.NotFound)
        val liveIds = lesson.participants.filter { it.removedName == null }.map { it.childId }
        if (liveIds.isNotEmpty() && knownChildren(liveIds).toSet() != liveIds.toSet())
            return LessonWriteResult.Failure(LessonWriteResult.Reason.UnknownChild)
        check(updateScalars(lesson.toEntity(existing.lesson.position)) == 1)
        clearParticipants(lesson.id)
        clearVideos(lesson.id)
        insertRelations(lesson)
        return LessonWriteResult.Success(lesson)
    }

    @Transaction
    open suspend fun deleteExisting(id: String): LessonWriteResult {
        val existing = find(id) ?: return LessonWriteResult.Failure(LessonWriteResult.Reason.NotFound)
        deleteLesson(id)
        return LessonWriteResult.Success(existing.toDomain())
    }
}

internal fun invalidReason(lesson: Lesson): LessonWriteResult.Reason? =
    if (lesson.id.isBlank() || lesson.durationMinutes <= 0 || lesson.participants.isEmpty() ||
        lesson.participants.any { it.childId.isBlank() } ||
        lesson.participants.map { it.childId }.distinct().size != lesson.participants.size ||
        lesson.videoAttachments.any { it.uri.isBlank() } ||
        lesson.videoAttachments.map { it.uri }.distinct().size != lesson.videoAttachments.size ||
        (lesson.type == LessonType.GROUP && lesson.shiftId != null))
        LessonWriteResult.Reason.InvalidData else null
