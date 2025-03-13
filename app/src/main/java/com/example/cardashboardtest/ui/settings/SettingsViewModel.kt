package com.example.cardashboardtest.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.app.Application
import androidx.preference.PreferenceManager

class SettingsViewModel() : ViewModel() {
    
    private val _themeChanged = MutableLiveData<String>()
    val themeChanged: LiveData<String> = _themeChanged
    
    private val _speedUnitChanged = MutableLiveData<String>()
    val speedUnitChanged: LiveData<String> = _speedUnitChanged
    
    private val _tempUnitChanged = MutableLiveData<String>()
    val tempUnitChanged: LiveData<String> = _tempUnitChanged
    
    private val _showSpeedChanged = MutableLiveData<Boolean>()
    val showSpeedChanged: LiveData<Boolean> = _showSpeedChanged
    
    private val _showRpmChanged = MutableLiveData<Boolean>()
    val showRpmChanged: LiveData<Boolean> = _showRpmChanged
    
    private val _showFuelChanged = MutableLiveData<Boolean>()
    val showFuelChanged: LiveData<Boolean> = _showFuelChanged
    
    private val _showTempChanged = MutableLiveData<Boolean>()
    val showTempChanged: LiveData<Boolean> = _showTempChanged
    
    fun setTheme(theme: String) {
        _themeChanged.value = theme
    }
    
    fun setSpeedUnit(unit: String) {
        _speedUnitChanged.value = unit
    }
    
    fun setTempUnit(unit: String) {
        _tempUnitChanged.value = unit
    }
    
    fun setShowSpeed(show: Boolean) {
        _showSpeedChanged.value = show
    }
    
    fun setShowRpm(show: Boolean) {
        _showRpmChanged.value = show
    }
    
    fun setShowFuel(show: Boolean) {
        _showFuelChanged.value = show
    }
    
    fun setShowTemp(show: Boolean) {
        _showTempChanged.value = show
    }
}
