package com.logoped_plus.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.logoped_plus.domain.model.Child
import com.logoped_plus.domain.model.ChildSchedule
import com.logoped_plus.domain.model.GenerationCounts
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.model.Tag
import com.logoped_plus.domain.model.TagColorKey
import com.logoped_plus.domain.model.WeeklyLessonSlot
import com.logoped_plus.domain.repository.ChildWriteResult
import com.logoped_plus.domain.repository.GenerateChildLessonsResult
import com.logoped_plus.domain.repository.LessonWriteResult
import com.logoped_plus.domain.repository.TagWriteResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalDatabaseIntegrationTest {
    private lateinit var context: Context
    private val databases = mutableListOf<ChildrenDatabase>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        databases.forEach { it.close() }
        databases.clear()
    }

    private fun open(name: String): ChildrenDatabase {
        context.deleteDatabase(name)
        val db = createChildrenDatabase(context, name)
        databases.add(db)
        return db
    }

    private fun openExisting(name: String): ChildrenDatabase {
        val db = createChildrenDatabase(context, name)
        databases.add(db)
        return db
    }

    private suspend fun children(db: ChildrenDatabase) = db.childDao().observeChildren().first()
    private suspend fun tags(db: ChildrenDatabase) = db.tagDao().observeTags().first()
    private suspend fun card(db: ChildrenDatabase, id: String) =
        db.childDao().observeCard(id).first().firstOrNull()?.toDomain()
    private suspend fun lessons(db: ChildrenDatabase) = db.lessonDao().observeLessons().first()

    @Test
    fun migrationToLatestPreservesChildren() = runBlocking {
        val name = "itest-migration-1-4.db"
        context.deleteDatabase(name)
        val raw = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            V1_DDL.forEach { raw.execSQL(it) }
            raw.execSQL("PRAGMA user_version = 1")
            raw.execSQL("INSERT INTO children (id, name) VALUES ('c1', 'Анна')")
            raw.execSQL("INSERT INTO children (id, name) VALUES ('c2', 'Борис')")
        } finally {
            raw.close()
        }

        val db = openExisting(name)
        assertEquals(5, db.openHelper.readableDatabase.version)
        assertEquals(listOf("c1", "c2"), children(db).map { it.id })
        assertEquals(listOf("Анна", "Борис"), children(db).map { it.name })
        assertTrue(lessons(db).isEmpty())
        assertTrue(tags(db).isEmpty())
    }

    @Test
    fun missingMigrationFailsWithoutWipingData() {
        val name = "itest-no-destructive-fallback.db"
        context.deleteDatabase(name)
        val raw = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            V1_DDL.forEach { raw.execSQL(it) }
            raw.execSQL("PRAGMA user_version = 6")
            raw.execSQL("INSERT INTO children (id, name) VALUES ('c1', 'Анна')")
        } finally {
            raw.close()
        }

        val db = openExisting(name)
        assertThrows(IllegalStateException::class.java) {
            db.openHelper.writableDatabase
        }
        db.close()

        val check = SQLiteDatabase.openDatabase(
            context.getDatabasePath(name).path,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            check.rawQuery("SELECT id, name FROM children", null).use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("c1", rows.getString(0))
                assertEquals("Анна", rows.getString(1))
                assertFalse(rows.moveToNext())
            }
        } finally {
            check.close()
        }
    }

    @Test
    fun reopeningDatabaseKeepsData() = runBlocking {
        val name = "itest-reopen.db"
        val first = open(name)
        assertTrue(first.childDao().addOnce("c1", "Анна"))
        first.close()
        databases.remove(first)

        val second = openExisting(name)
        assertEquals(5, second.openHelper.readableDatabase.version)
        assertEquals(listOf("c1"), children(second).map { it.id })
    }

    @Test
    fun migration3ToLatestPreservesData() = runBlocking {
        val name = "itest-migration.db"
        context.deleteDatabase(name)
        val raw = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            V3_DDL.forEach { raw.execSQL(it) }
            raw.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            raw.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '2aff2df951e96f2f56c716e461ed7497')")
            raw.execSQL("PRAGMA user_version = 3")
            val epochDay = LocalDate(2026, 9, 1).toEpochDays()
            val nano = LocalTime(9, 0).toNanosecondOfDay()
            raw.execSQL("INSERT INTO children (id, name) VALUES ('c1', 'Анна')")
            raw.execSQL("INSERT INTO children (id, name) VALUES ('c2', 'Борис')")
            raw.execSQL("INSERT INTO lessons (id, scheduledEpochDay, scheduledNanoOfDay, durationMinutes, comment) VALUES ('l1', $epochDay, $nano, 40, 'комментарий')")
            raw.execSQL("INSERT INTO lesson_participants (lessonId, childId, childName, ordinal) VALUES ('l1', 'c1', NULL, 0)")
            raw.execSQL("INSERT INTO lesson_participants (lessonId, childId, childName, ordinal) VALUES ('l1', 'gone', 'Иван', 1)")
            raw.execSQL("INSERT INTO lesson_videos (lessonId, uri, ordinal) VALUES ('l1', 'content://v1', 0)")
        } finally {
            raw.close()
        }

        val db = openExisting(name)
        assertEquals(5, db.openHelper.readableDatabase.version)
        val rows = children(db)
        assertEquals(listOf("c1", "c2"), rows.map { it.id })
        assertEquals(listOf("Анна", "Борис"), rows.map { it.name })
        assertEquals(listOf(1L, 2L), rows.map { it.position })
        val lesson = db.lessonDao().find("l1")!!
        assertEquals("комментарий", lesson.lesson.comment)
        assertEquals(listOf("c1", "gone"), lesson.participants.sortedBy { it.ordinal }.map { it.childId })
        assertNull(lesson.participants.first { it.childId == "c1" }.childName)
        assertEquals("Иван", lesson.participants.first { it.childId == "gone" }.childName)
        assertEquals("content://v1", lesson.videos.single().uri)
        assertTrue(tags(db).isEmpty())
        val migrated = card(db, "c1")!!
        assertTrue(migrated.tagIds.isEmpty())
        assertTrue(migrated.schedule.slots.isEmpty())
        assertNull(migrated.schedule.endDate)
    }

    @Test
    fun migration4To5PreservesDataAndFillsType() = runBlocking {
        val name = "itest-migration-4-5.db"
        context.deleteDatabase(name)
        val raw = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            V4_DDL.forEach { raw.execSQL(it) }
            raw.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            raw.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '7441455d846ab97cdbe9ae33d072969c')")
            raw.execSQL("PRAGMA user_version = 4")
            val epochDay = LocalDate(2026, 9, 1).toEpochDays()
            val nano = LocalTime(9, 0).toNanosecondOfDay()
            raw.execSQL("INSERT INTO children (id, name) VALUES ('c1', 'Анна')")
            raw.execSQL("INSERT INTO children (id, name) VALUES ('c2', 'Борис')")
            raw.execSQL("INSERT INTO tags (id, name, nameKey, colorKey) VALUES ('t1', 'Группа А', 'группа а', 'RED')")
            raw.execSQL("INSERT INTO child_tags (childId, tagId) VALUES ('c1', 't1')")
            raw.execSQL("INSERT INTO child_schedule (childId, endEpochDay) VALUES ('c1', ${LocalDate(2026, 10, 31).toEpochDays()})")
            raw.execSQL("INSERT INTO child_schedule_slots (childId, id, dayOfWeek, startNanoOfDay, durationMinutes, ordinal) VALUES ('c1', 's1', 4, ${LocalTime(16, 0).toNanosecondOfDay()}, 40, 0)")
            raw.execSQL("INSERT INTO lessons (id, scheduledEpochDay, scheduledNanoOfDay, durationMinutes, comment) VALUES ('solo', $epochDay, $nano, 40, 'один')")
            raw.execSQL("INSERT INTO lessons (id, scheduledEpochDay, scheduledNanoOfDay, durationMinutes, comment) VALUES ('pair', $epochDay, $nano, 40, 'двое')")
            raw.execSQL("INSERT INTO lessons (id, scheduledEpochDay, scheduledNanoOfDay, durationMinutes, comment) VALUES ('empty', $epochDay, $nano, 40, '')")
            raw.execSQL("INSERT INTO lesson_participants (lessonId, childId, childName, ordinal) VALUES ('solo', 'c1', NULL, 0)")
            raw.execSQL("INSERT INTO lesson_participants (lessonId, childId, childName, ordinal) VALUES ('pair', 'c1', NULL, 0)")
            raw.execSQL("INSERT INTO lesson_participants (lessonId, childId, childName, ordinal) VALUES ('pair', 'c2', NULL, 1)")
            raw.execSQL("INSERT INTO lesson_videos (lessonId, uri, ordinal) VALUES ('solo', 'content://v1', 0)")
        } finally {
            raw.close()
        }

        val db = openExisting(name)
        assertEquals(5, db.openHelper.readableDatabase.version)
        assertEquals(listOf("c1", "c2"), children(db).map { it.id })
        assertEquals(listOf("t1"), tags(db).map { it.id })
        val solo = db.lessonDao().find("solo")!!
        val pair = db.lessonDao().find("pair")!!
        val empty = db.lessonDao().find("empty")!!
        assertEquals(LessonType.INDIVIDUAL.name, solo.lesson.type)
        assertEquals(LessonType.GROUP.name, pair.lesson.type)
        assertEquals(LessonType.INDIVIDUAL.name, empty.lesson.type)
        assertNull(solo.lesson.shiftId)
        assertEquals("один", solo.lesson.comment)
        assertEquals(listOf("c1"), solo.participants.map { it.childId })
        assertEquals(listOf("c1", "c2"), pair.participants.sortedBy { it.ordinal }.map { it.childId })
        assertEquals("content://v1", solo.videos.single().uri)
        assertEquals(3, lessons(db).size)
        assertTrue(db.shiftDao().observeShifts().first().isEmpty())
        assertTrue(db.shiftDao().observeTemplates().first().isEmpty())
        val migrated = card(db, "c1")!!
        assertEquals(listOf("t1"), migrated.tagIds)
        assertEquals(listOf("s1"), migrated.schedule.slots.map { it.id })
        assertEquals(LocalDate(2026, 10, 31).toEpochDays(), migrated.schedule.endDate!!.toEpochDays())
    }

    @Test
    fun lessonTypeIsStoredAndNotDerivedFromParticipants() = runBlocking {
        val db = open("itest-lesson-type.db")
        assertTrue(db.childDao().addOnce("c1", "Анна"))
        assertTrue(db.childDao().addOnce("c2", "Борис"))
        check(db.lessonDao().addOnce(
            Lesson(
                "group", listOf(LessonParticipant("c1"), LessonParticipant("c2")),
                LocalDateTime(2026, 9, 1, 9, 0), 40, type = LessonType.GROUP
            )
        ) is LessonWriteResult.Success)
        assertEquals(LessonType.GROUP, db.lessonDao().find("group")!!.toDomain().type)

        val single = db.lessonDao().find("group")!!.toDomain().copy(participants = listOf(LessonParticipant("c1")))
        check(db.lessonDao().updateExisting(single) is LessonWriteResult.Success)

        val stored = db.lessonDao().find("group")!!.toDomain()
        assertEquals(LessonType.GROUP, stored.type)
        assertEquals(listOf("c1"), stored.participants.map { it.childId })
    }

    @Test
    fun cardSaveReplacesTagsAndScheduleAtomically() = runBlocking {
        val db = open("itest-card.db")
        val dao = db.childDao()
        val tagDao = db.tagDao()
        assertTrue(dao.addOnce("c1", "Анна"))
        assertTrue(dao.addOnce("c2", "Борис"))
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t1", "Группа А", TagColorKey.RED)))
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t2", "Бис", TagColorKey.BLUE)))
        val end = LocalDate(2026, 10, 31)
        val first = ChildSchedule(
            slots = listOf(
                WeeklyLessonSlot("s1", 4, LocalTime(16, 0), 40),
                WeeklyLessonSlot("s2", 5, LocalTime(15, 0), 30)
            ), endDate = end
        )
        assertEquals(ChildWriteResult.Success, dao.saveCardOnce(Child("c1", "Анна", listOf("t1", "t2"), first)))
        var saved = card(db, "c1")!!
        assertEquals(listOf("t1", "t2"), saved.tagIds.sorted())
        assertEquals(end.toEpochDays(), saved.schedule.endDate!!.toEpochDays())
        assertEquals(listOf("s1", "s2"), saved.schedule.slots.map { it.id })

        val second = ChildSchedule(
            slots = listOf(
                WeeklyLessonSlot("s1", 4, LocalTime(17, 0), 40),
                WeeklyLessonSlot("s2", 5, LocalTime(15, 0), 30),
                WeeklyLessonSlot("s3", 4, LocalTime(10, 0), 20)
            ), endDate = end
        )
        assertEquals(ChildWriteResult.Success, dao.saveCardOnce(Child("c1", "Алина", listOf("t2"), second)))
        saved = card(db, "c1")!!
        assertEquals("Алина", saved.name)
        assertEquals(listOf("t2"), saved.tagIds)
        assertEquals(
            listOf("s1" to LocalTime(17, 0), "s2" to LocalTime(15, 0), "s3" to LocalTime(10, 0)),
            saved.schedule.slots.map { it.id to it.startTime }
        )

        assertEquals(ChildWriteResult.Success, dao.saveCardOnce(Child("c1", "Алина", listOf("t2", "ghost"), second)))
        assertEquals(listOf("t2"), card(db, "c1")!!.tagIds)
        assertEquals(ChildWriteResult.Failure(ChildWriteResult.Reason.NotFound), dao.saveCardOnce(Child("nope", "Нет", emptyList(), ChildSchedule())))

        assertEquals(TagWriteResult.Success, tagDao.deleteOnce("t2"))
        assertEquals(emptyList<String>(), card(db, "c1")!!.tagIds)
        assertEquals(1, tags(db).size)
        assertEquals(2, children(db).size)

        check(db.lessonDao().addOnce(
            Lesson("l1", listOf(LessonParticipant("c1")),
                LocalDateTime(2026, 9, 1, 9, 0), 40, "", emptyList())
        ) is LessonWriteResult.Success)
        assertTrue(dao.deleteOnce("c1"))
        assertNull(card(db, "c1"))
        assertEquals(1, children(db).size)
        assertEquals(1, lessons(db).size)
        assertEquals("Алина", db.lessonDao().find("l1")!!.participants.single().childName)
        assertEquals(1, tags(db).size)
    }

    @Test
    fun cardFlowReEmitsWhenTagsChange() = runBlocking {
        val db = open("itest-reemit.db")
        val dao = db.childDao()
        val tagDao = db.tagDao()
        assertTrue(dao.addOnce("c1", "Анна"))
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t1", "Группа А", TagColorKey.RED)))

        val emissions = Channel<List<String>>(Channel.UNLIMITED)
        val job = launch {
            dao.observeCard("c1").collect { rows ->
                emissions.trySend(rows.firstOrNull()?.tags?.map { it.tagId } ?: emptyList())
            }
        }
        assertEquals(emptyList<String>(), withTimeout(5000) { emissions.receive() })

        assertEquals(
            ChildWriteResult.Success,
            dao.saveCardOnce(Child("c1", "Анна", listOf("t1"), ChildSchedule()))
        )
        var seen = false
        withTimeout(5000) {
            while (!seen) {
                if (emissions.receive() == listOf("t1")) seen = true
            }
        }
        assertTrue(seen)
        job.cancel()
    }

    @Test
    fun tagReorderChangesOrderAndRejectsStaleIds() = runBlocking {
        val db = open("itest-tag-reorder.db")
        val tagDao = db.tagDao()
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t1", "Альфа", TagColorKey.RED)))
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t2", "Бета", TagColorKey.BLUE)))
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t3", "Гамма", TagColorKey.GREEN)))
        assertEquals(listOf("t1", "t2", "t3"), tags(db).map { it.id })

        assertEquals(TagWriteResult.Success, tagDao.reorderOnce(listOf("t3", "t1", "t2")))
        assertEquals(listOf("t3", "t1", "t2"), tags(db).map { it.id })

        assertEquals(TagWriteResult.Success, tagDao.reorderOnce(listOf("t1", "t2", "t3")))
        assertEquals(listOf("t1", "t2", "t3"), tags(db).map { it.id })

        assertEquals(TagWriteResult.Failure(TagWriteResult.Reason.NotFound), tagDao.reorderOnce(listOf("t1", "t3")))
        assertEquals(TagWriteResult.Failure(TagWriteResult.Reason.NotFound), tagDao.reorderOnce(listOf("t1", "t2", "t4")))
        assertEquals(TagWriteResult.Success, tagDao.reorderOnce(emptyList()))
        assertEquals(listOf("t1", "t2", "t3"), tags(db).map { it.id })
    }

    @Test
    fun childrenWithTagsExposeAssignedTags() = runBlocking {
        val db = open("itest-list-tags.db")
        val dao = db.childDao()
        val tagDao = db.tagDao()
        assertTrue(dao.addOnce("c1", "Анна"))
        assertTrue(dao.addOnce("c2", "Борис"))
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t1", "Группа А", TagColorKey.RED)))
        assertEquals(TagWriteResult.Success, tagDao.saveOnce(Tag("t2", "Бис", TagColorKey.BLUE)))
        assertEquals(ChildWriteResult.Success, dao.saveCardOnce(Child("c1", "Анна", listOf("t1", "t2"), ChildSchedule())))
        assertEquals(ChildWriteResult.Success, dao.saveCardOnce(Child("c2", "Борис", listOf("t1"), ChildSchedule())))

        val rows = dao.observeChildrenWithTags().first()
        assertEquals(listOf("c1", "c2"), rows.map { it.child.id })
        assertEquals(listOf(listOf("t1", "t2"), listOf("t1")), rows.map { it.toDomain().tagIds.sorted() })
    }

    @Test
    fun generationRepeatKeepsOldLessonsAndRollsBackOnFailure() = runBlocking {
        val db = open("itest-gen.db")
        val dao = db.childDao()
        dao.addOnce("c1", "Анна")
        dao.addOnce("c2", "Борис")
        check(db.lessonDao().addOnce(
            Lesson("old", listOf(LessonParticipant("c1")),
                LocalDateTime(2026, 9, 1, 9, 0), 40, "старое", emptyList())
        ) is LessonWriteResult.Success)
        check(db.lessonDao().addOnce(
            Lesson("busy", listOf(LessonParticipant("c2")),
                LocalDateTime(2026, 10, 8, 16, 20), 30, "", emptyList())
        ) is LessonWriteResult.Success)
        val schedule = ChildSchedule(
            slots = listOf(WeeklyLessonSlot("s1", 4, LocalTime(16, 0), 40)),
            endDate = LocalDate(2026, 10, 15)
        )
        assertEquals(ChildWriteResult.Success, dao.saveCardOnce(Child("c1", "Анна", emptyList(), schedule)))
        val now = LocalDateTime(2026, 10, 5, 9, 0)

        val first = db.lessonDao().generateForChild("c1", now)
        assertEquals(GenerateChildLessonsResult.Success(GenerationCounts(2, 0)), first)
        assertEquals(4, lessons(db).size)
        val generated = lessons(db).filter { it.lesson.id != "old" && it.lesson.id != "busy" }
        assertEquals(listOf(LocalDate(2026, 10, 8), LocalDate(2026, 10, 15)), generated.map { LocalDate.fromEpochDays(it.lesson.scheduledEpochDay) })
        assertEquals(listOf(LocalTime(16, 0), LocalTime(16, 0)).map { it.toNanosecondOfDay() }, generated.map { it.lesson.scheduledNanoOfDay })
        assertEquals(listOf(listOf("c1"), listOf("c1")), generated.map { it.participants.map { p -> p.childId } })

        val second = db.lessonDao().generateForChild("c1", now)
        assertEquals(GenerateChildLessonsResult.Success(GenerationCounts(0, 2)), second)
        assertEquals(4, lessons(db).size)
        assertEquals("старое", db.lessonDao().find("old")!!.lesson.comment)

        val scheduleTwo = ChildSchedule(
            slots = listOf(
                WeeklyLessonSlot("a", 4, LocalTime(10, 0), 40),
                WeeklyLessonSlot("b", 5, LocalTime(10, 0), 40)
            ), endDate = LocalDate(2026, 10, 16)
        )
        assertEquals(ChildWriteResult.Success, dao.saveCardOnce(Child("c1", "Анна", emptyList(), scheduleTwo)))
        db.openHelper.writableDatabase.execSQL("CREATE TABLE probe_count (n INTEGER)")
        db.openHelper.writableDatabase.execSQL("INSERT INTO probe_count VALUES (0)")
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_gen BEFORE INSERT ON lessons " +
                "BEGIN UPDATE probe_count SET n = n + 1; SELECT RAISE(ABORT, 'probe') WHERE (SELECT n FROM probe_count) >= 2; END"
        )
        var failed = false
        try {
            db.lessonDao().generateForChild("c1", now)
        } catch (_: android.database.sqlite.SQLiteException) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(4, lessons(db).size)
        val morningNanos = LocalTime(10, 0).toNanosecondOfDay()
        val rollbackDays = listOf(LocalDate(2026, 10, 8), LocalDate(2026, 10, 9)).map { it.toEpochDays() }.toSet()
        assertTrue(lessons(db).none { it.lesson.scheduledNanoOfDay == morningNanos && it.lesson.scheduledEpochDay in rollbackDays })
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_gen")
        db.openHelper.writableDatabase.execSQL("DROP TABLE probe_count")

        val third = db.lessonDao().generateForChild("c1", now)
        assertEquals(GenerateChildLessonsResult.Success(GenerationCounts(4, 0)), third)
        assertEquals(8, lessons(db).size)
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

        val V4_DDL = V3_DDL + listOf(
            "CREATE TABLE IF NOT EXISTS `tags` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameKey` TEXT NOT NULL, `colorKey` TEXT NOT NULL)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_id` ON `tags` (`id`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_nameKey` ON `tags` (`nameKey`)",
            "CREATE TABLE IF NOT EXISTS `child_tags` (`childId` TEXT NOT NULL, `tagId` TEXT NOT NULL, PRIMARY KEY(`childId`, `tagId`), FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS `child_schedule` (`childId` TEXT NOT NULL, `endEpochDay` INTEGER NOT NULL, PRIMARY KEY(`childId`), FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS `child_schedule_slots` (`childId` TEXT NOT NULL, `id` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startNanoOfDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`childId`, `id`), FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`childId`) REFERENCES `child_schedule`(`childId`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_child_schedule_slots_childId_ordinal` ON `child_schedule_slots` (`childId`, `ordinal`)"
        )
    }
}
