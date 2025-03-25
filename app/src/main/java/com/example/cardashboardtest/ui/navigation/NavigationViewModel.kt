package com.example.cardashboardtest.ui.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cardashboardtest.R

class NavigationViewModel : ViewModel() {
    
    private val _mapImageResource = MutableLiveData<Int>()
    val mapImageResource: LiveData<Int> = _mapImageResource
    
    private val _destination = MutableLiveData<String>()
    val destination: LiveData<String> = _destination
    
    private val _eta = MutableLiveData<String>()
    val eta: LiveData<String> = _eta
    
    init {
        // Set default values for demonstration
        _mapImageResource.value = R.drawable.ic_launcher_background // Placeholder - would be a map image
        _destination.value = "123 Main Street, Anytown, USA"
        _eta.value = "Arriving at 10:30 AM (15 min)"
    }
}
