package com.hivid.app.util

import android.content.Context
import android.util.Log
import com.hivid.app.data.local.HividDatabase
import com.hivid.app.data.local.LogDao
import com.hivid.app.data.local.LogEntity
import com.hivid.app.domain.model.LogLevel
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicInteger

object AppLogger {

    private const val DEFAULT_TAG = "Hivid"
    const val MAX_LOG_ENTRIES = 2000

    private var logDao: LogDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val counterSinceLastPrune = AtomicInteger(0)

    @Volatile
    private var activeDiagnosticId: String? = null

    fun init(context: Context) {
        logDao = HividDatabase.getDatabase(context.applicationContext).logDao()
        i(DEFAULT_TAG, "Logging system initialized (Retention limit: $MAX_LOG_ENTRIES entries)")
    }

    fun setDiagnosticId(id: String?) {
        activeDiagnosticId = id
    }

    fun getDiagnosticId(): String? = activeDiagnosticId

    fun i(tag: String, message: String, diagnosticId: String? = activeDiagnosticId) {
        log(LogLevel.INFO, tag, message, null, diagnosticId)
    }

    fun d(tag: String, message: String, diagnosticId: String? = activeDiagnosticId) {
        log(LogLevel.DEBUG, tag, message, null, diagnosticId)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null, diagnosticId: String? = activeDiagnosticId) {
        log(LogLevel.WARNING, tag, message, throwable, diagnosticId)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, diagnosticId: String? = activeDiagnosticId) {
        log(LogLevel.ERROR, tag, message, throwable, diagnosticId)
    }

    private fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        diagnosticId: String?
    ) {
        // 1. Android Logcat output
        when (level) {
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.WARNING -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }

        // 2. Persistent Room insertion
        val exceptionMessage = throwable?.localizedMessage ?: throwable?.message
        val stackTrace = throwable?.let { t ->
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entity = LogEntity(
            level = level.name,
            tag = tag,
            message = message,
            exceptionMessage = exceptionMessage,
            stackTrace = stackTrace,
            diagnosticId = diagnosticId
        )

        val dao = logDao ?: return
        scope.launch {
            try {
                dao.insert(entity)
                // Periodically prune logs every 50 inserts
                if (counterSinceLastPrune.incrementAndGet() >= 50) {
                    counterSinceLastPrune.set(0)
                    dao.pruneOldLogs(MAX_LOG_ENTRIES)
                }
            } catch (e: Exception) {
                Log.w(DEFAULT_TAG, "Failed to persist log entry: ${e.message}")
            }
        }
    }

    fun exportLogsToString(logs: List<LogEntity>): String {
        return buildString {
            appendLine("========================================================")
            appendLine("             HIVID APPLICATION LOGS DUMP                ")
            appendLine("========================================================")
            appendLine("Total Entries: ${logs.size}")
            appendLine("Generated At : ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine("========================================================")
            appendLine()
            logs.reversed().forEach { log ->
                appendLine(log.toDisplayString())
                appendLine("--------------------------------------------------------")
            }
        }
    }
}
