package com.speech_studio.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.speech_studio.data.local.ChildrenDatabase
import com.speech_studio.data.local.createChildrenDatabase
import com.speech_studio.data.local.toDomain
import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.model.LessonParticipant
import com.speech_studio.domain.model.VideoAttachment
import com.speech_studio.domain.repository.ChildLoadState
import com.speech_studio.domain.repository.ChildWriteResult
import com.speech_studio.domain.repository.LessonLoadState
import com.speech_studio.domain.repository.LessonWriteResult
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
class RoomChildRepositoryTest {
    private lateinit var db: ChildrenDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var children: RoomChildRepository
    private lateinit var repo: RoomLessonRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-child-delete.db"
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
    fun deleteChildKeepsLessonsWithSnapshotName() = runBlocking {
        db.childDao().addOnce("a", "А")
        db.childDao().addOnce("b", "Б")
        val single = Lesson("single", listOf(LessonParticipant("a")), LocalDateTime.of(2026, 9, 1, 9, 0), 40, "комментарий", listOf(VideoAttachment("content://v1")))
        val shared = Lesson("shared", listOf(LessonParticipant("a"), LessonParticipant("b")), LocalDateTime.of(2026, 9, 2, 9, 0), 40, "", emptyList())
        check(db.lessonDao().addOnce(single) is LessonWriteResult.Success)
        check(db.lessonDao().addOnce(shared) is LessonWriteResult.Success)

        assertEquals(ChildWriteResult.Success, children.deleteChild(Child("a", "А")))

        assertNull(db.childDao().find("a"))
        assertEquals(single.copy(participants = listOf(LessonParticipant("a", "А"))), checkNotNull(db.lessonDao().find("single")).toDomain())
        assertEquals(shared.copy(participants = listOf(LessonParticipant("a", "А"), LessonParticipant("b", null))), checkNotNull(db.lessonDao().find("shared")).toDomain())
        assertEquals(2, count("SELECT COUNT(*) FROM lessons"))
        assertEquals(3, count("SELECT COUNT(*) FROM lesson_participants"))
        assertEquals(ChildWriteResult.Failure(ChildWriteResult.Reason.NotFound), children.deleteChild(Child("a", "А")))
    }

    private fun count(query: String): Int = db.openHelper.readableDatabase.query(query).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }
}
