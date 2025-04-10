package com.example.cardashboardtest.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.cardashboardtest.data.BluetoothLogRepository
import com.example.cardashboardtest.model.BluetoothLog
import com.example.cardashboardtest.model.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date

class LoggingHelper(context: Context, private val coroutineScope: CoroutineScope) {
    private val repository = BluetoothLogRepository(context)

    fun logDeviceFound(device: BluetoothDevice) {
        coroutineScope.launch {
            repository.insertLog(
                BluetoothLog(
                    timestamp = Date(),
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    logType = LogType.DEVICE_FOUND,
                    message = "Device found: ${device.name ?: "Unknown Device"}"
                )
            )
        }
    }

    fun logConnectionAttempt(device: BluetoothDevice) {
        coroutineScope.launch {
            repository.insertLog(
                BluetoothLog(
                    timestamp = Date(),
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    logType = LogType.CONNECT,
                    message = "Attempting to connect to device"
                )
            )
        }
    }

    fun logConnectionSuccess(device: BluetoothDevice) {
        coroutineScope.launch {
            repository.insertLog(
                BluetoothLog(
                    timestamp = Date(),
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    logType = LogType.CONNECT,
                    message = "Successfully connected to device"
                )
            )
        }
    }

    fun logConnectionError(device: BluetoothDevice, error: Exception) {
        coroutineScope.launch {
            repository.insertLog(
                BluetoothLog(
                    timestamp = Date(),
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    logType = LogType.ERROR,
                    message = "Connection error: ${error.message}"
                )
            )
        }
    }

    fun logDisconnect(device: BluetoothDevice, duration: Long?) {
        coroutineScope.launch {
            repository.insertLog(
                BluetoothLog(
                    timestamp = Date(),
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    logType = LogType.DISCONNECT,
                    message = "Disconnected from device",
                    connectionDuration = duration
                )
            )
        }
    }

    fun logDataRead(device: BluetoothDevice, message: String) {
        coroutineScope.launch {
            repository.insertLog(
                BluetoothLog(
                    timestamp = Date(),
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    logType = LogType.DATA_READ,
                    message = message
                )
            )
        }
    }

    fun logError(device: BluetoothDevice, message: String) {
        coroutineScope.launch {
            repository.insertLog(
                BluetoothLog(
                    timestamp = Date(),
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    logType = LogType.ERROR,
                    message = message
                )
            )
        }
    }
}
