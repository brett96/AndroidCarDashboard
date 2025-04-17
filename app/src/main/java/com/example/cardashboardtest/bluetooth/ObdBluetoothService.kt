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
    private var brokenPipeErrorCounter = 0 // Counter for consecutive errors
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val logRepository = BluetoothLogRepository(context)
    
    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    // OBD data
    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData
    
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
                brokenPipeErrorCounter = 0 // Reset error counter on new connection attempt
                
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
    
    fun disconnect(forcedDisconnectDueToError: Boolean = false) {
        dataPollingJob?.cancel()
        connectionJob?.cancel()
        brokenPipeErrorCounter = 0 // Reset counter on disconnect
        
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
            // Only set to Disconnected if not a forced disconnect caused by persistent errors
            if (!forcedDisconnectDueToError) {
                 _connectionState.value = ConnectionState.Disconnected
            }
        }
    }
    
    private suspend fun initializeObd() {
        try {
            // Reset OBD
            sendCommand("ATZ")
            delay(1000)
            
            // Turn off echo
            sendCommand("ATE0")
            delay(50) // Short delay after commands
            
            // Set protocol to automatic
            sendCommand("ATSP0")
            delay(50)
            
            // Turn off headers
            sendCommand("ATH0")
            delay(50)
            
            // Turn off spaces (Important for parsing!)
            sendCommand("ATS0") // Already present, ensure it's ATS0 not ATS1
            delay(50)
            
            // Turn off linefeeds
            sendCommand("ATL0")
            delay(50)
            
            // Set timeout to max (adjust if needed)
            sendCommand("ATSTFF") // Or a specific hex value like ATST64 for ~100ms
            delay(50)
            
            // Set long messages off - Removed, potentially needed for some PIDs
            // sendCommand("ATL0") // Duplicate? Already turned off linefeeds
            
            // Adaptive timing mode 1 - Use default or AT1
            sendCommand("ATAT1")
            delay(50)
            
            // Set adaptive timing mode 2 - Maybe not needed? Let's comment out for now
            // sendCommand("ATAT2")
            // delay(50)
            
            // Set protocol to automatic - Redundant?
            // sendCommand("ATSP0")
            // delay(1000) // Long delay already present
            
            // Test connection with a standard PID
            val response = sendCommand("0100") // Get supported PIDs [01-20]
            delay(200) // Allow time for response
            if (response == null || response.contains("NODATA", ignoreCase = true) || response.contains("ERROR", ignoreCase = true)) {
                throw IOException("Failed to initialize OBD connection - No response to 0100")
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

    private suspend fun logPolledObdData(data: ObdData) {
        connectedDevice?.let { device ->
            // Log converted, human-readable data
            val message = "Polled Data: RPM=${data.rpm ?: "N/A"}, Speed=${data.speed?.toString() ?: "N/A"} km/h, " +
                    "Engine Temp=${data.engineTemp?.toString() ?: "N/A"} °C, Fuel Level=${data.fuelLevel?.toString() ?: "N/A"}%, " +
                    "Voltage=${data.batteryVoltage?.toString() ?: "N/A"} V, Check Engine=${data.checkEngine?.toString() ?: "N/A"}, " +
                    "Oil Temp=${data.oilTemp?.toString() ?: "N/A"} °C"

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
    }

    private fun startDataPolling() {
        dataPollingJob?.cancel()
        dataPollingJob = coroutineScope.launch {
            try {
                while (isActive && _connectionState.value is ConnectionState.Connected) { // Only poll when connected
                    try {
                        val currentData = pollObdData()
                        _obdData.value = currentData // Update StateFlow with latest data
                        logPolledObdData(currentData) // Log the converted data
                        delay(200L) // Poll frequency (e.g., 5 times per second)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) {
                            Log.d(TAG, "Data polling cancelled")
                            throw e // Re-throw cancellation
                        }
                        // Check if it was a persistent error handled by send/read
                        if (_connectionState.value !is ConnectionState.Error) {
                             Log.e(TAG, "Error polling OBD data", e)
                             connectedDevice?.let { device ->
                                logError(device, "Data polling error: ${e.message}")
                             }
                             // Set temporary error state, might be overridden by persistent check
                             _connectionState.value = ConnectionState.Error("Data reading error: ${e.message}")
                        }
                        // If a persistent error occurred, the state is already set, and the loop should break
                        if (_connectionState.value is ConnectionState.Error && (_connectionState.value as ConnectionState.Error).message.contains("Persistent")) {
                            break // Exit polling loop on persistent error
                        }
                        delay(1000L) // Wait longer after an error before retrying
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Data polling job cancelled")
                } else {
                    Log.e(TAG, "Fatal error in data polling coroutine", e)
                }
            } finally {
                 Log.d(TAG, "Data polling stopped.")
            }
        }
    }
    
    private suspend fun pollObdData() : ObdData {
        // Read multiple PIDs in sequence
        val rpm = readPid("010C")?.let { parseRpm(it) }
        val speed = readPid("010D")?.let { parseSpeed(it) }
        val engineTemp = readPid("0105")?.let { parseTemp(it) }
        val fuelLevel = readPid("012F")?.let { parseFuelLevel(it) }
        val batteryVoltage = sendCommand("ATRV")?.let { parseVoltage(it) } // Use ATRV
        val milStatus = readPid("0101")?.let { parseMilStatus(it) }
        val oilTemp = readPid("015C")?.let { parseTemp(it) }

        // Return ObdData with potentially null values if reads failed
        return ObdData(
            rpm = rpm,
            speed = speed,
            engineTemp = engineTemp,
            fuelLevel = fuelLevel,
            batteryVoltage = batteryVoltage,
            checkEngine = milStatus,
            oilTemp = oilTemp
        )
    }

    private suspend fun sendCommand(command: String): String? {
        return try {
            if (bluetoothSocket?.isConnected != true) {
                Log.e(TAG, "Socket not connected when trying to send command: $command")
                handleConnectionError(command)
                return null
            }

            clearBuffer() // Clear buffer before sending

            connectedDevice?.let { device ->
                logDataRead(device, "Sending Command: $command") // Log command clearly
            }

            bluetoothSocket?.outputStream?.write((command + "\r").toByteArray())
            bluetoothSocket?.outputStream?.flush()
            delay(50) // Short delay after sending

            val response = readResponse()

            connectedDevice?.let { device ->
                logDataRead(device, "Received Raw: $response") // Log raw response directly
            }
            
            // Reset error counter on successful communication
            brokenPipeErrorCounter = 0

            // Basic validation - check for common errors explicitly
             if (response.contains("NODATA", ignoreCase = true)) {
                Log.w(TAG, "Received NO DATA for command: $command")
                connectedDevice?.let { logError(it, "NO DATA received for $command") }
                return null // Treat NO DATA as null response
            }
             if (response.contains("?", ignoreCase = true) || response.contains("ERROR", ignoreCase = true)) {
                Log.w(TAG, "Received ERROR response for command: $command : $response")
                connectedDevice?.let { logError(it, "ERROR response for $command: $response") }
                return null // Treat ERROR as null response
            }
            if (response.contains("UNABLETOCONNECT", ignoreCase = true)) {
                Log.e(TAG, "Received UNABLE TO CONNECT for command: $command")
                handleConnectionError(command)
                return null
            }
            if (response.contains("STOPPED", ignoreCase = true)) {
                 Log.w(TAG, "Device stopped, attempting to reconnect")
                 handleConnectionError(command)
                 return null
             }
             if (response.contains("BUSINIT", ignoreCase = true)) {
                 Log.w(TAG, "Bus Initialization Error for command: $command")
                 connectedDevice?.let { logError(it, "Bus Init Error for $command") }
                 // Sometimes requires a delay or re-init
                 delay(500)
                 return null
             }
            // Check for negative response (7F)
            if (response.contains("7F")) { // 7F indicates ECU negative response
                Log.w(TAG, "Negative response (7F) received for command: $command")
                connectedDevice?.let { logError(it, "Negative response (7F) for command: $command") }
                return null
            }

            response // Return the potentially processed response
        } catch (e: IOException) {
            Log.e(TAG, "IO Error sending/reading command: $command", e)
            if (e.message?.contains("Broken pipe", ignoreCase = true) == true ||
                e.message?.contains("socket closed", ignoreCase = true) == true ||
                e.message?.contains("connection reset", ignoreCase = true) == true) {
                handleConnectionError(command)
            } else {
                 connectedDevice?.let { logError(it, "IO Error for command $command: ${e.message}") }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Error sending command: $command", e)
            connectedDevice?.let { logError(it, "Unexpected command error for $command: ${e.message}") }
            null
        }
    }
    
    private suspend fun readResponse(): String {
        val buffer = ByteArray(1024)
        val response = StringBuilder()
        val endTime = System.currentTimeMillis() + 2000 // 2 second timeout for reading

        while (System.currentTimeMillis() < endTime) {
             try {
                if (bluetoothSocket?.isConnected != true) {
                    throw IOException("Socket not connected during read")
                }

                val inputStream = bluetoothSocket?.inputStream
                if (inputStream?.available() ?: 0 > 0) {
                    val bytesRead = inputStream?.read(buffer)
                    if (bytesRead != null && bytesRead > 0) {
                        val chunk = String(buffer, 0, bytesRead)
                        response.append(chunk)
                        // Check for OBD prompt '>' which signifies end of response
                        if (chunk.contains('>')) {
                            break
                        }
                    } else if (bytesRead == -1) {
                         // End of stream reached unexpectedly
                         throw IOException("End of stream reached while reading response")
                    }
                } else {
                    // No data available, short delay before checking again
                    delay(50)
                }
             } catch (e: IOException) {
                 Log.e(TAG, "IO Error reading response", e)
                 // Check if it's a connection error like broken pipe
                 if (e.message?.contains("Broken pipe", ignoreCase = true) == true ||
                     e.message?.contains("socket closed", ignoreCase = true) == true ||
                     e.message?.contains("connection reset", ignoreCase = true) == true) {
                     // Let sendCommand handle the reconnect logic
                     throw e // Re-throw to be caught by sendCommand
                 }
                 // Other IO errors might be temporary, log and continue trying within timeout
                 connectedDevice?.let { logError(it, "Read IO Error: ${e.message}") }
                 delay(100) // Wait a bit longer after other IO errors
             } catch (e: Exception) {
                 Log.e(TAG, "Unexpected error reading response", e)
                 connectedDevice?.let { logError(it, "Unexpected Read Error: ${e.message}") }
                 delay(100)
             }
        }

        if (System.currentTimeMillis() >= endTime && response.isEmpty()) {
            Log.w(TAG, "Read response timed out")
             connectedDevice?.let { logError(it, "Read response timed out") }
        }
        
        // Clean up the response: remove command echo, whitespace, prompt '>'
        return cleanObdResponse(response.toString())
    }

    private fun cleanObdResponse(rawResponse: String): String {
         // Remove echoes of the command (like "010C\r")
         // Remove extra whitespace and the final prompt '>'
         return rawResponse
             // Use double backslashes for regex escapes
             .replace(Regex("^[0-9A-Fa-f]+\\s*\\r"), "") // Remove command echo like "010C\r"
             .replace(Regex("[\\s>]"), "") // Remove all whitespace and '>' prompt
             .uppercase() // Standardize to uppercase hex
    }
    
    private suspend fun clearBuffer() {
        try {
            if (bluetoothSocket?.isConnected != true) {
                Log.w(TAG, "Attempted to clear buffer but socket not connected.")
                return
            }
            val inputStream = bluetoothSocket?.inputStream
            val available = inputStream?.available() ?: 0
            if (available > 0) {
                Log.d(TAG, "Clearing $available bytes from input buffer.")
                inputStream?.skip(available.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing buffer", e)
            if (e is IOException && (e.message?.contains("Broken pipe", ignoreCase = true) == true ||
                                      e.message?.contains("socket closed", ignoreCase = true) == true)) {
                handleConnectionError("buffer clearing")
            }
        }
    }

    // Centralized handling for connection errors like broken pipe
    private suspend fun handleConnectionError(contextCommand: String) {
         brokenPipeErrorCounter++
         Log.w(TAG, "Connection error ($brokenPipeErrorCounter/$MAX_BROKEN_PIPE_ERRORS) during: $contextCommand")
         connectedDevice?.let { logError(it, "Connection error #$brokenPipeErrorCounter during $contextCommand") }

         if (brokenPipeErrorCounter >= MAX_BROKEN_PIPE_ERRORS) {
             Log.e(TAG, "Persistent connection error threshold reached. Disconnecting.")
             _connectionState.value = ConnectionState.Error("Persistent connection failure. Please reconnect.")
             // Disconnect without immediately setting state back to Disconnected
             disconnect(forcedDisconnectDueToError = true)
         } else {
             // Attempt reconnect if threshold not reached
             reconnect()
         }
    }
    
    private suspend fun reconnect() {
        // Don't attempt reconnect if already handling persistent error or disconnected
        if (_connectionState.value is ConnectionState.Error && (_connectionState.value as ConnectionState.Error).message.contains("Persistent")) {
            Log.w(TAG, "Reconnect attempt skipped due to persistent error state.")
            return
        }
         if (_connectionState.value == ConnectionState.Disconnected) {
            Log.w(TAG, "Reconnect attempt skipped as state is already Disconnected.")
            return
        }

        try {
            Log.d(TAG, "Attempting temporary reconnect...")
            val deviceToReconnect = connectedDevice // Store device before disconnect clears it

            // Close existing potentially broken connection first
            dataPollingJob?.cancel() // Stop polling
            connectionJob?.cancel() // Cancel any ongoing connection attempt
            try { bluetoothSocket?.close() } catch (e: Exception) { /* Ignore close errors */ }
            bluetoothSocket = null
            _connectionState.value = ConnectionState.Connecting // Show reconnecting state

            delay(1000) // Wait before attempting to reconnect

            if (deviceToReconnect != null) {
                Log.d(TAG, "Reconnecting to device: ${deviceToReconnect.name}")
                logConnectionAttempt(deviceToReconnect)
                // Re-establish the connection using the original connect logic but without canceling jobs
                 bluetoothSocket = deviceToReconnect.createRfcommSocketToServiceRecord(SPP_UUID)
                 bluetoothSocket?.connect()

                 if (bluetoothSocket?.isConnected == true) {
                     connectedDevice = deviceToReconnect // Re-assign connected device
                     // Don't reset connectionStartTime here
                     _connectionState.value = ConnectionState.Connected(deviceToReconnect.name ?: "Unknown Device")
                     Log.d(TAG, "Reconnection successful.")
                     logConnectionSuccess(deviceToReconnect)
                     brokenPipeErrorCounter = 0 // Reset error counter on successful reconnect
                     initializeObd() // Re-initialize OBD communication
                     startDataPolling() // Restart data polling
                 } else {
                     throw IOException("Failed to reconnect")
                 }
            } else {
                 Log.w(TAG, "Cannot reconnect, no device information available.")
                 _connectionState.value = ConnectionState.Disconnected // Fall back to disconnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during reconnect attempt", e)
            connectedDevice?.let { device ->
                logError(device, "Reconnection attempt failed: ${e.message}")
            }
             // If reconnect fails, consider it a persistent error
             _connectionState.value = ConnectionState.Error("Reconnection failed: ${e.message}. Please reconnect manually.")
             disconnect(forcedDisconnectDueToError = true) // Force disconnect after failed reconnect
        }
    }
    
    private suspend fun readPid(pid: String): String? {
        // Send the command (e.g., "010C") and get the cleaned response
        val rawResponse = sendCommand(pid) ?: return null // Returns null if command failed or got NO DATA/ERROR

        // Expected response format for Mode 01 PIDs: 41<PID><DATA>
        // Example: "410C1AF0" for RPM
        val expectedHeader = "41${pid.substring(2)}" // e.g., "410C" for command "010C"
        
        return if (rawResponse.startsWith(expectedHeader)) {
            // Extract data bytes after the header
            rawResponse.substring(expectedHeader.length)
        } else {
            Log.w(TAG, "Unexpected response format for PID $pid: $rawResponse")
            connectedDevice?.let { logError(it, "Bad response format for $pid: $rawResponse") }
            null
        }
    }

    // --- Data Parsing Functions ---

    private fun parseRpm(hexData: String): Int? {
        // Formula: (256A + B) / 4. Expects 4 hex digits (2 bytes).
        return try {
            if (hexData.length >= 4) {
                val a = hexData.substring(0, 2).toInt(16)
                val b = hexData.substring(2, 4).toInt(16)
                ((a * 256) + b) / 4
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse RPM: $hexData", e)
            null
        }
    }

    private fun parseSpeed(hexData: String): Int? {
        // Formula: A (km/h). Expects 2 hex digits (1 byte).
        return try {
            if (hexData.length >= 2) {
                hexData.substring(0, 2).toInt(16)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Speed: $hexData", e)
            null
        }
    }

    private fun parseTemp(hexData: String): Int? {
        // Formula: A - 40 (°C). Expects 2 hex digits (1 byte).
        return try {
            if (hexData.length >= 2) {
                hexData.substring(0, 2).toInt(16) - 40
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Temperature: $hexData", e)
            null
        }
    }

    private fun parseFuelLevel(hexData: String): Int? {
        // Formula: (A * 100) / 255 (%). Expects 2 hex digits (1 byte).
        return try {
            if (hexData.length >= 2) {
                (hexData.substring(0, 2).toInt(16) * 100) / 255
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Fuel Level: $hexData", e)
            null
        }
    }
    
     private fun parseVoltage(atrvResponse: String): Float? {
        // ATRV response is typically like "14.1V"
        // Need to extract the float value
        return try {
             // Use double backslashes for regex escapes
            val regex = Regex("(\\d+\\.?\\d*)")
            val matchResult = regex.find(atrvResponse)
            matchResult?.groupValues?.get(1)?.toFloat()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Voltage from ATRV: $atrvResponse", e)
            null
        }
    }

     private fun parseMilStatus(hexData: String): Boolean? {
        // PID 0101, Response: 41 01 [A] [B] [C] [D]
        // Byte A, Bit 7 (MSB) is MIL status (1 = ON, 0 = OFF)
        // Also includes number of DTCs in lower 7 bits of Byte A.
         return try {
             if (hexData.length >= 2) {
                 val byteA = hexData.substring(0, 2).toInt(16)
                 // Check if the most significant bit (bit 7) is set
                 // Use bitwise AND correctly
                 (byteA and 0x80) != 0
             } else null
         } catch (e: Exception) {
             Log.e(TAG, "Failed to parse MIL Status: $hexData", e)
             null
         }
     }
    
    // --- Logging functions (using repository directly) ---
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
        // Generic log for sent/received data
        logRepository.insertLog(
            BluetoothLog(
                timestamp = Date(),
                deviceName = device.name ?: "Unknown Device",
                deviceAddress = device.address,
                logType = LogType.DATA_READ, // Using existing type
                message = message // Message includes context like "Sending Command:" or "Received Raw:"
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
        val speed: Int? = null,          // Kilometers per hour (km/h)
        val engineTemp: Int? = null,      // Celsius (°C)
        val fuelLevel: Int? = null,       // Percentage (%)
        val batteryVoltage: Float? = null, // Volts (V)
        val checkEngine: Boolean? = null, // Check Engine Light (MIL) status
        val oilTemp: Int? = null          // Celsius (°C)
    )

    companion object {
        private const val MAX_BROKEN_PIPE_ERRORS = 3 // Threshold for persistent errors
    }
} 