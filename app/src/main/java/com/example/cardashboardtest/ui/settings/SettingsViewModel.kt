package com.example.cardashboardtest.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _updateInterval = MutableStateFlow(1000L)
    val updateInterval: StateFlow<Long> = _updateInterval

    private val _autoConnect = MutableStateFlow(true)
    val autoConnect: StateFlow<Boolean> = _autoConnect

    private val _temperatureUnit = MutableStateFlow("celsius")
    val temperatureUnit: StateFlow<String> = _temperatureUnit

    private val _speedUnit = MutableStateFlow("mph")
    val speedUnit: StateFlow<String> = _speedUnit

    private val _showNotifications = MutableStateFlow(true)
    val showNotifications: StateFlow<Boolean> = _showNotifications

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn

    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode

    private val _logBluetooth = MutableStateFlow(true)
    val logBluetooth: StateFlow<Boolean> = _logBluetooth

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme

    private val _showSpeed = MutableStateFlow(true)
    val showSpeed: StateFlow<Boolean> = _showSpeed

    private val _showRpm = MutableStateFlow(true)
    val showRpm: StateFlow<Boolean> = _showRpm

    private val _showFuel = MutableStateFlow(true)
    val showFuel: StateFlow<Boolean> = _showFuel

    private val _showTemp = MutableStateFlow(true)
    val showTemp: StateFlow<Boolean> = _showTemp

    fun setUpdateInterval(interval: String) {
        viewModelScope.launch {
            _updateInterval.value = interval.toLong()
        }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            _autoConnect.value = enabled
        }
    }

    fun setTemperatureUnit(unit: String) {
        viewModelScope.launch {
            _temperatureUnit.value = unit
        }
    }

    fun setSpeedUnit(unit: String) {
        viewModelScope.launch {
            _speedUnit.value = unit
        }
    }

    fun setShowNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _showNotifications.value = enabled
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            _keepScreenOn.value = enabled
        }
    }

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            _debugMode.value = enabled
        }
    }

    fun setLogBluetooth(enabled: Boolean) {
        viewModelScope.launch {
            _logBluetooth.value = enabled
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            _theme.value = theme
        }
    }

    fun setTempUnit(unit: String) {
        viewModelScope.launch {
            _temperatureUnit.value = unit
        }
    }

    fun setShowSpeed(show: Boolean) {
        viewModelScope.launch {
            _showSpeed.value = show
        }
    }

    fun setShowRpm(show: Boolean) {
        viewModelScope.launch {
            _showRpm.value = show
        }
    }

    fun setShowFuel(show: Boolean) {
        viewModelScope.launch {
            _showFuel.value = show
        }
    }

    fun setShowTemp(show: Boolean) {
        viewModelScope.launch {
            _showTemp.value = show
        }
    }
}
