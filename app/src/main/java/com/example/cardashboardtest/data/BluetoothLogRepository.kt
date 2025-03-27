package com.example.cardashboardtest.data

import android.content.Context
import com.example.cardashboardtest.model.BluetoothLog
import com.example.cardashboardtest.model.LogType
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.concurrent.TimeUnit

class BluetoothLogRepository(context: Context) {
    private val bluetoothLogDao = AppDatabase.getDatabase(context).bluetoothLogDao()

    fun getAllLogs(): Flow<List<BluetoothLog>> = bluetoothLogDao.getAllLogs()

    fun getRecentLogs(): Flow<List<BluetoothLog>> {
        val cutoffDate = Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(72))
        return bluetoothLogDao.getRecentLogs(cutoffDate)
    }

    fun getLogsByDevice(deviceAddress: String): Flow<List<BluetoothLog>> =
        bluetoothLogDao.getLogsByDevice(deviceAddress)

    fun getLogsByType(logType: LogType): Flow<List<BluetoothLog>> =
        bluetoothLogDao.getLogsByType(logType)

    fun getUnreadLogs(): Flow<List<BluetoothLog>> = bluetoothLogDao.getUnreadLogs()

    fun searchLogs(query: String): Flow<List<BluetoothLog>> = bluetoothLogDao.searchLogs(query)

    suspend fun insertLog(log: BluetoothLog) {
        bluetoothLogDao.insertLog(log)
        cleanupOldLogs()
    }

    private suspend fun cleanupOldLogs() {
        val cutoffDate = Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(72))
        bluetoothLogDao.deleteOldLogs(cutoffDate)
    }

    suspend fun deleteAllLogs() {
        bluetoothLogDao.deleteAllLogs()
    }

    suspend fun markLogAsRead(logId: Long) {
        bluetoothLogDao.markLogAsRead(logId)
    }

    suspend fun markAllLogsAsRead() {
        bluetoothLogDao.markAllLogsAsRead()
    }
} 