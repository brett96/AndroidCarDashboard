package com.example.cardashboardtest.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cardashboardtest.data.BluetoothLogRepository
import com.example.cardashboardtest.model.BluetoothLog
import com.example.cardashboardtest.model.LogType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BluetoothLogRepository = BluetoothLogRepository(application)
    private val _logs = MutableLiveData<List<BluetoothLog>>()
    val logs: LiveData<List<BluetoothLog>> = _logs

    private val activeFilters = mutableSetOf<LogType>()

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            repository.getAllLogs().collectLatest { logs ->
                applyFilters(logs)
            }
        }
    }

    fun toggleFilter(logType: LogType, isChecked: Boolean) {
        if (isChecked) {
            activeFilters.add(logType)
        } else {
            activeFilters.remove(logType)
        }
        loadLogs()
    }

    private fun applyFilters(logs: List<BluetoothLog>) {
        val filteredLogs = if (activeFilters.isEmpty()) {
            logs
        } else {
            logs.filter { log -> activeFilters.contains(log.logType) }
        }
        _logs.value = filteredLogs
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.deleteAllLogs()
            loadLogs()
        }
    }
} 