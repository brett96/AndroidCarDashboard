package com.example.cardashboardtest.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

/**
 * Class that simulates car data for the dashboard
 */
class CarData {
    // Speed in MPH
    private val _speed = MutableLiveData(0)
    val speed: LiveData<Int> = _speed

    // RPM (0-8000)
    private val _rpm = MutableLiveData(0)
    val rpm: LiveData<Int> = _rpm

    // Fuel level (0-100%)
    private val _fuelLevel = MutableLiveData(80)
    val fuelLevel: LiveData<Int> = _fuelLevel

    // Engine temperature (in Celsius)
    private val _engineTemp = MutableLiveData(90)
    val engineTemp: LiveData<Int> = _engineTemp

    // Odometer reading in miles
    private val _odometer = MutableLiveData(12345)
    val odometer: LiveData<Int> = _odometer

    // Current gear (P, R, N, D, 1, 2, etc.)
    private val _gear = MutableLiveData("P")
    val gear: LiveData<String> = _gear

    // Is engine running
    private val _engineRunning = MutableLiveData(false)
    val engineRunning: LiveData<Boolean> = _engineRunning

    // Outside temperature in Celsius
    private val _outsideTemp = MutableLiveData(22)
    val outsideTemp: LiveData<Int> = _outsideTemp

    // Current time
    private val _time = MutableLiveData<String>()
    val time: LiveData<String> = _time

    /**
     * Start the engine and begin simulating data
     */
    fun startEngine() {
        _engineRunning.value = true
        _rpm.value = 800
    }

    /**
     * Stop the engine
     */
    fun stopEngine() {
        _engineRunning.value = false
        _speed.value = 0
        _rpm.value = 0
    }

    /**
     * Update speed value
     */
    fun setSpeed(newSpeed: Int) {
        _speed.value = newSpeed.coerceIn(0, 150)
        // Update RPM based on speed (simplified simulation)
        _rpm.value = 800 + (newSpeed * 35)
    }

    /**
     * Update fuel level
     */
    fun setFuelLevel(level: Int) {
        _fuelLevel.value = level.coerceIn(0, 100)
    }

    /**
     * Update engine temperature
     */
    fun setEngineTemp(temp: Int) {
        _engineTemp.value = temp.coerceIn(50, 130)
    }

    /**
     * Set current gear
     */
    fun setGear(newGear: String) {
        _gear.value = newGear
    }

    /**
     * Update time
     */
    fun setTime(newTime: String) {
        _time.value = newTime
    }

    /**
     * Simulate random data changes for demo purposes
     */
    fun simulateDataChanges() {
        if (_engineRunning.value == true) {
            // Randomly adjust speed
            val currentSpeed = _speed.value ?: 0
            val speedChange = Random.nextInt(-5, 6)
            if (_speed.value ?: 0 > 0){
                setSpeed((currentSpeed + speedChange).coerceIn(0, 150))
            }
            

            // Slowly decrease fuel
            val currentFuel = _fuelLevel.value ?: 75
            if (Random.nextInt(0, 100) < 10) { // 10% chance to decrease fuel
                setFuelLevel(currentFuel - 1)
            }

            // Fluctuate engine temperature
            val currentTemp = _engineTemp.value ?: 90
            val tempChange = Random.nextInt(-2, 3)
            setEngineTemp(currentTemp + tempChange)
        }
    }

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: CarData? = null

        fun getInstance(): CarData {
            return INSTANCE ?: synchronized(this) {
                val instance = CarData()
                INSTANCE = instance
                instance
            }
        }
    }
}
