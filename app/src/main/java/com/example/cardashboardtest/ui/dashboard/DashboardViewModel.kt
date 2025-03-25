package com.example.cardashboardtest.ui.dashboard

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cardashboardtest.bluetooth.ObdBluetoothService
import com.example.cardashboardtest.model.CarData
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.random.Random

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val carData = CarData.getInstance()
    private val obdService = ObdBluetoothService(application)

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

        // Observe OBD connection state
        obdService.connectionState
            .onEach { state ->
                _connectionState.value = state
                useSimulatedData = state !is ObdBluetoothService.ConnectionState.Connected
                if (state is ObdBluetoothService.ConnectionState.Connected) {
                    // Reset data when connected
                    _speed.value = 0
                    _rpm.value = 0
                    _fuelLevel.value = 0
                    _engineTemp.value = 0
                    _voltage.value = 0f
                    _checkGauges.value = false
                    _lowOil.value = false
                }
            }
            .launchIn(viewModelScope)

        // Observe OBD data
        obdService.obdData
            .onEach { data ->
                if (!useSimulatedData) {
                    data.speed?.let { _speed.value = it }
                    data.rpm?.let { _rpm.value = it }
                    data.engineTemp?.let { _engineTemp.value = it }
                    data.fuelLevel?.let { _fuelLevel.value = it }
                    data.batteryVoltage?.let { _voltage.value = it }
                    data.checkEngine?.let { _checkGauges.value = it }
                    
                    // Update engine running state based on RPM
                    _engineRunning.value = data.rpm != null && data.rpm > 0
                    
                    // Update gear based on speed
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
        obdService.connect(device)
    }

    fun disconnectDevice() {
        obdService.disconnect()
        useSimulatedData = true
        // Reset values to default
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
            // Simulate small random changes in temperature
            _engineTemp.value = _engineTemp.value?.let { temp ->
                (temp + Random.nextInt(-2, 3)).coerceIn(50, 120)
            }

            // Simulate oil pressure changes
            _oilPressure.value = _oilPressure.value?.let { pressure ->
                (pressure + Random.nextInt(-5, 6)).coerceIn(0, 80)
            }

            // Simulate voltage changes
            _voltage.value = _voltage.value?.let { voltage ->
                (voltage + Random.nextFloat() * 0.2f - 0.1f).coerceIn(11.0f, 15.0f)
            }

            // Update fuel level based on engine running
            updateFuelLevel(_speed.value ?: 0)

            // Update warning lights
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

    override fun onCleared() {
        super.onCleared()
        obdService.disconnect()
    }
}
