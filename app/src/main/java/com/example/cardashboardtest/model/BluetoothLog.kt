package com.example.cardashboardtest.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "bluetooth_logs")
data class BluetoothLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceName: String,
    val deviceAddress: String,
    val timestamp: Date,
    val logType: LogType,
    val message: String,
    val connectionDuration: Long? = null, // in milliseconds
    val isRead: Boolean = false
)

enum class LogType {
    ERROR,
    DEVICE_FOUND,
    CONNECT,
    DATA_READ,
    DISCONNECT
} 