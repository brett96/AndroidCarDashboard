package com.example.cardashboardtest.data

import androidx.room.*
import com.example.cardashboardtest.model.BluetoothLog
import com.example.cardashboardtest.model.LogType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface BluetoothLogDao {
    @Query("SELECT * FROM bluetooth_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<BluetoothLog>>

    @Query("SELECT * FROM bluetooth_logs WHERE timestamp > :cutoffDate ORDER BY timestamp DESC")
    fun getRecentLogs(cutoffDate: Date): Flow<List<BluetoothLog>>

    @Query("SELECT * FROM bluetooth_logs WHERE deviceAddress = :deviceAddress ORDER BY timestamp DESC")
    fun getLogsByDevice(deviceAddress: String): Flow<List<BluetoothLog>>

    @Query("SELECT * FROM bluetooth_logs WHERE logType = :logType ORDER BY timestamp DESC")
    fun getLogsByType(logType: LogType): Flow<List<BluetoothLog>>

    @Query("SELECT * FROM bluetooth_logs WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadLogs(): Flow<List<BluetoothLog>>

    @Query("SELECT * FROM bluetooth_logs WHERE deviceName LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<BluetoothLog>>

    @Insert
    suspend fun insertLog(log: BluetoothLog)

    @Query("DELETE FROM bluetooth_logs WHERE timestamp < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: Date)

    @Query("DELETE FROM bluetooth_logs")
    suspend fun deleteAllLogs()

    @Query("UPDATE bluetooth_logs SET isRead = 1 WHERE id = :logId")
    suspend fun markLogAsRead(logId: Long)

    @Query("UPDATE bluetooth_logs SET isRead = 1")
    suspend fun markAllLogsAsRead()
} 