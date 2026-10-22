package com.logoped_plus.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        ChildEntity::class, LessonEntity::class, LessonParticipantEntity::class, LessonVideoEntity::class,
        TagEntity::class, ChildTagEntity::class, ChildScheduleEntity::class, ChildScheduleSlotEntity::class,
        ShiftTemplateEntity::class, WorkShiftEntity::class, ShiftSlotEntity::class
    ], version = 5, exportSchema = true
)
@ConstructedBy(ChildrenDatabaseConstructor::class)
abstract class ChildrenDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun lessonDao(): LessonDao
    abstract fun tagDao(): TagDao
    abstract fun shiftDao(): ShiftDao
}

// Room generates the actual implementations for Android and iOS.
@Suppress("KotlinNoActualForExpect")
expect object ChildrenDatabaseConstructor : RoomDatabaseConstructor<ChildrenDatabase> {
    override fun initialize(): ChildrenDatabase
}
