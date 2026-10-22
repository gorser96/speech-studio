package com.logoped_plus.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.model.ShiftTemplate
import com.logoped_plus.domain.model.ShiftTimeCalculator
import com.logoped_plus.domain.model.ShiftValidation
import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.domain.repository.CreateShiftLessonsResult
import com.logoped_plus.domain.repository.ShiftGenerationResult
import com.logoped_plus.domain.repository.ShiftWriteResult
import com.logoped_plus.domain.usecase.ShiftGenerationPlanner
import com.logoped_plus.platform.randomUuidString
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

data class LessonVideoRow(
    @ColumnInfo(name = "lessonId") val lessonId: String,
    val uri: String
)

/** Конфликт создания занятий смены: транзакция откатывается целиком. */
internal class ShiftLessonConflictException : Exception("Конфликт занятия смены")

@Dao
abstract class ShiftDao {

    @Query("SELECT * FROM shift_templates ORDER BY position ASC")
    abstract fun observeTemplates(): Flow<List<ShiftTemplateEntity>>

    @Query("SELECT * FROM shift_templates WHERE id = :id")
    abstract suspend fun findTemplate(id: String): ShiftTemplateEntity?

    @Insert
    abstract suspend fun insertTemplate(row: ShiftTemplateEntity)

    @Query(
        "UPDATE shift_templates SET name = :name, startNanoOfDay = :startNanoOfDay, endNanoOfDay = :endNanoOfDay, " +
            "baseDurationMinutes = :baseDurationMinutes, breakMinutes = :breakMinutes, daysMask = :daysMask, " +
            "endEpochDay = :endEpochDay WHERE id = :id"
    )
    abstract suspend fun updateTemplate(
        id: String, name: String, startNanoOfDay: Long, endNanoOfDay: Long,
        baseDurationMinutes: Int, breakMinutes: Int, daysMask: Int, endEpochDay: Long?
    ): Int

    @Query("DELETE FROM shift_templates WHERE id = :id")
    abstract suspend fun deleteTemplateRow(id: String): Int

    @Transaction
    @Query("SELECT * FROM work_shifts ORDER BY epochDay ASC, startNanoOfDay ASC")
    abstract fun observeShifts(): Flow<List<ShiftWithSlots>>

    @Transaction
    @Query("SELECT * FROM work_shifts WHERE id = :id")
    abstract fun observeShift(id: String): Flow<ShiftWithSlots?>

    @Query("SELECT * FROM work_shifts WHERE id = :id")
    abstract suspend fun findShiftRow(id: String): WorkShiftEntity?

    @Transaction
    @Query("SELECT * FROM work_shifts WHERE id = :id")
    abstract suspend fun findShift(id: String): ShiftWithSlots?

    @Query("SELECT epochDay FROM work_shifts WHERE templateId = :templateId")
    abstract suspend fun findShiftDates(templateId: String): List<Long>

    @Insert
    abstract suspend fun insertShift(row: WorkShiftEntity)

    @Query(
        "UPDATE work_shifts SET epochDay = :epochDay, startNanoOfDay = :startNanoOfDay, " +
            "endNanoOfDay = :endNanoOfDay, baseDurationMinutes = :baseDurationMinutes, " +
            "breakMinutes = :breakMinutes, templateId = :templateId WHERE id = :id"
    )
    abstract suspend fun updateShiftWindow(
        id: String, epochDay: Long, startNanoOfDay: Long, endNanoOfDay: Long,
        baseDurationMinutes: Int, breakMinutes: Int, templateId: String?
    ): Int

    @Query("DELETE FROM work_shifts WHERE id = :id")
    abstract suspend fun deleteShiftRow(id: String): Int

    @Query("SELECT * FROM shift_slots WHERE shiftId = :shiftId ORDER BY position ASC")
    abstract suspend fun findSlots(shiftId: String): List<ShiftSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSlots(rows: List<ShiftSlotEntity>)

    @Query("DELETE FROM shift_slots WHERE shiftId = :shiftId")
    abstract suspend fun clearSlots(shiftId: String)

    @Query("SELECT id FROM children WHERE id IN (:ids)")
    abstract suspend fun knownChildren(ids: List<String>): List<String>

    @Query("SELECT * FROM lessons WHERE id = :id")
    abstract suspend fun findLessonRow(id: String): LessonEntity?

    @Query(
        "SELECT l.id FROM lessons l JOIN lesson_participants p ON p.lessonId = l.id " +
            "WHERE p.childId = :childId AND l.scheduledEpochDay = :epochDay AND l.scheduledNanoOfDay = :nanoOfDay"
    )
    abstract suspend fun findLessonIdsAt(childId: String, epochDay: Long, nanoOfDay: Long): List<String>

    @Insert
    abstract suspend fun insertLesson(row: LessonEntity)

    @Insert
    abstract suspend fun insertParticipant(row: LessonParticipantEntity)

    @Query("SELECT childId FROM lesson_participants WHERE lessonId = :lessonId ORDER BY ordinal ASC")
    abstract suspend fun participantChildIds(lessonId: String): List<String>

    @Query("DELETE FROM lesson_participants WHERE lessonId = :lessonId")
    abstract suspend fun clearParticipants(lessonId: String)

    @Query(
        "UPDATE lessons SET shiftId = :shiftId, scheduledEpochDay = :epochDay, scheduledNanoOfDay = :nanoOfDay, " +
            "durationMinutes = :durationMinutes WHERE id = :id"
    )
    abstract suspend fun applySlotToLesson(
        id: String, shiftId: String, epochDay: Long, nanoOfDay: Long, durationMinutes: Int
    ): Int

    @Query("UPDATE lessons SET shiftId = :shiftId WHERE id = :id")
    abstract suspend fun applyLessonShift(id: String, shiftId: String): Int

    @Query("UPDATE lessons SET shiftId = NULL WHERE id = :id")
    abstract suspend fun unlinkLesson(id: String)

    @Query("UPDATE lessons SET shiftId = NULL WHERE shiftId = :shiftId")
    abstract suspend fun unlinkShiftLessons(shiftId: String)

    @Query("UPDATE shift_slots SET lessonId = NULL WHERE shiftId = :shiftId")
    abstract suspend fun clearSlotLessons(shiftId: String)

    @Query("DELETE FROM lessons WHERE shiftId = :shiftId")
    abstract suspend fun deleteShiftLessonRows(shiftId: String)

    @Query("UPDATE shift_slots SET lessonId = :lessonId WHERE id = :slotId")
    abstract suspend fun linkSlot(slotId: String, lessonId: String)

    @Query("SELECT v.lessonId AS lessonId, v.uri AS uri FROM lesson_videos v WHERE v.lessonId IN (SELECT id FROM lessons WHERE shiftId = :shiftId)")
    abstract suspend fun attachedVideos(shiftId: String): List<LessonVideoRow>

    @Query("SELECT uri FROM lesson_videos WHERE lessonId = :lessonId")
    abstract suspend fun videoUris(lessonId: String): List<String>

    @Query("SELECT COALESCE(MAX(ordinal), -1) + 1 FROM lesson_videos WHERE lessonId = :lessonId")
    abstract suspend fun nextVideoOrdinal(lessonId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertVideo(row: LessonVideoEntity)

    @Transaction
    open suspend fun saveTemplateOnce(template: ShiftTemplate): ShiftWriteResult {
        ShiftValidation.templateIssue(template)?.let { return ShiftWriteResult.Failure(ShiftWriteResult.Reason.InvalidData) }
        val row = template.toEntity()
        if (findTemplate(template.id) == null) {
            insertTemplate(row)
        } else if (updateTemplate(
                id = row.id, name = row.name, startNanoOfDay = row.startNanoOfDay, endNanoOfDay = row.endNanoOfDay,
                baseDurationMinutes = row.baseDurationMinutes, breakMinutes = row.breakMinutes,
                daysMask = row.daysMask, endEpochDay = row.endEpochDay
            ) != 1
        ) {
            return ShiftWriteResult.Failure(ShiftWriteResult.Reason.NotFound)
        }
        return ShiftWriteResult.Success
    }

    @Transaction
    open suspend fun deleteTemplateOnce(id: String): ShiftWriteResult {
        if (findTemplate(id) == null) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.NotFound)
        // Смены сохраняются без шаблона и не удаляются вместе с ним.
        unlinkTemplateShifts(id)
        deleteTemplateRow(id)
        return ShiftWriteResult.Success
    }

    @Query("UPDATE work_shifts SET templateId = NULL WHERE templateId = :templateId")
    abstract suspend fun unlinkTemplateShifts(templateId: String)

    @Transaction
    open suspend fun createShiftOnce(shift: WorkShift): ShiftWriteResult {
        ShiftValidation.shiftIssue(shift)?.let { return ShiftWriteResult.Failure(ShiftWriteResult.Reason.InvalidData) }
        if (shift.slots.isNotEmpty()) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.InvalidData)
        val existing = findShiftRow(shift.id)
        if (existing != null) {
            return if (existing.epochDay == shift.date.toEpochDays()) ShiftWriteResult.Success
            else ShiftWriteResult.Failure(ShiftWriteResult.Reason.InvalidData)
        }
        insertShift(
            WorkShiftEntity(
                id = shift.id, epochDay = shift.date.toEpochDays(),
                startNanoOfDay = shift.startTime.toNanosecondOfDay(), endNanoOfDay = shift.endTime.toNanosecondOfDay(),
                baseDurationMinutes = shift.baseDurationMinutes, breakMinutes = shift.breakMinutes,
                templateId = shift.templateId
            )
        )
        return ShiftWriteResult.Success
    }

    /**
     * Сохраняет окно, базовую длительность, перерыв, дату и упорядоченные слоты одной транзакцией.
     * Начала слотов пересчитываются; связанные занятия следуют за слотами и датой смены
     * (время, дата, длительность, участник), слоты без занятия обнуляют ссылку занятия на смену.
     */
    @Transaction
    open suspend fun saveShiftOnce(shift: WorkShift): ShiftWriteResult {
        ShiftValidation.shiftIssue(shift)?.let { return ShiftWriteResult.Failure(ShiftWriteResult.Reason.InvalidData) }
        if (findShiftRow(shift.id) == null) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.NotFound)
        val childIds = shift.slots.map { it.childId }
        if (childIds.any { it.isBlank() }) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.InvalidData)
        if (childIds.isNotEmpty() && knownChildren(childIds).toSet() != childIds.toSet())
            return ShiftWriteResult.Failure(ShiftWriteResult.Reason.UnknownChild)
        val geometry = ShiftTimeCalculator.compute(
            shift.startTime, shift.endTime, shift.breakMinutes, shift.slots.map { it.durationMinutes }
        )
        if (geometry.starts.size != shift.slots.size) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.InvalidData)

        val previous = findSlots(shift.id)
        val linkedLessons = shift.slots.mapNotNull { slot -> slot.lessonId?.let { id -> id to slot } }.toMap()
        val linkable = linkedLessons.filterValues { slot ->
            val row = findLessonRow(slot.lessonId!!)
            row != null && row.type == LessonType.INDIVIDUAL.name && (row.shiftId == null || row.shiftId == shift.id)
        }
        val epochDay = shift.date.toEpochDays()
        val rows = shift.slots.mapIndexed { index, slot ->
            val lessonId = slot.lessonId?.takeIf { it in linkable }
            ShiftSlotEntity(
                id = slot.id, shiftId = shift.id, position = index,
                startNanoOfDay = geometry.starts[index].toNanosecondOfDay(),
                durationMinutes = slot.durationMinutes, childId = slot.childId, lessonId = lessonId
            )
        }
        updateShiftWindow(
            id = shift.id, epochDay = epochDay, startNanoOfDay = shift.startTime.toNanosecondOfDay(),
            endNanoOfDay = shift.endTime.toNanosecondOfDay(), baseDurationMinutes = shift.baseDurationMinutes,
            breakMinutes = shift.breakMinutes, templateId = shift.templateId
        )
        clearSlots(shift.id)
        // Занятия, потерявшие слот, остаются в расписании без смены.
        previous.mapNotNull { it.lessonId }.filter { it !in linkable.keys }.forEach { unlinkLesson(it) }
        insertSlots(rows)
        for (index in rows.indices) {
            val lessonId = rows[index].lessonId ?: continue
            if (applySlotToLesson(
                    id = lessonId, shiftId = shift.id, epochDay = epochDay,
                    nanoOfDay = rows[index].startNanoOfDay, durationMinutes = rows[index].durationMinutes
                ) != 1
            ) {
                throw IllegalStateException("Занятие слота не найдено: $lessonId")
            }
            val childId = rows[index].childId
            if (participantChildIds(lessonId) != listOf(childId)) {
                clearParticipants(lessonId)
                insertParticipant(LessonParticipantEntity(lessonId, childId, null, 0))
            }
        }
        return ShiftWriteResult.Success
    }

    @Transaction
    open suspend fun deleteShiftOnce(id: String): ShiftWriteResult {
        if (findShiftRow(id) == null) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.NotFound)
        // Занятия остаются в расписании без смены, слоты удаляются вместе со сменой.
        unlinkShiftLessons(id)
        clearSlotLessons(id)
        clearSlots(id)
        deleteShiftRow(id)
        return ShiftWriteResult.Success
    }

    /**
     * Атомарно создаёт по одному индивидуальному занятию на заполненный слот. Существующее
     * индивидуальное занятие того же ребёнка, начала и длительности связывается со сменой;
     * групповое занятие, другая длительность или занятие другой смены — конфликт без изменений.
     */
    @Transaction
    open suspend fun createLessonsOnce(shiftId: String): CreateShiftLessonsResult {
        val shift = findShiftRow(shiftId) ?: return CreateShiftLessonsResult.Failure(CreateShiftLessonsResult.Reason.NotFound)
        val slots = findSlots(shiftId)
        var created = 0
        var linked = 0
        var conflict = false
        for (slot in slots) {
            val current = slot.lessonId?.let { findLessonRow(it) }
            if (current != null && current.shiftId == shiftId) continue
            if (current != null) {
                conflict = true
                break
            }
            val candidates = findLessonIdsAt(slot.childId, shift.epochDay, slot.startNanoOfDay)
            if (candidates.size > 1) {
                conflict = true
                break
            }
            val existing = candidates.firstOrNull()?.let { findLessonRow(it) }
            if (existing != null) {
                if (existing.type != LessonType.INDIVIDUAL.name ||
                    existing.durationMinutes != slot.durationMinutes ||
                    (existing.shiftId != null && existing.shiftId != shiftId)
                ) {
                    conflict = true
                    break
                }
                applyLessonShift(existing.id, shiftId)
                linkSlot(slot.id, existing.id)
                linked++
            } else {
                val id = randomUuidString()
                insertLesson(
                    LessonEntity(
                        id = id, scheduledEpochDay = shift.epochDay, scheduledNanoOfDay = slot.startNanoOfDay,
                        durationMinutes = slot.durationMinutes, comment = "", type = LessonType.INDIVIDUAL.name,
                        shiftId = shiftId
                    )
                )
                insertParticipant(LessonParticipantEntity(id, slot.childId, null, 0))
                linkSlot(slot.id, id)
                created++
            }
        }
        if (conflict) throw ShiftLessonConflictException()
        return CreateShiftLessonsResult.Success(created, linked)
    }

    @Transaction
    open suspend fun deleteShiftLessonsOnce(shiftId: String): ShiftWriteResult {
        if (findShiftRow(shiftId) == null) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.NotFound)
        // Файлы видео не удаляются: удаляются записи занятий и их привязки.
        clearSlotLessons(shiftId)
        deleteShiftLessonRows(shiftId)
        return ShiftWriteResult.Success
    }

    /**
     * Добавляет только новые привязки видео к занятиям смены, не снимая связи с другими
     * занятиями и не создавая дубликатов.
     */
    @Transaction
    open suspend fun attachVideosOnce(shiftId: String, assignments: List<Pair<String, String>>): ShiftWriteResult {
        if (findShiftRow(shiftId) == null) return ShiftWriteResult.Failure(ShiftWriteResult.Reason.NotFound)
        for ((uri, lessonId) in assignments) {
            if (uri.isBlank()) continue
            val lesson = findLessonRow(lessonId) ?: continue
            if (lesson.shiftId != shiftId) continue
            if (uri in videoUris(lessonId)) continue
            insertVideo(LessonVideoEntity(lessonId, uri, nextVideoOrdinal(lessonId)))
        }
        return ShiftWriteResult.Success
    }

    @Transaction
    open suspend fun generateShiftsOnce(templateId: String, today: LocalDate): ShiftGenerationResult {
        val template = findTemplate(templateId)?.toDomain()
            ?: return ShiftGenerationResult.Failure(ShiftGenerationResult.Reason.TemplateNotFound)
        ShiftValidation.templateIssue(template)
            ?.let { return ShiftGenerationResult.Failure(ShiftGenerationResult.Reason.InvalidTemplate) }
        if (ShiftValidation.missingEndDate(template))
            return ShiftGenerationResult.Failure(ShiftGenerationResult.Reason.InvalidTemplate)
        val existing = findShiftDates(templateId).map { LocalDate.fromEpochDays(it) }.toSet()
        val planned = ShiftGenerationPlanner.plan(template, today, emptySet())
        val missing = planned.filter { it !in existing }
        missing.forEach { date ->
            insertShift(
                WorkShiftEntity(
                    id = randomUuidString(), epochDay = date.toEpochDays(),
                    startNanoOfDay = template.startTime.toNanosecondOfDay(),
                    endNanoOfDay = template.endTime.toNanosecondOfDay(),
                    baseDurationMinutes = template.baseDurationMinutes, breakMinutes = template.breakMinutes,
                    templateId = templateId
                )
            )
        }
        return ShiftGenerationResult.Success(missing.size, planned.size - missing.size)
    }
}