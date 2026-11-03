package com.hivid.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hivid.app.domain.model.LogLevel
import java.text.SimpleDateFormat
import java.util.*

@Entity(
    tableName = "app_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["level"]),
        Index(value = ["diagnosticId"])
    ]
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // INFO, DEBUG, WARNING, ERROR
    val tag: String,
    val message: String,
    val exceptionMessage: String? = null,
    val stackTrace: String? = null,
    val diagnosticId: String? = null
) {
    val logLevel: LogLevel
        get() = try {
            LogLevel.valueOf(level)
        } catch (_: Exception) {
            LogLevel.INFO
        }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            return sdf.format(Date(timestamp))
        }

    val formattedDateTime: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            return sdf.format(Date(timestamp))
        }

    fun toDisplayString(): String {
        return buildString {
            append("[$formattedDateTime] [${level.padEnd(7)}] [$tag]")
            if (!diagnosticId.isNullOrEmpty()) {
                append(" [ID:${diagnosticId.take(8)}]")
            }
            append(": $message")
            if (!exceptionMessage.isNullOrEmpty()) {
                append("\nException: $exceptionMessage")
            }
            if (!stackTrace.isNullOrEmpty()) {
                append("\nStack Trace:\n$stackTrace")
            }
        }
    }
}
