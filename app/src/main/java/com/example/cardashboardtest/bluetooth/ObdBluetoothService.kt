package com.example.cardashboardtest.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.cardashboardtest.data.BluetoothLogRepository
import com.example.cardashboardtest.model.BluetoothLog
import com.example.cardashboardtest.model.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.io.IOException
import java.util.Date
import java.util.UUID
import kotlin.math.exp

class ObdBluetoothService(private val context: Context) {
    private val TAG = "ObdBluetoothService"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedDevice: BluetoothDevice? = null
    private var connectionJob: Job? = null
    private var dataPollingJob: Job? = null
    private var connectionStartTime: Long? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val logRepository = BluetoothLogRepository(context)
    
    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    // OBD data
    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData
    
    // Data smoothing
    private val speedSmoother = DataSmoother(5)
    private val rpmSmoother = DataSmoother(5)
    private val tempSmoother = DataSmoother(10)
    private val voltageSmoother = DataSmoother(10)
    
    fun scanForDevices(): List<BluetoothDevice> {
        val devices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        coroutineScope.launch {
            devices.forEach { device ->
                logDeviceFound(device)
            }
        }
        return devices
    }
    
    fun connect(device: BluetoothDevice) {
        connectionJob?.cancel()
        connectionJob = coroutineScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                logConnectionAttempt(device)
                
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()
                
                if (bluetoothSocket?.isConnected == true) {
                    connectedDevice = device
                    connectionStartTime = System.currentTimeMillis()
                    _connectionState.value = ConnectionState.Connected(device.name ?: "Unknown Device")
                    logConnectionSuccess(device)
                    initializeObd()
                    startDataPolling()
                } else {
                    throw IOException("Failed to connect")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                logConnectionError(device, e)
                _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
                disconnect()
            }
        }
    }
    
    fun disconnect() {
        dataPollingJob?.cancel()
        connectionJob?.cancel()
        
        coroutineScope.launch {
            try {
                bluetoothSocket?.close()
                connectedDevice?.let { device ->
                    val duration = connectionStartTime?.let { System.currentTimeMillis() - it }
                    logDisconnect(device, duration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket", e)
                connectedDevice?.let { device ->
                    logError(device, "Error closing socket: ${e.message}")
                }
            }
            
            bluetoothSocket = null
            connectedDevice = null
            connectionStartTime = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }
    
    private suspend fun initializeObd() {
        try {
            // Reset OBD
            sendCommand("ATZ")
            delay(1000)
            
            // Turn off echo
            sendCommand("ATE0")
            
            // Set protocol to automatic
            sendCommand("ATSP0")
            
            // Turn off headers
            sendCommand("ATH0")
            
            // Turn off spaces
            sendCommand("ATS0")
            
            // Turn off linefeeds
            sendCommand("ATL0")
            
            // Set timeout
            sendCommand("ATSTFF")
            
            // Set long messages off
            sendCommand("ATL0")
            
            // Set adaptive timing mode 1
            sendCommand("ATAT1")
            
            // Set adaptive timing mode 2
            sendCommand("ATAT2")
            
            // Set protocol to automatic
            sendCommand("ATSP0")
            
            // Wait for protocol to be set
            delay(1000)
            
            // Test connection
            val response = sendCommand("0100")
            if (response == null || response.contains("NO DATA") || response.contains("ERROR")) {
                throw IOException("Failed to initialize OBD connection")
            }
            
            connectedDevice?.let { device ->
                logDataRead(device, "OBD initialization successful")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OBD initialization failed", e)
            connectedDevice?.let { device ->
                logError(device, "OBD initialization failed: ${e.message}")
            }
            throw e
        }
    }
    
    private fun startDataPolling() {
        dataPollingJob?.cancel()
        dataPollingJob = coroutineScope.launch {
            try {
                while (isActive) {
                    try {
                        pollObdData()
                        delay(100) // Poll 10 times per second
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) {
                            Log.d(TAG, "Data polling cancelled")
                            break
                        }
                        Log.e(TAG, "Error polling OBD data", e)
                        connectedDevice?.let { device ->
                            logError(device, "Data polling error: ${e.message}")
                        }
                        _connectionState.value = ConnectionState.Error("Data reading error: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Data polling job cancelled")
                } else {
                    Log.e(TAG, "Fatal error in data polling", e)
                }
            }
        }
    }
    
    private suspend fun pollObdData() {
        try {
            val currentData = ObdData(
                rpm = readPid("010C")?.let { rpmSmoother.smoothInt(calculateRpm(it)) },
                speed = readPid("010D")?.let { speedSmoother.smoothInt(it.toInt()) },
                engineTemp = readPid("0105")?.let { tempSmoother.smoothInt(it.toInt() - 40) },
                fuelLevel = readPid("012F")?.let { it.toInt() * 100 / 255 },
                batteryVoltage = readPid("ATRV")?.let { voltageSmoother.smoothFloat(it.toFloat()) },
                checkEngine = readPid("0101")?.let { it.toInt() and 0x80 != 0 },
                oilTemp = readPid("015C")?.let { it.toInt() - 40 }
            )
            
            // Only update if we have at least some valid data
            if (currentData.rpm != null || currentData.speed != null || 
                currentData.engineTemp != null || currentData.fuelLevel != null || 
                currentData.batteryVoltage != null || currentData.checkEngine != null || 
                currentData.oilTemp != null) {
                _obdData.value = currentData
                connectedDevice?.let { device ->
                    logDataRead(device, "Data updated: RPM=${currentData.rpm}, Speed=${currentData.speed}, Temp=${currentData.engineTemp}")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            Log.e(TAG, "Error in pollObdData", e)
            connectedDevice?.let { device ->
                logError(device, "Data polling error: ${e.message}")
            }
            throw e
        }
    }
    
    private suspend fun sendCommand(command: String): String? {
        return try {
            // Clear any pending data
            clearBuffer()
            
            // Send command
            bluetoothSocket?.outputStream?.write((command + "\r").toByteArray())
            delay(100)
            
            // Read response
            val response = readResponse()
            
            // Check for errors
            if (response.contains("STOPPED")) {
                Log.w(TAG, "Device stopped, attempting to reconnect")
                connectedDevice?.let { device ->
                    logError(device, "Device stopped")
                }
                reconnect()
                return null
            }
            
            // Check for negative response
            if (response.contains("7F")) {
                Log.w(TAG, "Negative response received for command: $command")
                connectedDevice?.let { device ->
                    logError(device, "Negative response for command: $command")
                }
                return null
            }
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command: $command", e)
            connectedDevice?.let { device ->
                logError(device, "Command error: ${e.message}")
            }
            null
        }
    }
    
    private suspend fun readResponse(): String {
        val buffer = ByteArray(1024)
        val response = StringBuilder()
        var attempts = 0
        val maxAttempts = 3
        
        while (attempts < maxAttempts) {
            val bytes = bluetoothSocket?.inputStream?.read(buffer)
            if (bytes != null && bytes > 0) {
                val chunk = String(buffer, 0, bytes).trim()
                response.append(chunk)
                
                // Check if we have a complete response
                if (chunk.contains(">")) {
                    break
                }
            }
            delay(100)
            attempts++
        }
        
        return response.toString().trim()
    }
    
    private suspend fun clearBuffer() {
        try {
            val buffer = ByteArray(1024)
            while (bluetoothSocket?.inputStream?.available() ?: 0 > 0) {
                bluetoothSocket?.inputStream?.read(buffer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing buffer", e)
        }
    }
    
    private suspend fun reconnect() {
        try {
            disconnect()
            delay(1000)
            connectedDevice?.let { connect(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error reconnecting", e)
            connectedDevice?.let { device ->
                logError(device, "Reconnection failed: ${e.message}")
            }
            _connectionState.value = ConnectionState.Error("Reconnection failed: ${e.message}")
        }
    }
    
    private suspend fun readPid(pid: String): String? {
        return sendCommand(pid)?.let { response ->
            if (response.contains("NO DATA") || response.contains("ERROR")) null
            else {
                // Extract the last valid hex value
                response.split(" ")
                    .filter { it.matches(Regex("[0-9A-Fa-f]+")) }
                    .lastOrNull()
            }
        }
    }
    
    private fun calculateRpm(hexValue: String): Int {
        return try {
            val value = hexValue.toInt(16)
            (value * 0.25).toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    // Logging functions
    private suspend fun logDeviceFound(device: BluetoothDevice) {
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name,
                deviceAddress = device.address,
                logType = LogType.DEVICE_FOUND,
                message = "Device found: ${device.name ?: "Unknown Device"}"
            )
        )
    }
    
    private suspend fun logConnectionAttempt(device: BluetoothDevice) {
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name,
                deviceAddress = device.address,
                logType = LogType.CONNECT,
                message = "Attempting to connect to device"
            )
        )
    }
    
    private suspend fun logConnectionSuccess(device: BluetoothDevice) {
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name,
                deviceAddress = device.address,
                logType = LogType.CONNECT,
                message = "Successfully connected to device"
            )
        )
    }
    
    private suspend fun logConnectionError(device: BluetoothDevice, error: Exception) {
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name,
                deviceAddress = device.address,
                logType = LogType.ERROR,
                message = "Connection error: ${error.message}"
            )
        )
    }
    
    private suspend fun logDisconnect(device: BluetoothDevice, duration: Long?) {
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name,
                deviceAddress = device.address,
                logType = LogType.DISCONNECT,
                message = "Disconnected from device",
                connectionDuration = duration
            )
        )
    }
    
    private suspend fun logDataRead(device: BluetoothDevice, message: String) {
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name,
                deviceAddress = device.address,
                logType = LogType.DATA_READ,
                message = message
            )
        )
    }
    
    private suspend fun logError(device: BluetoothDevice, message: String) {
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name,
                deviceAddress = device.address,
                logType = LogType.ERROR,
                message = message
            )
        )
    }
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val deviceName: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    data class ObdData(
        val rpm: Int? = null,
        val speed: Int? = null,
        val engineTemp: Int? = null,
        val fuelLevel: Int? = null,
        val batteryVoltage: Float? = null,
        val checkEngine: Boolean? = null,
        val oilTemp: Int? = null
    )
}

class DataSmoother(private val windowSize: Int) {
    private val values = ArrayDeque<Double>(windowSize)
    private val weights = List(windowSize) { i -> exp(i.toDouble() / windowSize) }
    private val weightSum = weights.sum()
    
    fun smoothInt(value: Number): Int {
        if (values.size >= windowSize) {
            values.removeFirst()
        }
        values.addLast(value.toDouble())
        
        return if (values.size < 3) {
            value.toInt()
        } else {
            var sum = 0.0
            values.forEachIndexed { index, v ->
                sum += v * weights[index]
            }
            (sum / weightSum).toInt()
        }
    }

    fun smoothFloat(value: Number): Float {
        if (values.size >= windowSize) {
            values.removeFirst()
        }
        values.addLast(value.toDouble())
        
        return if (values.size < 3) {
            value.toFloat()
        } else {
            var sum = 0.0
            values.forEachIndexed { index, v ->
                sum += v * weights[index]
            }
            (sum / weightSum).toFloat()
        }
    }
    
    fun reset() {
        values.clear()
    }
} 