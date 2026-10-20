package com.logoped_plus.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.logoped_plus.data.local.ChildrenDatabase
import com.logoped_plus.data.local.createChildrenDatabase
import com.logoped_plus.data.local.toDomain
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.VideoAttachment
import com.logoped_plus.domain.repository.ChildLoadState
import com.logoped_plus.domain.repository.LessonLoadState
import com.logoped_plus.domain.repository.LessonWriteResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class RoomLessonRepositoryTest {
    private lateinit var db: ChildrenDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var children: RoomChildRepository
    private lateinit var repo: RoomLessonRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-lesson-delete.db"
        context.deleteDatabase(name)
        db = createChildrenDatabase(context, name)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        children = RoomChildRepository(db.childDao(), scope)
        repo = RoomLessonRepository(db.lessonDao(), children, scope)
        runBlocking {
            withTimeout(5_000) {
                while (repo.state.value !is LessonLoadState.Ready || children.state.value !is ChildLoadState.Ready)
                    delay(50)
            }
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Test
    fun deleteRemovesWholeAggregateAndKeepsNeighbors() = runBlocking {
        db.childDao().addOnce("a", "А")
        db.childDao().addOnce("b", "Б")
        val removed = lesson("removed", listOf("a", "b"), listOf("content://v1", "content://v2"))
        val neighbor = lesson("neighbor", listOf("b"), listOf("content://v1"))
        check(db.lessonDao().addOnce(removed) is LessonWriteResult.Success)
        check(db.lessonDao().addOnce(neighbor) is LessonWriteResult.Success)

        val result = repo.deleteLesson("removed")
        assertEquals(LessonWriteResult.Success(removed), result)

        assertNull(db.lessonDao().find("removed"))
        assertEquals(0, count("SELECT COUNT(*) FROM lesson_participants WHERE lessonId = 'removed'"))
        assertEquals(0, count("SELECT COUNT(*) FROM lesson_videos WHERE lessonId = 'removed'"))
        val kept = checkNotNull(db.lessonDao().find("neighbor"))
        assertEquals(neighbor, kept.toDomain())
        assertEquals(1, count("SELECT COUNT(*) FROM lesson_participants"))
        assertEquals(1, count("SELECT COUNT(*) FROM lesson_videos"))
    }

    @Test
    fun deleteMissingIdReturnsNotFoundAndKeepsRows() = runBlocking {
        db.childDao().addOnce("a", "А")
        val existing = lesson("existing", listOf("a"), listOf("content://v1"))
        check(db.lessonDao().addOnce(existing) is LessonWriteResult.Success)

        val result = repo.deleteLesson("absent")
        assertEquals(LessonWriteResult.Failure(LessonWriteResult.Reason.NotFound), result)

        val kept = checkNotNull(db.lessonDao().find("existing"))
        assertEquals(existing, kept.toDomain())
    }

    private fun lesson(id: String, childIds: List<String>, uris: List<String>): Lesson =
        Lesson(id, childIds.map(::LessonParticipant), LocalDateTime.of(2026, 9, 1, 9, 0), 40, "комментарий $id", uris.map(::VideoAttachment))

    private fun count(query: String): Int = db.openHelper.readableDatabase.query(query).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }
}
