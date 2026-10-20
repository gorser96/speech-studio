package com.logoped_plus.data.local

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.VideoAttachment
import com.logoped_plus.domain.repository.LessonWriteResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Проверяет на iOS bundled SQLite: переоткрытие файла БД и миграции 1→4 и 3→4.
 */
class IosDatabasePersistenceTest {

    @Test
    fun reopeningDatabaseKeepsChildLessonAndVideo() = runTest {
        val path = tempDatabasePath()
        try {
            val first = createChildrenDatabase(path)
            try {
                assertTrue(first.childDao().addOnce("c1", "Анна"))
                val lesson = Lesson(
                    id = "l1",
                    participants = listOf(LessonParticipant("c1")),
                    scheduledAt = LocalDateTime(2026, 10, 20, 9, 0),
                    durationMinutes = 40,
                    comment = "Комментарий",
                    videoAttachments = listOf(VideoAttachment("file:///tmp/test.mov"))
                )
                assertEquals(LessonWriteResult.Success(lesson), first.lessonDao().addOnce(lesson))
            } finally {
                first.close()
            }

            val reopened = createChildrenDatabase(path)
            try {
                assertEquals("Анна", reopened.childDao().find("c1")?.name)
                val restored = reopened.lessonDao().find("l1")!!.toDomain()
                assertEquals(LessonParticipant("c1"), restored.participants.single())
                assertEquals(LocalDateTime(2026, 10, 20, 9, 0), restored.scheduledAt)
                assertEquals(40, restored.durationMinutes)
                assertEquals("Комментарий", restored.comment)
                assertEquals("file:///tmp/test.mov", restored.videoAttachments.single().uri)
            } finally {
                reopened.close()
            }
        } finally {
            deleteDatabaseFiles(path)
        }
    }

    @Test
    fun migration1To4PreservesChildrenAndCreatesTables() = runTest {
        val path = tempDatabasePath()
        try {
            writeRawDatabase(
                path = path,
                version = 1,
                statements = V1_DDL + listOf(
                    "INSERT INTO children (id, name) VALUES ('c1', 'Анна')",
                    "INSERT INTO children (id, name) VALUES ('c2', 'Борис')"
                )
            )

            val database = createChildrenDatabase(path)
            try {
                assertEquals(
                    listOf("c1", "c2"),
                    database.childDao().observeChildren().first().map { it.id }
                )
                assertTrue(database.lessonDao().observeLessons().first().isEmpty())
                assertTrue(database.tagDao().observeTags().first().isEmpty())
                assertTrue(database.childDao().addOnce("c3", "Вера"))
            } finally {
                database.close()
            }
        } finally {
            deleteDatabaseFiles(path)
        }
    }

    @Test
    fun migration3To4PreservesLessonParticipantsAndVideos() = runTest {
        val path = tempDatabasePath()
        try {
            writeRawDatabase(
                path = path,
                version = 3,
                statements = V3_DDL + listOf(
                    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
                    "INSERT INTO room_master_table (id, identity_hash) VALUES (42, '2aff2df951e96f2f56c716e461ed7497')",
                    "INSERT INTO children (id, name) VALUES ('c1', 'Анна')",
                    "INSERT INTO children (id, name) VALUES ('c2', 'Борис')",
                    "INSERT INTO lessons (id, scheduledEpochDay, scheduledNanoOfDay, durationMinutes, comment) VALUES ('l1', 20746, 32400000000000, 40, 'Комментарий')",
                    "INSERT INTO lesson_participants (lessonId, childId, childName, ordinal) VALUES ('l1', 'c1', NULL, 0)",
                    "INSERT INTO lesson_participants (lessonId, childId, childName, ordinal) VALUES ('l1', 'gone', 'Иван', 1)",
                    "INSERT INTO lesson_videos (lessonId, uri, ordinal) VALUES ('l1', 'file:///tmp/old.mov', 0)"
                )
            )

            val database = createChildrenDatabase(path)
            try {
                val lesson = database.lessonDao().find("l1")!!
                assertEquals("Комментарий", lesson.lesson.comment)
                assertEquals(listOf("c1", "gone"), lesson.participants.sortedBy { it.ordinal }.map { it.childId })
                assertNull(lesson.participants.first { it.childId == "c1" }.childName)
                assertEquals("Иван", lesson.participants.first { it.childId == "gone" }.childName)
                assertEquals("file:///tmp/old.mov", lesson.videos.single().uri)
                assertTrue(database.tagDao().observeTags().first().isEmpty())
                val card = database.childDao().observeCard("c1").first().single().toDomain()
                assertTrue(card.tagIds.isEmpty())
                assertTrue(card.schedule.slots.isEmpty())
                assertNull(card.schedule.endDate)
            } finally {
                database.close()
            }
        } finally {
            deleteDatabaseFiles(path)
        }
    }

    private fun tempDatabasePath(): String =
        NSTemporaryDirectory() + "logoped-plus-${NSUUID().UUIDString}.db"

    private fun writeRawDatabase(path: String, version: Int, statements: List<String>) {
        val connection = BundledSQLiteDriver().open(path)
        try {
            statements.forEach { connection.execSQL(it) }
            connection.execSQL("PRAGMA user_version = $version")
        } finally {
            connection.close()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun deleteDatabaseFiles(path: String) {
        listOf(path, "$path-wal", "$path-shm", "$path-journal").forEach {
            NSFileManager.defaultManager.removeItemAtPath(it, error = null)
        }
    }

    private companion object {
        val V1_DDL = listOf(
            "CREATE TABLE IF NOT EXISTS `children` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `name` TEXT NOT NULL)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_children_id` ON `children` (`id`)"
        )

        val V3_DDL = listOf(
            "CREATE TABLE IF NOT EXISTS `children` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `name` TEXT NOT NULL)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_children_id` ON `children` (`id`)",
            "CREATE TABLE IF NOT EXISTS `lessons` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `scheduledEpochDay` INTEGER NOT NULL, `scheduledNanoOfDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `comment` TEXT NOT NULL)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_lessons_id` ON `lessons` (`id`)",
            "CREATE TABLE IF NOT EXISTS `lesson_participants` (`lessonId` TEXT NOT NULL, `childId` TEXT NOT NULL, `childName` TEXT, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`lessonId`, `childId`), FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_participants_lessonId_ordinal` ON `lesson_participants` (`lessonId`, `ordinal`)",
            "CREATE INDEX IF NOT EXISTS `index_lesson_participants_childId` ON `lesson_participants` (`childId`)",
            "CREATE TABLE IF NOT EXISTS `lesson_videos` (`lessonId` TEXT NOT NULL, `uri` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`lessonId`, `uri`), FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_videos_lessonId_ordinal` ON `lesson_videos` (`lessonId`, `ordinal`)"
        )
    }
}
