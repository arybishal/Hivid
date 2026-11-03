package com.hivid.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hivid.app.data.local.HistoryDao
import com.hivid.app.data.local.HistoryEntity
import com.hivid.app.data.local.HividDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao: HistoryDao = HividDatabase.getDatabase(application).historyDao()

    val historyList: StateFlow<List<HistoryEntity>> = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteItem(entity: HistoryEntity) {
        viewModelScope.launch {
            historyDao.delete(entity)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }
}
