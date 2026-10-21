package com.logoped_plus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.logoped_plus.domain.model.Tag
import com.logoped_plus.domain.repository.TagWriteResult
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagDao {
    @Query("SELECT * FROM tags ORDER BY position ASC")
    abstract fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY position ASC")
    abstract suspend fun listAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    abstract suspend fun find(id: String): TagEntity?

    @Query("SELECT id FROM tags WHERE nameKey = :nameKey")
    abstract suspend fun findIdsByNameKey(nameKey: String): List<String>

    @Insert
    abstract suspend fun insert(tag: TagEntity)

    @Query("UPDATE tags SET name = :name, nameKey = :nameKey, colorKey = :colorKey WHERE id = :id")
    abstract suspend fun update(id: String, name: String, nameKey: String, colorKey: String): Int

    @Query("UPDATE tags SET position = :position WHERE id = :id")
    abstract suspend fun updatePosition(id: String, position: Long): Int

    @Query("DELETE FROM tags WHERE id = :id")
    abstract suspend fun delete(id: String): Int

    @Query("DELETE FROM child_tags WHERE tagId = :id")
    abstract suspend fun detachAssignments(id: String)

    @Transaction
    open suspend fun saveOnce(tag: Tag): TagWriteResult {
        val name = tag.name.trim()
        if (name.isBlank()) return TagWriteResult.Failure(TagWriteResult.Reason.InvalidName)
        val nameKey = name.lowercase()
        if (findIdsByNameKey(nameKey).any { it != tag.id }) return TagWriteResult.Failure(TagWriteResult.Reason.DuplicateName)
        val existing = find(tag.id)
        if (existing != null) {
            if (existing.name == name && existing.colorKey == tag.color.name) return TagWriteResult.Success
            check(update(tag.id, name, nameKey, tag.color.name) == 1)
        } else {
            insert(tag.copy(name = name).toEntity())
        }
        return TagWriteResult.Success
    }

    @Transaction
    open suspend fun reorderOnce(ids: List<String>): TagWriteResult {
        if (ids.isEmpty()) return TagWriteResult.Success
        val rows = listAll()
        if (rows.size != ids.size || rows.map { it.id }.toSet() != ids.toSet())
            return TagWriteResult.Failure(TagWriteResult.Reason.NotFound)
        // position is the primary key, so a direct swap would collide mid-update:
        // first shift every row into the free upper half, then write the final order.
        val n = rows.size
        rows.forEach { row -> check(updatePosition(row.id, row.position + n) == 1) }
        ids.forEachIndexed { index, id -> check(updatePosition(id, index.toLong()) == 1) }
        return TagWriteResult.Success
    }

    @Transaction
    open suspend fun deleteOnce(id: String): TagWriteResult {
        if (find(id) == null) return TagWriteResult.Failure(TagWriteResult.Reason.NotFound)
        detachAssignments(id)
        check(delete(id) == 1)
        return TagWriteResult.Success
    }
}
