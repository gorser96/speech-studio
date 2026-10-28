package com.speech_studio.data.repository

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import androidx.room.withTransaction
import com.speech_studio.data.local.ChildrenDatabase
import com.speech_studio.data.local.createChildrenDatabase
import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.model.LessonParticipant
import com.speech_studio.domain.model.VideoAttachment
import com.speech_studio.domain.repository.LessonWriteResult
import kotlinx.coroutines.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Debug-only shell probe; never opens the application database or the children probe. */
class LessonTransactionProbeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var operation: Job? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra("action") == "kill") {
            if (intent.getIntExtra("pid", -1) == Process.myPid()) Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        val run = intent?.getStringExtra("run") ?: return START_NOT_STICKY
        if (!run.matches(Regex("[a-f0-9]{32}")) || operation?.isActive == true) return START_NOT_STICKY
        val action = intent.getStringExtra("action") ?: "write"
        val mode = intent.getStringExtra("mode") ?: "BEFORE_COMMIT"
        val update = intent.getBooleanExtra("update", false)
        if (action !in listOf("write", "inspect", "rollback") || mode !in listOf("BEFORE_COMMIT", "AFTER_COMMIT")) return START_NOT_STICKY
        operation = scope.launch {
            val name = "lesson-probe-$run.db"
            val path = getDatabasePath(name).canonicalFile
            check(path.parentFile == getDatabasePath("lesson-probe.db").canonicalFile.parentFile)
            val db = createChildrenDatabase(this@LessonTransactionProbeService, name)
            try {
                if (action == "inspect") {
                    report(run, JSONObject().put("status", "INSPECTED").put("snapshot", snapshot(db)))
                } else {
                    db.childDao().addOnce("a", "Саша"); db.childDao().addOnce("b", "Саша")
                    val old = Lesson("subject", listOf(LessonParticipant("a")), LocalDateTime(2026, 9, 11, 23, 30, 1, 123456789), 90, "  старое\n ", listOf(VideoAttachment("content://old")))
                    val control = old.copy(id = "control", participants = listOf(LessonParticipant("b")), comment = "Контроль")
                    check(db.lessonDao().addOnce(control) is LessonWriteResult.Success)
                    if (update) check(db.lessonDao().addOnce(old) is LessonWriteResult.Success)
                    val before = snapshot(db)
                    val changed = old.copy(scheduledAt = old.scheduledAt.date.plus(2, DateTimeUnit.DAY).atTime(old.scheduledAt.time), durationMinutes = 55,
                        participants = listOf(LessonParticipant("b"), LessonParticipant("a")), comment = "  новое\n  ",
                        videoAttachments = listOf(VideoAttachment("content://new/z"), VideoAttachment("content://new/a")))
                    if (action == "rollback") {
                        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_video BEFORE INSERT ON lesson_videos WHEN NEW.uri='content://new/z' BEGIN SELECT RAISE(ABORT, 'probe'); END")
                        var failed = false
                        try {
                            if (update) db.lessonDao().updateExisting(changed) else db.lessonDao().addOnce(changed)
                        } catch (_: android.database.sqlite.SQLiteException) { failed = true }
                        check(failed)
                        val after = snapshot(db)
                        check(before.toString() == after.toString())
                        report(run, JSONObject().put("status", "ROLLED_BACK").put("snapshot", after).put("before", before))
                    } else {
                        var after: JSONObject? = null
                        db.withTransaction {
                            val result = if (update) db.lessonDao().updateExisting(changed) else db.lessonDao().addOnce(changed)
                            check(result == LessonWriteResult.Success(changed))
                            after = snapshot(db)
                            if (mode == "BEFORE_COMMIT") {
                                report(run, JSONObject().put("status", mode).put("before", before).put("after", after))
                                awaitCancellation()
                            }
                        }
                        report(run, JSONObject().put("status", "AFTER_COMMIT").put("before", before).put("after", after))
                        awaitCancellation()
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { report(run, JSONObject().put("status", "ERROR").put("error", error.javaClass.simpleName)) }
            finally { db.close() }
        }
        return START_NOT_STICKY
    }

    private suspend fun snapshot(db: ChildrenDatabase): JSONObject = db.withTransaction {
        val children = JSONArray()
        db.openHelper.readableDatabase.query("SELECT position,id,name FROM children ORDER BY position").use { rows ->
            while (rows.moveToNext()) children.put(JSONObject().put("position", rows.getLong(0)).put("id", rows.getString(1)).put("name", rows.getString(2)))
        }
        val ids = mutableListOf<String>()
        db.openHelper.readableDatabase.query("SELECT id FROM lessons ORDER BY position").use { rows -> while (rows.moveToNext()) ids += rows.getString(0) }
        val lessons = JSONArray()
        ids.forEach { id ->
            val row = checkNotNull(db.lessonDao().find(id))
            val participants = JSONArray(); row.participants.sortedBy { it.ordinal }.forEach { participants.put(JSONObject().put("id", it.childId).put("ordinal", it.ordinal)) }
            val videos = JSONArray(); row.videos.sortedBy { it.ordinal }.forEach { videos.put(JSONObject().put("uri", it.uri).put("ordinal", it.ordinal)) }
            lessons.put(JSONObject().put("id", id).put("position", row.lesson.position)
                .put("epochDay", row.lesson.scheduledEpochDay).put("nanoOfDay", row.lesson.scheduledNanoOfDay)
                .put("duration", row.lesson.durationMinutes).put("comment", row.lesson.comment)
                .put("participants", participants).put("videos", videos))
        }
        JSONObject().put("children", children).put("lessons", lessons)
    }

    private fun report(run: String, result: JSONObject) {
        result.put("run", run).put("pid", Process.myPid())
        val root = filesDir.canonicalFile
        val target = File(root, "lesson-probe-$run.json").canonicalFile
        val temporary = File(root, "lesson-probe-$run.tmp").canonicalFile
        check(target.parentFile == root && temporary.parentFile == root)
        temporary.writeText(result.toString())
        check(temporary.renameTo(target))
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
