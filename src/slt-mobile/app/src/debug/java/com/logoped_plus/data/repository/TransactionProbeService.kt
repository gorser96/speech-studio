package com.logoped_plus.data.repository

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import androidx.room.withTransaction
import com.logoped_plus.data.local.createChildrenDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Debug-only, shell-permission-protected probe. Never opens children.db. */
class TransactionProbeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var operation: Job? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra("action") == "kill") {
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        val run = intent?.getStringExtra("run") ?: return START_NOT_STICKY
        if (!run.matches(Regex("[a-f0-9]{32}")) || operation?.isActive == true) return START_NOT_STICKY
        val mode = intent.getStringExtra("mode") ?: "BEFORE_COMMIT"
        val rename = intent.getBooleanExtra("rename", false)
        val action = intent.getStringExtra("action") ?: "write"
        operation = scope.launch {
            val db = createChildrenDatabase(this@TransactionProbeService, "probe-$run.db")
            try {
                val dao = db.childDao()
                if (action == "inspect") {
                    val rows = dao.observeChildren().first()
                    val jsonRows = JSONArray()
                    rows.forEach { jsonRows.put(JSONObject().put("id", it.id).put("name", it.name).put("position", it.position)) }
                    report(run, JSONObject().put("status", "INSPECTED").put("rows", jsonRows))
                } else {
                    dao.addOnce("control", "Контроль")
                    if (rename) dao.addOnce("subject", "Старое")
                    db.withTransaction {
                        if (rename) dao.rename("subject", "Новое") else dao.addOnce("subject", "Новое")
                        if (mode == "BEFORE_COMMIT") {
                            report(run, JSONObject().put("status", mode))
                            awaitCancellation()
                        }
                    }
                    report(run, JSONObject().put("status", "AFTER_COMMIT"))
                    awaitCancellation()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                report(run, JSONObject().put("status", "ERROR").put("error", error.javaClass.simpleName))
            } finally {
                db.close()
            }
        }
        return START_NOT_STICKY
    }

    private fun report(run: String, result: JSONObject) {
        result.put("run", run).put("pid", Process.myPid())
        val target = File(filesDir, "probe-$run.json")
        val temporary = File(filesDir, "probe-$run.tmp")
        temporary.writeText(result.toString())
        check(temporary.renameTo(target))
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
