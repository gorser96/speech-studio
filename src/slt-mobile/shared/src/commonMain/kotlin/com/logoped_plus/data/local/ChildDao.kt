package com.logoped_plus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.logoped_plus.domain.model.Child
import com.logoped_plus.domain.repository.ChildWriteResult
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChildDao {
    @Query("SELECT * FROM children ORDER BY position ASC")
    abstract fun observeChildren(): Flow<List<ChildEntity>>

    @Transaction
    @Query("SELECT * FROM children ORDER BY position ASC")
    abstract fun observeChildrenWithTags(): Flow<List<ChildWithTags>>

    @Transaction
    @Query("SELECT * FROM children WHERE id = :id")
    abstract fun observeCard(id: String): Flow<List<ChildCardWithRelations>>

    @Query("SELECT * FROM children WHERE id = :id")
    abstract suspend fun find(id: String): ChildEntity?

    @Insert
    abstract suspend fun insert(child: ChildEntity)

    @Query("UPDATE children SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: String, name: String): Int

    @Query("SELECT name FROM children WHERE id = :id")
    abstract suspend fun findName(id: String): String?

    @Query("UPDATE lesson_participants SET childName = :name WHERE childId = :id AND childName IS NULL")
    abstract suspend fun snapshotChildName(id: String, name: String)

    @Query("DELETE FROM children WHERE id = :id")
    abstract suspend fun deleteChild(id: String): Int

    @Query("SELECT id FROM tags WHERE id IN (:ids)")
    abstract suspend fun knownTagIds(ids: List<String>): List<String>

    @Query("DELETE FROM child_tags WHERE childId = :id")
    abstract suspend fun clearTags(id: String)

    @Insert
    abstract suspend fun insertTags(rows: List<ChildTagEntity>)

    @Query("DELETE FROM child_schedule WHERE childId = :id")
    abstract suspend fun clearSchedule(id: String)

    @Insert
    abstract suspend fun insertSchedule(schedule: ChildScheduleEntity)

    @Insert
    abstract suspend fun insertSlots(rows: List<ChildScheduleSlotEntity>)

    @Transaction
    open suspend fun addOnce(id: String, name: String): Boolean {
        val existing = find(id)
        if (existing != null) return existing.name == name
        insert(ChildEntity(id = id, name = name))
        return true
    }

    @Transaction
    open suspend fun saveCardOnce(child: Child): ChildWriteResult {
        val existing = find(child.id)
        if (existing == null) return ChildWriteResult.Failure(ChildWriteResult.Reason.NotFound)
        if (rename(child.id, child.name) != 1) return ChildWriteResult.Failure(ChildWriteResult.Reason.NotFound)
        val liveTags = if (child.tagIds.isEmpty()) emptyList() else child.tagIds.filter { it in knownTagIds(child.tagIds).toSet() }
        clearTags(child.id)
        if (liveTags.isNotEmpty()) insertTags(liveTags.map { ChildTagEntity(child.id, it) })
        clearSchedule(child.id)
        val endDate = child.schedule.endDate
        if (child.schedule.slots.isNotEmpty() && endDate != null) {
            insertSchedule(ChildScheduleEntity(child.id, endDate.toEpochDays()))
            insertSlots(child.schedule.slots.mapIndexed { index, slot ->
                ChildScheduleSlotEntity(child.id, slot.id, slot.dayOfWeek, slot.startTime.toNanosecondOfDay(), slot.durationMinutes, index)
            })
        }
        return ChildWriteResult.Success
    }

    @Transaction
    open suspend fun deleteOnce(id: String): Boolean {
        val name = findName(id) ?: return false
        snapshotChildName(id, name)
        return deleteChild(id) == 1
    }
}
