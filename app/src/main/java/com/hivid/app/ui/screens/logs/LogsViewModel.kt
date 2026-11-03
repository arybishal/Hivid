package com.hivid.app.ui.screens.logs

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hivid.app.data.local.HividDatabase
import com.hivid.app.data.local.LogDao
import com.hivid.app.data.local.LogEntity
import com.hivid.app.domain.model.LogLevel
import com.hivid.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class SystemHealthStatus {
    HEALTHY,
    WARNING,
    ERROR
}

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val logDao: LogDao = HividDatabase.getDatabase(application).logDao()

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow<LogLevel?>(null) // null = ALL

    val totalCount: StateFlow<Int> = logDao.getTotalLogCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val errorCount: StateFlow<Int> = logDao.getCountByLevel("ERROR")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val warningCount: StateFlow<Int> = logDao.getCountByLevel("WARNING")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val healthStatus: StateFlow<SystemHealthStatus> = combine(errorCount, warningCount) { errors, warnings ->
        when {
            errors > 0 -> SystemHealthStatus.ERROR
            warnings > 0 -> SystemHealthStatus.WARNING
            else -> SystemHealthStatus.HEALTHY
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemHealthStatus.HEALTHY)

    @OptIn(ExperimentalCoroutinesApi::class)
    val logs: StateFlow<List<LogEntity>> = combine(searchQuery, selectedFilter) { query, filter ->
        Pair(query.trim(), filter)
    }.flatMapLatest { (query, filter) ->
        when {
            query.isEmpty() && filter == null -> logDao.getAllLogs()
            query.isEmpty() && filter != null -> logDao.getLogsByLevel(filter.name)
            query.isNotEmpty() && filter == null -> logDao.searchLogs(query)
            else -> logDao.searchLogsWithLevel(query, filter!!.name)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilter(filter: LogLevel?) {
        selectedFilter.value = filter
    }

    fun clearLogs() {
        viewModelScope.launch {
            logDao.clearAll()
            AppLogger.i("LogsViewModel", "User cleared all application logs")
        }
    }

    fun copyLogToClipboard(context: Context, log: LogEntity) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Hivid Log", log.toDisplayString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Log entry copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun copyAllLogsToClipboard(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            val allLogs = logs.value
            val text = AppLogger.exportLogsToString(allLogs)
            withContext(Dispatchers.Main) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Hivid All Logs", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "All ${allLogs.size} logs copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportAndShareLogs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allLogs = logs.value
                val text = AppLogger.exportLogsToString(allLogs)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(context.cacheDir, "Hivid_Diagnostics_$timeStamp.txt")
                file.writeText(text)

                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Hivid Diagnostics Log ($timeStamp)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostic Logs"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export logs: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
