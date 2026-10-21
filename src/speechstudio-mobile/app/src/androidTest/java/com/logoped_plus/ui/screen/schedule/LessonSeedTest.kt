package com.logoped_plus.ui.screen.schedule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.logoped_plus.data.local.ChildrenDatabase
import com.logoped_plus.data.local.createChildrenDatabase
import com.logoped_plus.data.local.toDomain
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.VideoAttachment
import com.logoped_plus.domain.repository.LessonWriteResult
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

/** Сиды демо-данных в application-БД для адресных ручных проверок расписания. */
@RunWith(AndroidJUnit4::class)
class LessonSeedTest {

    @Test
    fun seed() = runBlocking {
        val db = createChildrenDatabase(ApplicationProvider.getApplicationContext<Context>())
        try {
            db.childDao().addOnce("seed-child-1", "Анна Смирнова")
            db.childDao().addOnce("seed-child-2", "Богдан Орлов")
            val today = LocalDate.now()
            upsert(db, Lesson("seed-lesson-1", listOf(LessonParticipant("seed-child-1")), today.atTime(9, 0), 40,
                "Тренировка звуков [С], [Ш].",
                listOf(VideoAttachment("content://media/external/video/1"), VideoAttachment("content://media/external/video/2"))))
            upsert(db, Lesson("seed-lesson-2", listOf(LessonParticipant("seed-child-1"), LessonParticipant("seed-child-2")), today.atTime(10, 0), 50,
                "", listOf(VideoAttachment("content://media/external/video/1"))))
            upsert(db, Lesson("seed-lesson-3", listOf(LessonParticipant("seed-child-2")), today.atTime(11, 0), 30, "", emptyList()))
        } finally {
            db.close()
        }
    }

    private suspend fun upsert(db: ChildrenDatabase, lesson: Lesson) {
        db.lessonDao().find(lesson.id)?.let {
            if (it.toDomain() != lesson) db.lessonDao().deleteExisting(lesson.id)
        }
        check(db.lessonDao().addOnce(lesson) is LessonWriteResult.Success)
    }
}
