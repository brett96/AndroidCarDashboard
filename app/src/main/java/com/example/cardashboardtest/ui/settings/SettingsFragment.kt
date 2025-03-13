package com.example.cardashboardtest.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.lifecycle.ViewModelProvider
import com.example.cardashboardtest.R
import com.example.cardashboardtest.ui.theme.DashboardTheme
import com.example.cardashboardtest.utils.ThemePreferences

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var themePreferences: ThemePreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        themePreferences = ThemePreferences(requireContext())

        setupPreferences()
    }

    private fun setupPreferences() {
        // App theme preference (light/dark)
        val appThemePreference = findPreference<ListPreference>("app_theme_preference")
        appThemePreference?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue as String) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }

        // Dashboard style preference (Modern/Corvette)
        val dashboardStylePreference = findPreference<ListPreference>("dashboard_style_preference")
        dashboardStylePreference?.setOnPreferenceChangeListener { _, newValue ->
            val newTheme = when (newValue as String) {
                "modern" -> DashboardTheme.MODERN
                "corvette_1985" -> DashboardTheme.CORVETTE_1985
                else -> DashboardTheme.MODERN
            }
            themePreferences.setTheme(newTheme)
            activity?.recreate()
            true
        }

        // Theme preference
        val themePreference = findPreference<ListPreference>("theme_preference")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setTheme(newValue as String)
            true
        }

        // Units preference (mph vs km/h)
        val unitsPreference = findPreference<ListPreference>("units_preference")
        unitsPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setSpeedUnit(newValue as String)
            true
        }

        // Temperature units preference (C vs F)
        val tempUnitPreference = findPreference<ListPreference>("temp_unit_preference")
        tempUnitPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setTempUnit(newValue as String)
            true
        }

        // Show/hide gauges
        val showSpeedPreference = findPreference<SwitchPreferenceCompat>("show_speed_preference")
        showSpeedPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setShowSpeed(newValue as Boolean)
            true
        }

        val showRpmPreference = findPreference<SwitchPreferenceCompat>("show_rpm_preference")
        showRpmPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setShowRpm(newValue as Boolean)
            true
        }

        val showFuelPreference = findPreference<SwitchPreferenceCompat>("show_fuel_preference")
        showFuelPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setShowFuel(newValue as Boolean)
            true
        }

        val showTempPreference = findPreference<SwitchPreferenceCompat>("show_temp_preference")
        showTempPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setShowTemp(newValue as Boolean)
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Additional setup if needed
    }
}
