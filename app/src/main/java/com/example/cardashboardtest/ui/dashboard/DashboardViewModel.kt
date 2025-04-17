package com.example.cardashboardtest.ui.dashboard

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cardashboardtest.bluetooth.ObdBluetoothService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.exp
import kotlin.random.Random

class DataSmoother(private val windowSize: Int) {
    private val values = ArrayDeque<Double>(windowSize)
    private val weights = List(windowSize) { i -> exp(i.toDouble() / windowSize) }
    private val weightSum = weights.sum()

    fun smoothInt(value: Number): Int {
        synchronized(values) {
            if (values.size >= windowSize) {
                values.removeFirst()
            }
            values.addLast(value.toDouble())

            return if (values.size < 3) { // Require at least 3 values for smoothing
                value.toInt()
            } else {
                var weightedSum = 0.0
                var currentWeightSum = 0.0
                values.forEachIndexed { index, v ->
                    // Apply weights based on current position in deque
                    val weight = weights.getOrElse(index) { 1.0 } // Use 1.0 if weights list is somehow smaller
                    weightedSum += v * weight
                    currentWeightSum += weight
                }
                if (currentWeightSum == 0.0) value.toInt() else (weightedSum / currentWeightSum).toInt()
            }
        }
    }

    fun smoothFloat(value: Number): Float {
        synchronized(values) {
            if (values.size >= windowSize) {
                values.removeFirst()
            }
            values.addLast(value.toDouble())

            return if (values.size < 3) { // Require at least 3 values for smoothing
                value.toFloat()
            } else {
                var weightedSum = 0.0
                var currentWeightSum = 0.0
                 values.forEachIndexed { index, v ->
                    // Apply weights based on current position in deque
                    val weight = weights.getOrElse(index) { 1.0 } // Use 1.0 if weights list is somehow smaller
                    weightedSum += v * weight
                    currentWeightSum += weight
                }
                 if (currentWeightSum == 0.0) value.toFloat() else (weightedSum / currentWeightSum).toFloat()
            }
        }
    }

    fun reset() {
        synchronized(values) {
            values.clear()
        }
    }
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val obdService = ObdBluetoothService(application)

    private val speedSmoother = DataSmoother(5)
    private val rpmSmoother = DataSmoother(5)
    private val tempSmoother = DataSmoother(10)
    private val fuelSmoother = DataSmoother(20)
    private val voltageSmoother = DataSmoother(10)

    private val _speed = MutableLiveData<Int>()
    val speed: LiveData<Int> = _speed

    private val _rpm = MutableLiveData<Int>()
    val rpm: LiveData<Int> = _rpm

    private val _fuelLevel = MutableLiveData<Int>()
    val fuelLevel: LiveData<Int> = _fuelLevel

    private val _engineTemp = MutableLiveData<Int>()
    val engineTemp: LiveData<Int> = _engineTemp

    private val _oilPressure = MutableLiveData<Int>()
    val oilPressure: LiveData<Int> = _oilPressure

    private val _voltage = MutableLiveData<Float>()
    val voltage: LiveData<Float> = _voltage

    private val _checkGauges = MutableLiveData<Boolean>()
    val checkGauges: LiveData<Boolean> = _checkGauges

    private val _lowOil = MutableLiveData<Boolean>()
    val lowOil: LiveData<Boolean> = _lowOil

    private val _engineRunning = MutableLiveData<Boolean>()
    val engineRunning: LiveData<Boolean> = _engineRunning

    private val _gear = MutableLiveData<String>()
    val gear: LiveData<String> = _gear

    private val _connectionState = MutableLiveData<ObdBluetoothService.ConnectionState>()
    val connectionState: LiveData<ObdBluetoothService.ConnectionState> = _connectionState

    private val _showPersistentError = MutableStateFlow(false)
    val showPersistentError: StateFlow<Boolean> = _showPersistentError.asStateFlow()

    private var useSimulatedData = true

    init {
        _speed.value = 0
        _rpm.value = 0
        _fuelLevel.value = 75
        _engineTemp.value = 90
        _engineRunning.value = false
        _gear.value = "P"
        _oilPressure.value = 45
        _voltage.value = 14.4f
        _checkGauges.value = false
        _lowOil.value = false

        obdService.connectionState
            .onEach { state ->
                _connectionState.value = state
                useSimulatedData = state !is ObdBluetoothService.ConnectionState.Connected
                
                if (state !is ObdBluetoothService.ConnectionState.Error || 
                    !(state.message.contains("Persistent connection failure"))) { 
                    _showPersistentError.value = false
                }
                
                if (state is ObdBluetoothService.ConnectionState.Error && 
                    state.message.contains("Persistent connection failure")) {
                    _showPersistentError.value = true
                }

                if (state is ObdBluetoothService.ConnectionState.Connected) {
                    resetSmoothers()
                    _speed.value = 0
                    _rpm.value = 0
                    _fuelLevel.value = 0
                    _engineTemp.value = 0
                    _voltage.value = 0f
                    _checkGauges.value = false
                    _lowOil.value = false
                    _engineRunning.value = false
                    _gear.value = "P"
                } else {
                    if (!useSimulatedData) {
                         resetSmoothers()
                         _speed.value = 0
                         _rpm.value = 0
                         _fuelLevel.value = 0
                         _engineTemp.value = 0
                         _voltage.value = 0f
                         _checkGauges.value = false
                         _lowOil.value = false
                         _engineRunning.value = false
                         _gear.value = "P"
                    }
                }
            }
            .launchIn(viewModelScope)

        obdService.obdData
            .onEach { data ->
                if (!useSimulatedData) {
                    data.speed?.let { _speed.value = speedSmoother.smoothInt(it) }
                    data.rpm?.let { _rpm.value = rpmSmoother.smoothInt(it) }
                    data.engineTemp?.let { _engineTemp.value = tempSmoother.smoothInt(it) }
                    data.fuelLevel?.let { _fuelLevel.value = fuelSmoother.smoothInt(it) }
                    data.batteryVoltage?.let { _voltage.value = voltageSmoother.smoothFloat(it) }
                    data.checkEngine?.let { _checkGauges.value = it }
                    
                    _engineRunning.value = data.rpm != null && data.rpm > 0
                    
                    if (data.speed != null) {
                        updateGear(data.speed)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun scanForDevices(): List<BluetoothDevice> {
        return obdService.scanForDevices()
    }

    fun connectToDevice(device: BluetoothDevice) {
        _showPersistentError.value = false
        obdService.connect(device)
    }

    fun disconnectDevice() {
        obdService.disconnect()
        _showPersistentError.value = false
        useSimulatedData = true
        resetSmoothers()
        _speed.value = 0
        _rpm.value = 0
        _fuelLevel.value = 75
        _engineTemp.value = 90
        _voltage.value = 14.4f
        _checkGauges.value = false
        _lowOil.value = false
        _engineRunning.value = false
        _gear.value = "P"
    }

    fun startEngine() {
        _engineRunning.value = true
        _rpm.value = 1000
        if (useSimulatedData) {
            simulateDataChanges()
        }
    }

    fun stopEngine() {
        _engineRunning.value = false
        _speed.value = 0
        _rpm.value = 0
        _gear.value = "P"
    }

    fun setSpeed(speed: Int) {
        if (useSimulatedData) {
            _speed.value = speed
            _rpm.value = (speed * 50) + 1000
        }
    }

    fun setGear(gear: String) {
        _gear.value = gear
    }

    fun simulateDataChanges() {
        if (useSimulatedData && _engineRunning.value == true) {
            _engineTemp.value = _engineTemp.value?.let { temp ->
                (temp + Random.nextInt(-2, 3)).coerceIn(50, 120)
            }

            _oilPressure.value = _oilPressure.value?.let { pressure ->
                (pressure + Random.nextInt(-5, 6)).coerceIn(0, 80)
            }

            _voltage.value = _voltage.value?.let { voltage ->
                (voltage + Random.nextFloat() * 0.2f - 0.1f).coerceIn(11.0f, 15.0f)
            }

            updateFuelLevel(_speed.value ?: 0)

            _checkGauges.value = _engineTemp.value!! > 110 || _oilPressure.value!! < 20
            _lowOil.value = _oilPressure.value!! < 10
        }
    }

    private fun updateFuelLevel(speed: Int) {
        if (_engineRunning.value == true) {
            _fuelLevel.value = _fuelLevel.value?.let { currentFuel ->
                val consumption = when {
                    speed == 0 -> 0.000f
                    speed < 30 -> 0.0001f
                    speed < 60 -> 0.0002f
                    else -> 0.0003f
                }
                (currentFuel - consumption).coerceIn(0f, 100f).toInt()
            }
        }
    }

    private fun updateGear(speed: Int) {
        _gear.value = when {
            speed < 10 -> "P"
            speed < 20 -> "1"
            speed < 30 -> "2"
            speed < 40 -> "3"
            speed < 50 -> "4"
            speed < 60 -> "5"
            else -> "6"
        }
    }

    private fun resetSmoothers() {
        speedSmoother.reset()
        rpmSmoother.reset()
        tempSmoother.reset()
        fuelSmoother.reset()
        voltageSmoother.reset()
    }

    override fun onCleared() {
        super.onCleared()
        obdService.disconnect()
    }
}
