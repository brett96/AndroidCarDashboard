package com.example.cardashboardtest.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cardashboardtest.model.CarData
import kotlin.random.Random

class DashboardViewModel : ViewModel() {
    private val carData = CarData.getInstance()

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

    init {
        _speed.value = 0
        _rpm.value = 0
        _fuelLevel.value = 75
        _engineTemp.value = 90  // 90°C = 194°F
        _engineRunning.value = false
        _gear.value = "P"
        _oilPressure.value = 45
        _voltage.value = 14.4f
        _checkGauges.value = false
        _lowOil.value = false
    }

    // Expose car data LiveData objects
    val odometer: LiveData<Int> = carData.odometer
    val outsideTemp: LiveData<Int> = carData.outsideTemp
    val time: LiveData<String> = carData.time

    /**
     * Start the car engine
     */
    fun startEngine() {
        _engineRunning.value = true
        _rpm.value = 1000
        simulateDataChanges()
    }

    /**
     * Stop the car engine
     */
    fun stopEngine() {
        _engineRunning.value = false
        _speed.value = 0
        _rpm.value = 0
        _gear.value = "P"
    }

    /**
     * Set the car speed
     */
    fun setSpeed(speed: Int) {
        _speed.value = speed
        // Update RPM based on speed
        _rpm.value = (speed * 50) + 1000
        // Update fuel consumption based on speed and RPM
//        updateFuelLevel(speed)
    }

    private fun updateFuelLevel(speed: Int) {
        if (_engineRunning.value == true) {
            _fuelLevel.value = _fuelLevel.value?.let { currentFuel ->
                // Calculate fuel consumption based on speed
                val consumption = when {
                    speed == 0 -> 0.000f  // Idle consumption
                    speed < 30 -> 0.0001f  // City driving
                    speed < 60 -> 0.0002f  // Highway driving
                    else -> 0.0003f        // High speed driving
                }
                
                // Reduce fuel by consumption amount
                (currentFuel - consumption).coerceIn(0f, 100f).toInt()
            }
        }
    }

    /**
     * Set the car gear
     */
    fun setGear(gear: String) {
        _gear.value = gear
    }

    /**
     * Simulate random data changes for demo purposes
     */
    fun simulateDataChanges() {
        if (_engineRunning.value == true) {
            // Simulate small random changes in temperature
            _engineTemp.value = _engineTemp.value?.let { temp ->
                (temp + Random.nextInt(-2, 3)).coerceIn(50, 120)  // 50°C = 122°F, 120°C = 248°F
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
}
